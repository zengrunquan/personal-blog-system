package com.blog.media.dao;

import com.blog.media.model.ManagedMediaKey;
import com.blog.media.model.MediaAsset;
import com.blog.media.model.MediaReferenceType;
import com.blog.media.model.MediaType;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface MediaDao {

    long insert(Connection connection, MediaAsset asset) throws SQLException;

    /**
     * 兼容只实现生命周期操作的旧适配器；未实现查询时必须报错，不能伪装成没有元数据。
     */
    default Optional<String> findOriginalName(
            Connection connection,
            MediaType mediaType,
            String urlFileName
    ) throws SQLException {
        throw new UnsupportedOperationException("当前 MediaDao 未实现附件原名查询");
    }

    /**
     * 回填已存在资产的元数据和状态；更新必须带预期旧状态，避免覆盖并发清理状态。
     */
    default void reconcileBackfillAsset(
            Connection connection,
            MediaAsset asset,
            com.blog.media.model.MediaStatus expectedStatus,
            boolean fillOnlyMissing
    ) throws SQLException {
        throw new UnsupportedOperationException("当前 MediaDao 未实现回填资产协调");
    }

    List<MediaAsset> findByKeysForUpdate(
            Connection connection,
            Set<ManagedMediaKey> keys
    ) throws SQLException;

    void replaceAvatarReference(Connection connection, int userId, long mediaId)
            throws SQLException;

    void replaceArticleReferences(
            Connection connection,
            int articleId,
            Map<Long, Set<MediaReferenceType>> references
    ) throws SQLException;

    int markUnreferencedAssets(Connection connection, Instant now, Instant deleteAfter)
            throws SQLException;

    Optional<MediaAsset> claimNextDueAsset(
            Connection connection,
            String claimToken,
            Instant now,
            Instant staleBefore
    ) throws SQLException;

    void markDeleted(Connection connection, long mediaId, String claimToken) throws SQLException;

    void markDeleteFailed(
            Connection connection,
            long mediaId,
            String claimToken,
            Instant retryAt,
            String safeError
    ) throws SQLException;

    int recoverStaleClaims(Connection connection, Instant staleBefore, Instant retryAt)
            throws SQLException;
}
