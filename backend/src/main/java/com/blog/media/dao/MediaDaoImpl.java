package com.blog.media.dao;

import com.blog.media.model.ManagedMediaKey;
import com.blog.media.model.MediaAsset;
import com.blog.media.model.MediaReferenceType;
import com.blog.media.model.MediaStatus;
import com.blog.media.model.MediaType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

/** 媒体 JDBC 访问层；事务由上层持有，DAO 不私自提交或关闭外部连接。 */
public class MediaDaoImpl implements MediaDao {

    private static final Logger LOGGER = LogManager.getLogger(MediaDaoImpl.class);

    @Override
    public long insert(Connection connection, MediaAsset asset) throws SQLException {
        String sql = "INSERT INTO media_asset "
                + "(media_type, storage_name, url_file_name, original_name, content_type, "
                + "size_bytes, sha256, uploaded_by, status, delete_after, claim_token, "
                + "claimed_at, delete_attempts, last_error) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(
                sql,
                Statement.RETURN_GENERATED_KEYS
        )) {
            bindAsset(statement, asset);
            if (statement.executeUpdate() <= 0) {
                throw new SQLException("媒体资产插入没有影响任何行");
            }
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("媒体资产插入后未返回主键");
                long id = keys.getLong(1);
                asset.setId(id);
                return id;
            }
        }
    }

    @Override
    public void reconcileBackfillAsset(
            Connection connection,
            MediaAsset asset,
            MediaStatus expectedStatus,
            boolean fillOnlyMissing
    ) throws SQLException {
        if (asset == null || asset.getId() == null || asset.getStatus() == null) {
            throw new SQLException("回填资产缺少主键或目标状态");
        }
        if (expectedStatus == null) throw new SQLException("回填资产预期状态不能为空");

        String sql = "UPDATE media_asset SET original_name = "
                + backfillValueExpression("original_name", fillOnlyMissing)
                + ", content_type = "
                + backfillValueExpression("content_type", fillOnlyMissing)
                + ", size_bytes = "
                + backfillValueExpression("size_bytes", fillOnlyMissing)
                + ", sha256 = "
                + backfillValueExpression("sha256", fillOnlyMissing)
                + ", status = ?"
                + (asset.getStatus() == MediaStatus.ACTIVE
                ? ", delete_after = NULL, claim_token = NULL, claimed_at = NULL, last_error = NULL"
                : "")
                + " WHERE id = ? AND status = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int parameter = 1;
            setNullableString(statement, parameter++, asset.getOriginalName());
            setNullableString(statement, parameter++, asset.getContentType());
            setNullableLong(statement, parameter++, asset.getSizeBytes());
            setNullableString(statement, parameter++, asset.getSha256());
            statement.setString(parameter++, asset.getStatus().name());
            statement.setLong(parameter++, asset.getId());
            statement.setString(parameter, expectedStatus.name());
            if (statement.executeUpdate() != 1) {
                throw new SQLException("回填资产状态已变化或不存在，mediaId=" + asset.getId()
                        + "，expectedStatus=" + expectedStatus);
            }
        }
    }

    private String backfillValueExpression(String column, boolean fillOnlyMissing) {
        return fillOnlyMissing ? "COALESCE(" + column + ", ?)" : "?";
    }

    @Override
    public List<MediaAsset> findByKeysForUpdate(
            Connection connection,
            Set<ManagedMediaKey> keys
    ) throws SQLException {
        if (keys == null || keys.isEmpty()) return Collections.emptyList();
        List<ManagedMediaKey> sortedKeys = new ArrayList<>(keys);
        sortedKeys.sort(Comparator.comparing(ManagedMediaKey::toString));
        StringJoiner predicates = new StringJoiner(" OR ");
        for (int i = 0; i < sortedKeys.size(); i++) {
            predicates.add("(media_type = ? AND url_file_name = ?)");
        }
        String sql = "SELECT * FROM media_asset WHERE " + predicates + " FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int parameter = 1;
            for (ManagedMediaKey key : sortedKeys) {
                statement.setString(parameter++, key.getMediaType().name());
                statement.setString(parameter++, key.getUrlFileName());
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                List<MediaAsset> assets = new ArrayList<>();
                while (resultSet.next()) assets.add(mapRow(resultSet));
                return assets;
            }
        }
    }

    @Override
    public void replaceAvatarReference(Connection connection, int userId, long mediaId)
            throws SQLException {
        MediaAsset asset = lockById(connection, mediaId);
        ensureReferenceable(asset, mediaId);
        ensureReferenceType(asset, MediaReferenceType.USER_AVATAR, mediaId);

        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM media_reference "
                        + "WHERE reference_type = 'USER_AVATAR' AND user_id = ?")) {
            delete.setInt(1, userId);
            delete.executeUpdate();
        }
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO media_reference "
                        + "(media_id, reference_type, user_id, article_id) "
                        + "VALUES (?, 'USER_AVATAR', ?, NULL)")) {
            insert.setLong(1, mediaId);
            insert.setInt(2, userId);
            insert.executeUpdate();
        }
        activateIfNeeded(connection, asset);
    }

    @Override
    public void replaceArticleReferences(
            Connection connection,
            int articleId,
            Map<Long, Set<MediaReferenceType>> references
    ) throws SQLException {
        Map<Long, Set<MediaReferenceType>> safeReferences = references == null
                ? Collections.emptyMap()
                : references;
        Map<Long, MediaAsset> assets = new HashMap<>();
        List<Long> mediaIds = new ArrayList<>();
        for (Long mediaId : safeReferences.keySet()) {
            if (mediaId == null || mediaId <= 0) {
                throw new SQLException("文章媒体引用缺少合法媒体 ID");
            }
            mediaIds.add(mediaId);
        }
        mediaIds.sort(Long::compareTo);
        for (Long mediaId : mediaIds) {
            MediaAsset asset = lockById(connection, mediaId);
            ensureReferenceable(asset, mediaId);
            Set<MediaReferenceType> types = safeReferences.get(mediaId);
            if (types == null) {
                throw new SQLException("文章媒体引用类型集合不能为空，mediaId=" + mediaId);
            }
            for (MediaReferenceType type : types) {
                ensureReferenceType(asset, type, mediaId);
            }
            assets.put(mediaId, asset);
        }

        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM media_reference WHERE article_id = ? "
                        + "AND reference_type IN ('ARTICLE_CONTENT', 'ARTICLE_COVER')")) {
            delete.setInt(1, articleId);
            delete.executeUpdate();
        }
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO media_reference "
                        + "(media_id, reference_type, user_id, article_id) "
                        + "VALUES (?, ?, NULL, ?)")) {
            for (Long mediaId : mediaIds) {
                Set<MediaReferenceType> types = safeReferences.get(mediaId);
                if (types == null) continue;
                for (MediaReferenceType type : types) {
                    if (type != MediaReferenceType.ARTICLE_CONTENT
                            && type != MediaReferenceType.ARTICLE_COVER) {
                        throw new SQLException("文章引用类型不合法：" + type);
                    }
                    insert.setLong(1, mediaId);
                    insert.setString(2, type.name());
                    insert.setInt(3, articleId);
                    insert.addBatch();
                }
            }
            insert.executeBatch();
        }
        for (MediaAsset asset : assets.values()) activateIfNeeded(connection, asset);
    }

    @Override
    public int markUnreferencedAssets(
            Connection connection,
            Instant now,
            Instant deleteAfter
    ) throws SQLException {
        String sql = "UPDATE media_asset a SET status = 'DELETE_PENDING', "
                + "delete_after = ?, claim_token = NULL, claimed_at = NULL "
                + "WHERE a.status = 'ACTIVE' "
                + "AND NOT EXISTS (SELECT 1 FROM media_reference r WHERE r.media_id = a.id)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, timestamp(deleteAfter));
            return statement.executeUpdate();
        }
    }

    @Override
    public Optional<MediaAsset> claimNextDueAsset(
            Connection connection,
            String claimToken,
            Instant now,
            Instant staleBefore
    ) throws SQLException {
        if (claimToken == null || claimToken.trim().isEmpty()) {
            throw new IllegalArgumentException("清理 claim token 不能为空");
        }
        // XAMPP 的 MariaDB 10.4 不支持 SKIP LOCKED；保留事务行锁以防止重复认领。
        // 并发任务在此等待锁，超时由事务边界回滚，后续清理轮次继续尝试。
        String selectSql = "SELECT * FROM media_asset "
                + "WHERE status IN ('TEMP', 'DELETE_PENDING', 'DELETE_FAILED') "
                + "AND delete_after IS NOT NULL AND delete_after <= ? "
                + "AND NOT EXISTS (SELECT 1 FROM media_reference r WHERE r.media_id = media_asset.id) "
                + "ORDER BY delete_after ASC, id ASC LIMIT 1 FOR UPDATE";
        try (PreparedStatement select = connection.prepareStatement(selectSql)) {
            select.setTimestamp(1, timestamp(now));
            try (ResultSet resultSet = select.executeQuery()) {
                if (!resultSet.next()) return Optional.empty();
                MediaAsset asset = mapRow(resultSet);
                String updateSql = "UPDATE media_asset SET status = 'DELETING', "
                        + "claim_token = ?, claimed_at = ?, delete_attempts = delete_attempts + 1 "
                        + "WHERE id = ? AND status IN ('TEMP', 'DELETE_PENDING', 'DELETE_FAILED') "
                        + "AND NOT EXISTS (SELECT 1 FROM media_reference r WHERE r.media_id = media_asset.id)";
                try (PreparedStatement update = connection.prepareStatement(updateSql)) {
                    update.setString(1, claimToken);
                    update.setTimestamp(2, timestamp(now));
                    update.setLong(3, asset.getId());
                    if (update.executeUpdate() != 1) return Optional.empty();
                }
                asset.setStatus(MediaStatus.DELETING);
                asset.setClaimToken(claimToken);
                asset.setClaimedAt(now);
                asset.setDeleteAttempts(asset.getDeleteAttempts() + 1);
                return Optional.of(asset);
            }
        }
    }

    @Override
    public void markDeleted(Connection connection, long mediaId, String claimToken)
            throws SQLException {
        String sql = "UPDATE media_asset SET status = 'DELETED', delete_after = NULL, "
                + "claim_token = NULL, claimed_at = NULL, last_error = NULL "
                + "WHERE id = ? AND status = 'DELETING' AND claim_token = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, mediaId);
            statement.setString(2, claimToken);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("媒体删除完成状态未匹配 claim token，mediaId=" + mediaId);
            }
        }
    }

    @Override
    public void markDeleteFailed(
            Connection connection,
            long mediaId,
            String claimToken,
            Instant retryAt,
            String safeError
    ) throws SQLException {
        String sql = "UPDATE media_asset SET status = 'DELETE_FAILED', delete_after = ?, "
                + "claim_token = NULL, claimed_at = NULL, last_error = ? "
                + "WHERE id = ? AND status = 'DELETING' AND claim_token = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, timestamp(retryAt));
            statement.setString(2, truncateError(safeError));
            statement.setLong(3, mediaId);
            statement.setString(4, claimToken);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("媒体删除失败状态未匹配 claim token，mediaId=" + mediaId);
            }
        }
    }

    @Override
    public int recoverStaleClaims(
            Connection connection,
            Instant staleBefore,
            Instant retryAt
    ) throws SQLException {
        String sql = "UPDATE media_asset SET status = 'DELETE_PENDING', "
                + "delete_after = ?, claim_token = NULL, claimed_at = NULL "
                + "WHERE status = 'DELETING' AND claimed_at < ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, timestamp(retryAt));
            statement.setTimestamp(2, timestamp(staleBefore));
            return statement.executeUpdate();
        }
    }

    private MediaAsset lockById(Connection connection, long mediaId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM media_asset WHERE id = ? FOR UPDATE")) {
            statement.setLong(1, mediaId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) throw new SQLException("媒体资产不存在，mediaId=" + mediaId);
                return mapRow(resultSet);
            }
        }
    }

    private void ensureReferenceable(MediaAsset asset, long mediaId) throws SQLException {
        if (asset.getStatus() == MediaStatus.DELETING || asset.getStatus() == MediaStatus.DELETED) {
            throw new SQLException("媒体资产当前不可重新引用，mediaId=" + mediaId
                    + "，status=" + asset.getStatus());
        }
    }

    private void ensureReferenceType(
            MediaAsset asset,
            MediaReferenceType referenceType,
            long mediaId
    ) throws SQLException {
        if (referenceType == null) {
            throw new SQLException("媒体引用类型不能为空，mediaId=" + mediaId);
        }
        MediaType mediaType = asset.getMediaType();
        boolean valid = referenceType == MediaReferenceType.USER_AVATAR
                ? mediaType == MediaType.AVATAR
                : referenceType == MediaReferenceType.ARTICLE_COVER
                ? mediaType == MediaType.ARTICLE_IMAGE
                : mediaType == MediaType.AVATAR
                || mediaType == MediaType.ARTICLE_IMAGE
                || mediaType == MediaType.ATTACHMENT;
        if (!valid) {
            throw new SQLException("媒体引用类型与媒体资产类型不匹配，mediaId=" + mediaId
                    + "，mediaType=" + asset.getMediaType()
                    + "，referenceType=" + referenceType);
        }
    }

    private void activateIfNeeded(Connection connection, MediaAsset asset) throws SQLException {
        if (asset.getStatus() != MediaStatus.TEMP
                && asset.getStatus() != MediaStatus.DELETE_PENDING
                && asset.getStatus() != MediaStatus.DELETE_FAILED) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE media_asset SET status = 'ACTIVE', delete_after = NULL, "
                        + "claim_token = NULL, claimed_at = NULL, last_error = NULL "
                        + "WHERE id = ? AND status IN ('TEMP', 'DELETE_PENDING', 'DELETE_FAILED')")) {
            statement.setLong(1, asset.getId());
            statement.executeUpdate();
        }
    }

    private void bindAsset(PreparedStatement statement, MediaAsset asset) throws SQLException {
        statement.setString(1, asset.getMediaType().name());
        statement.setString(2, asset.getStorageName());
        statement.setString(3, asset.getUrlFileName());
        statement.setString(4, asset.getOriginalName());
        setNullableString(statement, 5, asset.getContentType());
        setNullableLong(statement, 6, asset.getSizeBytes());
        setNullableString(statement, 7, asset.getSha256());
        setNullableInt(statement, 8, asset.getUploadedBy());
        statement.setString(9, asset.getStatus().name());
        setNullableInstant(statement, 10, asset.getDeleteAfter());
        setNullableString(statement, 11, asset.getClaimToken());
        setNullableInstant(statement, 12, asset.getClaimedAt());
        statement.setInt(13, asset.getDeleteAttempts());
        setNullableString(statement, 14, truncateError(asset.getLastError()));
    }

    private MediaAsset mapRow(ResultSet resultSet) throws SQLException {
        MediaAsset asset = new MediaAsset();
        asset.setId(nullableLong(resultSet, "id"));
        asset.setMediaType(MediaType.valueOf(resultSet.getString("media_type")));
        asset.setStorageName(resultSet.getString("storage_name"));
        asset.setUrlFileName(resultSet.getString("url_file_name"));
        asset.setOriginalName(resultSet.getString("original_name"));
        asset.setContentType(resultSet.getString("content_type"));
        asset.setSizeBytes(nullableLong(resultSet, "size_bytes"));
        asset.setSha256(resultSet.getString("sha256"));
        asset.setUploadedBy(nullableInt(resultSet, "uploaded_by"));
        asset.setStatus(MediaStatus.valueOf(resultSet.getString("status")));
        asset.setDeleteAfter(toInstant(resultSet.getTimestamp("delete_after")));
        asset.setClaimToken(resultSet.getString("claim_token"));
        asset.setClaimedAt(toInstant(resultSet.getTimestamp("claimed_at")));
        asset.setDeleteAttempts(resultSet.getInt("delete_attempts"));
        asset.setLastError(resultSet.getString("last_error"));
        asset.setCreateTime(toInstant(resultSet.getTimestamp("create_time")));
        asset.setUpdateTime(toInstant(resultSet.getTimestamp("update_time")));
        return asset;
    }

    private Long nullableLong(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }

    private Integer nullableInt(ResultSet resultSet, String column) throws SQLException {
        int value = resultSet.getInt(column);
        return resultSet.wasNull() ? null : value;
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private Timestamp timestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private void setNullableString(PreparedStatement statement, int index, String value)
            throws SQLException {
        if (value == null) statement.setNull(index, java.sql.Types.VARCHAR);
        else statement.setString(index, value);
    }

    private void setNullableLong(PreparedStatement statement, int index, Long value)
            throws SQLException {
        if (value == null) statement.setNull(index, java.sql.Types.BIGINT);
        else statement.setLong(index, value);
    }

    private void setNullableInt(PreparedStatement statement, int index, Integer value)
            throws SQLException {
        if (value == null) statement.setNull(index, java.sql.Types.INTEGER);
        else statement.setInt(index, value);
    }

    private void setNullableInstant(PreparedStatement statement, int index, Instant value)
            throws SQLException {
        if (value == null) statement.setNull(index, java.sql.Types.TIMESTAMP);
        else statement.setTimestamp(index, timestamp(value));
    }

    private String truncateError(String error) {
        if (error == null) return null;
        return error.length() <= 1000 ? error : error.substring(0, 1000);
    }
}
