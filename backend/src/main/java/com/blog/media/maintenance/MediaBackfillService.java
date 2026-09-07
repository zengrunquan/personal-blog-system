package com.blog.media.maintenance;

import com.blog.api.upload.UploadPolicy;
import com.blog.media.dao.MediaDao;
import com.blog.media.dao.MediaDaoImpl;
import com.blog.media.model.ManagedMediaKey;
import com.blog.media.model.MediaAsset;
import com.blog.media.model.MediaReferenceType;
import com.blog.media.model.MediaStatus;
import com.blog.media.model.MediaType;
import com.blog.media.service.ManagedMediaUrlParser;
import com.blog.media.service.MediaReferenceExtractor;
import com.blog.media.storage.StorageRootResolver;
import com.blog.util.JdbcTransactionManager;
import com.blog.util.TransactionManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Stream;

/** 历史媒体回填；dry-run 只读，apply 只新增/补齐元数据和引用，不改动物理文件。 */
public class MediaBackfillService {

    private static final Logger LOGGER = LogManager.getLogger(MediaBackfillService.class);
    private static final int EXISTING_ASSET_BATCH_SIZE = 300;
    private final MediaBackfillSourceDao sourceDao;
    private final MediaDao mediaDao;
    private final TransactionManager transactionManager;
    private final Path configuredStorageRoot;
    private final Clock clock;

    public MediaBackfillService() {
        this(
                new JdbcMediaBackfillSourceDao(),
                new MediaDaoImpl(),
                new JdbcTransactionManager(),
                null,
                Clock.systemUTC()
        );
    }

    public MediaBackfillService(
            MediaBackfillSourceDao sourceDao,
            MediaDao mediaDao,
            TransactionManager transactionManager,
            Path storageRoot,
            Clock clock
    ) {
        this.sourceDao = Objects.requireNonNull(sourceDao, "sourceDao 不能为空");
        this.mediaDao = Objects.requireNonNull(mediaDao, "mediaDao 不能为空");
        this.transactionManager = Objects.requireNonNull(
                transactionManager, "transactionManager 不能为空");
        this.configuredStorageRoot = storageRoot == null
                ? null
                : storageRoot.toAbsolutePath().normalize();
        this.clock = Objects.requireNonNull(clock, "clock 不能为空");
    }

    /**
     * 执行回填扫描。apply=false 时不调用任何写 DAO，也不创建、移动或删除文件。
     */
    public BackfillReport run(boolean apply) throws IOException {
        Path storageRoot = resolveStorageRoot();
        SourceSnapshot sources = loadSources();
        ReferenceIndex references = collectReferences(sources.users, sources.articles);
        ScanResult scan = scanFiles(storageRoot);
        BackfillReport.Builder report = new BackfillReport.Builder()
                .storageRoot(storageRoot.toString())
                // 只记录实现类型，避免把 JDBC URL、用户名或其他连接配置写入维护报告。
                .sourceSummary(sourceDao.getClass().getSimpleName())
                .usersScanned(sources.users.size())
                .articlesScanned(sources.articles.size())
                .filesScanned(scan.filesScanned)
                .manualReview(scan.manualReview + references.manualReview)
                .duplicateKeys(scan.duplicateKeys)
                .hashFailures(scan.hashFailures)
                .referencesCreated(references.referenceCount());

        classify(scan, references, report);
        if (apply) apply(scan, references, report);
        return report.build();
    }

    private SourceSnapshot loadSources() throws IOException {
        try {
            SourceSnapshot snapshot = transactionManager.inTransaction(connection -> {
                boolean readOnlyKnown = false;
                boolean originalReadOnly = false;
                try {
                    if (connection != null) {
                        originalReadOnly = connection.isReadOnly();
                        connection.setReadOnly(true);
                        readOnlyKnown = true;
                    }
                    List<UserMediaSource> users = sourceDao.loadUsers(connection);
                    List<ArticleMediaSource> articles = sourceDao.loadArticles(connection);
                    if (users == null || articles == null) {
                        throw new java.sql.SQLException("回填数据源返回空结果集合");
                    }
                    return new SourceSnapshot(users, articles);
                } finally {
                    if (readOnlyKnown) {
                        try {
                            connection.setReadOnly(originalReadOnly);
                        } catch (java.sql.SQLException restoreError) {
                            LOGGER.error(
                                    "[MediaBackfillService#loadSources] 恢复数据库只读状态失败",
                                    restoreError
                            );
                        }
                    }
                }
            });
            if (snapshot == null) throw new IOException("回填数据源事务未返回快照");
            return snapshot;
        } catch (RuntimeException error) {
            LOGGER.error("[MediaBackfillService#loadSources] 读取回填数据源失败，已停止回填", error);
            throw new IOException("读取回填数据源失败，已停止回填", error);
        }
    }

    private ReferenceIndex collectReferences(
            List<UserMediaSource> users,
            List<ArticleMediaSource> articles
    ) {
        ReferenceIndex index = new ReferenceIndex();
        for (UserMediaSource user : users) {
            if (user == null) continue;
            Optional<ManagedMediaKey> avatarKey = ManagedMediaUrlParser.tryParse(user.getAvatar());
            if (avatarKey.isPresent()) {
                if (avatarKey.get().getMediaType() == MediaType.AVATAR) {
                    index.addAvatar(user.getId(), avatarKey.get());
                } else {
                    // 用户头像字段指向文章图或附件时不能猜测业务含义，交给人工处理。
                    index.manualReview++;
                }
            } else if (ManagedMediaUrlParser.isPotentialManagedUrl(user.getAvatar())) {
                index.manualReview++;
            }
        }
        for (ArticleMediaSource article : articles) {
            if (article == null) continue;
            try {
                index.manualReview += MediaReferenceExtractor.countManualReviewCandidates(
                        article.getContent(), article.getCoverImage());
                index.addArticle(
                        article.getId(),
                        MediaReferenceExtractor.extract(article.getContent(), article.getCoverImage())
                );
            } catch (RuntimeException error) {
                index.manualReview++;
                LOGGER.error(
                        "[MediaBackfillService#collectReferences] 解析历史文章媒体引用失败，articleId={}",
                        article.getId(),
                        error
                );
            }
        }
        return index;
    }

    private ScanResult scanFiles(Path storageRoot) throws IOException {
        ScanResult result = new ScanResult();
        scanDirectory(
                storageRoot.resolve("image"),
                result,
                Set.of(MediaType.AVATAR, MediaType.ARTICLE_IMAGE)
        );
        scanDirectory(storageRoot.resolve("file"), result, Set.of(MediaType.ATTACHMENT));
        return result;
    }

    private void scanDirectory(
            Path directory,
            ScanResult result,
            Set<MediaType> allowedTypes
    ) throws IOException {
        if (!Files.isDirectory(directory)) return;
        try (Stream<Path> paths = Files.list(directory)) {
            paths.filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(Path::toString))
                    .forEach(path -> scanFile(path, result, allowedTypes));
        } catch (IOException error) {
            throw new IOException("扫描历史媒体目录失败：" + directory, error);
        }
    }

    private void scanFile(Path path, ScanResult result, Set<MediaType> allowedTypes) {
        result.filesScanned++;
        ManagedMediaKey key = parsePhysicalKey(path.getFileName().toString(), allowedTypes);
        if (key == null) {
            result.manualReview++;
            return;
        }
        if (result.files.containsKey(key)) {
            result.duplicateKeys++;
            result.manualReview++;
            return;
        }
        try {
            long size = Files.size(path);
            String hash = sha256(path);
            String contentType = contentType(path, key.getMediaType());
            MediaAsset asset = new MediaAsset();
            asset.setMediaType(key.getMediaType());
            asset.setStorageName(key.getStorageName());
            asset.setUrlFileName(key.getUrlFileName());
            // 历史物理文件名无法反推出浏览器原名，回填使用稳定 URL 文件名避免伪造原名。
            asset.setOriginalName(key.getUrlFileName());
            asset.setContentType(contentType);
            asset.setSizeBytes(size);
            asset.setSha256(hash);
            result.files.put(key, new PhysicalMedia(path, asset));
        } catch (IOException error) {
            result.hashFailures++;
            result.manualReview++;
            result.failedKeys.add(key);
            LOGGER.error(
                    "[MediaBackfillService#scanFile] 计算历史媒体元数据失败，path={}",
                    path,
                    error
            );
        }
    }

    private ManagedMediaKey parsePhysicalKey(String fileName, Set<MediaType> allowedTypes) {
        if (fileName == null) return null;
        if (fileName.startsWith("avatar_")) {
            return allowedTypes.contains(MediaType.AVATAR)
                    ? safeKey(MediaType.AVATAR, fileName.substring("avatar_".length()))
                    : null;
        }
        if (fileName.startsWith("image_")) {
            return allowedTypes.contains(MediaType.ARTICLE_IMAGE)
                    ? safeKey(MediaType.ARTICLE_IMAGE, fileName.substring("image_".length()))
                    : null;
        }
        if (fileName.startsWith("file_")) {
            return allowedTypes.contains(MediaType.ATTACHMENT)
                    ? safeKey(MediaType.ATTACHMENT, fileName.substring("file_".length()))
                    : null;
        }
        return null;
    }

    private ManagedMediaKey safeKey(MediaType type, String urlFileName) {
        try {
            return new ManagedMediaKey(type, urlFileName);
        } catch (IllegalArgumentException error) {
            return null;
        }
    }

    private String contentType(Path path, MediaType type) {
        String fileName = path.getFileName().toString();
        String detected = null;
        try {
            detected = Files.probeContentType(path);
        } catch (IOException error) {
            LOGGER.warn(
                    "[MediaBackfillService#contentType] 探测历史媒体 MIME 失败，fileName={}",
                    fileName,
                    error
            );
        }
        if (detected != null && !detected.trim().isEmpty()) return detected;
        if (type == MediaType.AVATAR || type == MediaType.ARTICLE_IMAGE) {
            String imageType = UploadPolicy.imageContentType(fileName);
            if (imageType != null) return imageType;
        }
        return type == MediaType.ATTACHMENT ? "application/octet-stream" : null;
    }

    private String sha256(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(path)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    if (read > 0) digest.update(buffer, 0, read);
                }
            }
            return hex(digest.digest());
        } catch (NoSuchAlgorithmException error) {
            throw new IOException("JVM 不支持 SHA-256", error);
        }
    }

    private String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            result.append(Character.forDigit((value >>> 4) & 0x0f, 16));
            result.append(Character.forDigit(value & 0x0f, 16));
        }
        return result.toString();
    }

    private void classify(
            ScanResult scan,
            ReferenceIndex references,
            BackfillReport.Builder report
    ) {
        int active = 0;
        int protectedAssets = 0;
        for (ManagedMediaKey key : scan.files.keySet()) {
            if (references.allReferences.containsKey(key)) active++;
            else protectedAssets++;
        }
        int missing = 0;
        for (ManagedMediaKey key : references.allReferences.keySet()) {
            if (!scan.files.containsKey(key) && !scan.failedKeys.contains(key)) missing++;
        }
        report.activeAssets(active)
                .protectedAssets(protectedAssets)
                .missingBinary(missing);
    }

    private void apply(
            ScanResult scan,
            ReferenceIndex references,
            BackfillReport.Builder report
    ) {
        Set<ManagedMediaKey> allKeys = new LinkedHashSet<>();
        allKeys.addAll(scan.files.keySet());
        allKeys.addAll(references.allReferences.keySet());
        if (allKeys.isEmpty()) return;
        if (!Collections.disjoint(scan.failedKeys, references.allReferences.keySet())) {
            throw new IllegalStateException(
                    "存在无法读取且被历史数据引用的媒体文件，已停止 apply，请先人工复核："
                            + scan.failedKeys
            );
        }

        try {
            transactionManager.inTransaction(connection -> {
                Map<ManagedMediaKey, MediaAsset> assets = loadExisting(connection, allKeys);
                List<MediaAsset> assetsToInsert = new ArrayList<>();
                List<BackfillAssetUpdate> assetsToUpdate = new ArrayList<>();
                for (ManagedMediaKey key : sortedKeys(allKeys)) {
                    MediaAsset existing = assets.get(key);
                    if (existing == null) {
                        MediaAsset asset = newAssetForBackfill(key, scan, references);
                        assetsToInsert.add(asset);
                        assets.put(key, asset);
                    } else {
                        BackfillAssetUpdate update = planExistingAsset(
                                key,
                                existing,
                                scan,
                                references
                        );
                        if (update != null) assetsToUpdate.add(update);
                    }
                }
                // 先完成全部状态冲突预检查，再开始写入，避免维护命令产生部分结果。
                for (MediaAsset asset : assetsToInsert) {
                    mediaDao.insert(connection, asset);
                }
                for (BackfillAssetUpdate update : assetsToUpdate) {
                    mediaDao.reconcileBackfillAsset(
                            connection,
                            update.desired,
                            update.expectedStatus,
                            update.fillOnlyMissing
                    );
                    update.existing.setStatus(update.desired.getStatus());
                }
                applyAvatarReferences(connection, assets, references);
                applyArticleReferences(connection, assets, references);
                return true;
            });
        } catch (RuntimeException error) {
            LOGGER.error(
                    "[MediaBackfillService#apply] 历史媒体回填事务失败，assetKeyCount={}",
                    allKeys.size(),
                    error
            );
            throw error;
        }
    }

    private Map<ManagedMediaKey, MediaAsset> loadExisting(
            Connection connection,
            Set<ManagedMediaKey> keys
    ) throws java.sql.SQLException {
        Map<ManagedMediaKey, MediaAsset> result = new HashMap<>();
        List<ManagedMediaKey> sorted = sortedKeys(keys);
        for (int start = 0; start < sorted.size(); start += EXISTING_ASSET_BATCH_SIZE) {
            int end = Math.min(start + EXISTING_ASSET_BATCH_SIZE, sorted.size());
            List<ManagedMediaKey> batch = sorted.subList(start, end);
            List<MediaAsset> existing = mediaDao.findByKeysForUpdate(
                    connection,
                    new LinkedHashSet<>(batch)
            );
            if (existing == null) continue;
            for (MediaAsset asset : existing) {
                if (asset != null && asset.getUrlFileName() != null && asset.getMediaType() != null) {
                    result.put(new ManagedMediaKey(asset.getMediaType(), asset.getUrlFileName()), asset);
                }
            }
        }
        return result;
    }

    private BackfillAssetUpdate planExistingAsset(
            ManagedMediaKey key,
            MediaAsset existing,
            ScanResult scan,
            ReferenceIndex references
    ) {
        if (existing.getStatus() == null) {
            throw manualReviewFailure("已有媒体资产没有状态，media=" + key);
        }
        PhysicalMedia physical = scan.files.get(key);
        boolean referenced = references.allReferences.containsKey(key);
        if (physical != null && !key.getStorageName().equals(existing.getStorageName())) {
            throw manualReviewFailure("已有媒体 storage_name 与物理文件不一致，media=" + key);
        }
        switch (existing.getStatus()) {
            case MISSING_BINARY:
                if (physical == null) return null;
                return new BackfillAssetUpdate(
                        existing,
                        desiredFromExisting(physical, existing, referenced
                                ? MediaStatus.ACTIVE
                                : MediaStatus.LEGACY_PROTECTED),
                        MediaStatus.MISSING_BINARY,
                        false
                );
            case LEGACY_PROTECTED:
                if (physical == null || !hasMissingMetadata(existing)) return null;
                return new BackfillAssetUpdate(
                        existing,
                        desiredFromExisting(physical, existing, MediaStatus.LEGACY_PROTECTED),
                        MediaStatus.LEGACY_PROTECTED,
                        true
                );
            case ACTIVE:
                if (physical == null) {
                    throw manualReviewFailure("ACTIVE 媒体缺少物理文件，media=" + key);
                }
                ensureNoMetadataConflict(key, existing, physical.asset);
                if (!hasMissingMetadata(existing)) return null;
                return new BackfillAssetUpdate(
                        existing,
                        desiredFromExisting(physical, existing, MediaStatus.ACTIVE),
                        MediaStatus.ACTIVE,
                        true
                );
            case DELETED:
                throw manualReviewFailure("DELETED 媒体重新出现文件或引用，media=" + key);
            case DELETING:
            case DELETE_FAILED:
            case DELETE_PENDING:
                if (referenced) {
                    throw manualReviewFailure(
                            "媒体正在清理期间出现历史引用，media=" + key
                                    + "，status=" + existing.getStatus()
                    );
                }
                // 回填不与清理器争抢状态；无引用资产留给清理器按原状态处理。
                return null;
            case TEMP:
                if (physical == null || !referenced) return null;
                return new BackfillAssetUpdate(
                        existing,
                        desiredFromExisting(physical, existing, MediaStatus.ACTIVE),
                        MediaStatus.TEMP,
                        false
                );
            default:
                throw manualReviewFailure("未知媒体状态，media=" + key);
        }
    }

    private MediaAsset desiredFromPhysical(PhysicalMedia physical, MediaStatus status) {
        MediaAsset desired = new MediaAsset();
        desired.setId(physical.asset.getId());
        desired.setMediaType(physical.asset.getMediaType());
        desired.setStorageName(physical.asset.getStorageName());
        desired.setUrlFileName(physical.asset.getUrlFileName());
        desired.setOriginalName(physical.asset.getOriginalName());
        desired.setContentType(physical.asset.getContentType());
        desired.setSizeBytes(physical.asset.getSizeBytes());
        desired.setSha256(physical.asset.getSha256());
        desired.setStatus(status);
        return desired;
    }

    private MediaAsset desiredFromExisting(
            PhysicalMedia physical,
            MediaAsset existing,
            MediaStatus status
    ) {
        MediaAsset desired = desiredFromPhysical(physical, status);
        desired.setId(existing.getId());
        return desired;
    }

    private boolean hasMissingMetadata(MediaAsset asset) {
        return asset.getOriginalName() == null
                || asset.getContentType() == null
                || asset.getSizeBytes() == null
                || asset.getSha256() == null;
    }

    private void ensureNoMetadataConflict(
            ManagedMediaKey key,
            MediaAsset existing,
            MediaAsset scanned
    ) {
        if (differs(existing.getContentType(), scanned.getContentType())
                || differs(existing.getSizeBytes(), scanned.getSizeBytes())
                || differs(existing.getSha256(), scanned.getSha256())) {
            throw manualReviewFailure("ACTIVE 媒体元数据与物理文件冲突，media=" + key);
        }
    }

    private boolean differs(Object existing, Object scanned) {
        return existing != null && scanned != null && !existing.equals(scanned);
    }

    private IllegalStateException manualReviewFailure(String message) {
        LOGGER.error("[MediaBackfillService#planExistingAsset] {}，已停止 apply", message);
        return new IllegalStateException(message);
    }

    private MediaAsset newAssetForBackfill(
            ManagedMediaKey key,
            ScanResult scan,
            ReferenceIndex references
    ) {
        PhysicalMedia physical = scan.files.get(key);
        if (physical != null) {
            MediaAsset asset = physical.asset;
            asset.setStatus(references.allReferences.containsKey(key)
                    ? MediaStatus.ACTIVE
                    : MediaStatus.LEGACY_PROTECTED);
            return asset;
        }
        MediaAsset missing = new MediaAsset();
        missing.setMediaType(key.getMediaType());
        missing.setStorageName(key.getStorageName());
        missing.setUrlFileName(key.getUrlFileName());
        missing.setOriginalName(key.getUrlFileName());
        missing.setStatus(MediaStatus.MISSING_BINARY);
        return missing;
    }

    private void applyAvatarReferences(
            Connection connection,
            Map<ManagedMediaKey, MediaAsset> assets,
            ReferenceIndex references
    ) throws java.sql.SQLException {
        for (Map.Entry<Integer, ManagedMediaKey> entry : references.avatarByUser.entrySet()) {
            MediaAsset asset = assets.get(entry.getValue());
            if (asset == null || asset.getId() == null) {
                throw new java.sql.SQLException("历史头像缺少媒体资产，userId=" + entry.getKey());
            }
            mediaDao.replaceAvatarReference(connection, entry.getKey(), asset.getId());
        }
    }

    private void applyArticleReferences(
            Connection connection,
            Map<ManagedMediaKey, MediaAsset> assets,
            ReferenceIndex references
    ) throws java.sql.SQLException {
        for (Map.Entry<Integer, Map<ManagedMediaKey, Set<MediaReferenceType>>> article
                : references.articleReferences.entrySet()) {
            Map<Long, Set<MediaReferenceType>> byId = new LinkedHashMap<>();
            for (Map.Entry<ManagedMediaKey, Set<MediaReferenceType>> reference
                    : article.getValue().entrySet()) {
                MediaAsset asset = assets.get(reference.getKey());
                if (asset == null || asset.getId() == null) {
                    throw new java.sql.SQLException(
                            "历史文章引用缺少媒体资产，articleId=" + article.getKey());
                }
                byId.put(asset.getId(), reference.getValue());
            }
            mediaDao.replaceArticleReferences(connection, article.getKey(), byId);
        }
    }

    private List<ManagedMediaKey> sortedKeys(Set<ManagedMediaKey> keys) {
        List<ManagedMediaKey> sorted = new ArrayList<>(keys);
        sorted.sort(Comparator.comparing(ManagedMediaKey::toString));
        return sorted;
    }

    private Path resolveStorageRoot() throws IOException {
        Path root = configuredStorageRoot == null
                ? StorageRootResolver.resolve()
                : configuredStorageRoot;
        return StorageRootResolver.requireExistingReadableRoot(root);
    }

    private static final class SourceSnapshot {
        private final List<UserMediaSource> users;
        private final List<ArticleMediaSource> articles;

        private SourceSnapshot(List<UserMediaSource> users, List<ArticleMediaSource> articles) {
            this.users = users;
            this.articles = articles;
        }
    }

    private static final class BackfillAssetUpdate {
        private final MediaAsset existing;
        private final MediaAsset desired;
        private final MediaStatus expectedStatus;
        private final boolean fillOnlyMissing;

        private BackfillAssetUpdate(
                MediaAsset existing,
                MediaAsset desired,
                MediaStatus expectedStatus,
                boolean fillOnlyMissing
        ) {
            this.existing = existing;
            this.desired = desired;
            this.expectedStatus = expectedStatus;
            this.fillOnlyMissing = fillOnlyMissing;
        }
    }

    private static final class ReferenceIndex {
        private final Map<ManagedMediaKey, Set<MediaReferenceType>> allReferences =
                new LinkedHashMap<>();
        private final Map<Integer, ManagedMediaKey> avatarByUser = new LinkedHashMap<>();
        private final Map<Integer, Map<ManagedMediaKey, Set<MediaReferenceType>>> articleReferences =
                new LinkedHashMap<>();
        private int manualReview;

        private void addAvatar(int userId, ManagedMediaKey key) {
            avatarByUser.put(userId, key);
            allReferences.computeIfAbsent(key, ignored -> new LinkedHashSet<>())
                    .add(MediaReferenceType.USER_AVATAR);
        }

        private void addArticle(
                int articleId,
                Map<ManagedMediaKey, Set<MediaReferenceType>> references
        ) {
            Map<ManagedMediaKey, Set<MediaReferenceType>> safe = new LinkedHashMap<>();
            if (references != null) {
                for (Map.Entry<ManagedMediaKey, Set<MediaReferenceType>> entry
                        : references.entrySet()) {
                    Set<MediaReferenceType> allowed = new LinkedHashSet<>();
                    for (MediaReferenceType type : entry.getValue()) {
                        if (type == MediaReferenceType.ARTICLE_COVER
                                && entry.getKey().getMediaType() != MediaType.ARTICLE_IMAGE) {
                            // 回填必须遵循在线保存规则，避免把历史头像或非法封面写成文章引用。
                            manualReview++;
                            continue;
                        }
                        allowed.add(type);
                    }
                    if (!allowed.isEmpty()) safe.put(entry.getKey(), allowed);
                }
            }
            articleReferences.put(articleId, safe);
            for (Map.Entry<ManagedMediaKey, Set<MediaReferenceType>> entry : safe.entrySet()) {
                allReferences.computeIfAbsent(entry.getKey(), ignored -> new LinkedHashSet<>())
                        .addAll(entry.getValue());
            }
        }

        private int referenceCount() {
            int count = avatarByUser.size();
            for (Map<ManagedMediaKey, Set<MediaReferenceType>> article : articleReferences.values()) {
                for (Set<MediaReferenceType> types : article.values()) count += types.size();
            }
            return count;
        }
    }

    private static final class ScanResult {
        private final Map<ManagedMediaKey, PhysicalMedia> files = new LinkedHashMap<>();
        private final Set<ManagedMediaKey> failedKeys = new LinkedHashSet<>();
        private int filesScanned;
        private int manualReview;
        private int duplicateKeys;
        private int hashFailures;
    }

    private static final class PhysicalMedia {
        private final Path path;
        private final MediaAsset asset;

        private PhysicalMedia(Path path, MediaAsset asset) {
            this.path = path;
            this.asset = asset;
        }
    }

    public static final class BackfillReport {
        private final String storageRoot;
        private final String sourceSummary;
        private final int usersScanned;
        private final int articlesScanned;
        private final int filesScanned;
        private final int activeAssets;
        private final int protectedAssets;
        private final int missingBinary;
        private final int referencesCreated;
        private final int manualReview;
        private final int duplicateKeys;
        private final int hashFailures;

        private BackfillReport(Builder builder) {
            this.storageRoot = builder.storageRoot;
            this.sourceSummary = builder.sourceSummary;
            this.usersScanned = builder.usersScanned;
            this.articlesScanned = builder.articlesScanned;
            this.filesScanned = builder.filesScanned;
            this.activeAssets = builder.activeAssets;
            this.protectedAssets = builder.protectedAssets;
            this.missingBinary = builder.missingBinary;
            this.referencesCreated = builder.referencesCreated;
            this.manualReview = builder.manualReview;
            this.duplicateKeys = builder.duplicateKeys;
            this.hashFailures = builder.hashFailures;
        }

        public String getStorageRoot() { return storageRoot; }
        public String getSourceSummary() { return sourceSummary; }
        public int getUsersScanned() { return usersScanned; }
        public int getArticlesScanned() { return articlesScanned; }
        public int getFilesScanned() { return filesScanned; }
        public int getActiveAssets() { return activeAssets; }
        public int getProtectedAssets() { return protectedAssets; }
        public int getMissingBinary() { return missingBinary; }
        public int getReferencesCreated() { return referencesCreated; }
        public int getManualReview() { return manualReview; }
        public int getDuplicateKeys() { return duplicateKeys; }
        public int getHashFailures() { return hashFailures; }

        public Map<String, Object> asMap() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("storageRoot", storageRoot);
            result.put("sourceSummary", sourceSummary);
            result.put("usersScanned", usersScanned);
            result.put("articlesScanned", articlesScanned);
            result.put("filesScanned", filesScanned);
            result.put("activeAssets", activeAssets);
            result.put("protectedAssets", protectedAssets);
            result.put("missingBinary", missingBinary);
            result.put("referencesCreated", referencesCreated);
            result.put("manualReview", manualReview);
            result.put("duplicateKeys", duplicateKeys);
            result.put("hashFailures", hashFailures);
            return result;
        }

        @Override
        public String toString() {
            StringJoiner values = new StringJoiner(", ", "{", "}");
            for (Map.Entry<String, Object> entry : asMap().entrySet()) {
                values.add(entry.getKey() + "=" + entry.getValue());
            }
            return values.toString();
        }

        private static final class Builder {
            private String storageRoot;
            private String sourceSummary;
            private int usersScanned;
            private int articlesScanned;
            private int filesScanned;
            private int activeAssets;
            private int protectedAssets;
            private int missingBinary;
            private int referencesCreated;
            private int manualReview;
            private int duplicateKeys;
            private int hashFailures;

            private Builder storageRoot(String value) { storageRoot = value; return this; }
            private Builder sourceSummary(String value) { sourceSummary = value; return this; }
            private Builder usersScanned(int value) { usersScanned = value; return this; }
            private Builder articlesScanned(int value) { articlesScanned = value; return this; }
            private Builder filesScanned(int value) { filesScanned = value; return this; }
            private Builder activeAssets(int value) { activeAssets = value; return this; }
            private Builder protectedAssets(int value) { protectedAssets = value; return this; }
            private Builder missingBinary(int value) { missingBinary = value; return this; }
            private Builder referencesCreated(int value) { referencesCreated = value; return this; }
            private Builder manualReview(int value) { manualReview = value; return this; }
            private Builder duplicateKeys(int value) { duplicateKeys = value; return this; }
            private Builder hashFailures(int value) { hashFailures = value; return this; }

            private BackfillReport build() {
                return new BackfillReport(this);
            }
        }
    }
}
