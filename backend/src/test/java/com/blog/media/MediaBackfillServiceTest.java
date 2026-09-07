package com.blog.media;

import com.blog.media.dao.MediaDao;
import com.blog.media.maintenance.ArticleMediaSource;
import com.blog.media.model.ManagedMediaKey;
import com.blog.media.model.MediaAsset;
import com.blog.media.model.MediaReferenceType;
import com.blog.media.model.MediaStatus;
import com.blog.media.model.MediaType;
import com.blog.media.maintenance.MediaBackfillSourceDao;
import com.blog.media.maintenance.MediaBackfillService;
import com.blog.media.maintenance.UserMediaSource;
import com.blog.util.TransactionManager;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MediaBackfillServiceTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void dryRunShouldScanReferencesWithoutWritingDatabaseOrFiles() throws Exception {
        Path root = prepareFiles();
        MediaBackfillSourceDao source = mock(MediaBackfillSourceDao.class);
        when(source.loadUsers(any())).thenReturn(Collections.singletonList(userWithMissingAvatar()));
        when(source.loadArticles(any())).thenReturn(Collections.singletonList(articleWithReferences()));
        FakeMediaDao mediaDao = new FakeMediaDao();

        MediaBackfillService service = service(root, source, mediaDao);
        MediaBackfillService.BackfillReport report = service.run(false);

        assertEquals(2, report.getFilesScanned());
        assertEquals(2, report.getActiveAssets());
        assertEquals(1, report.getMissingBinary());
        assertEquals(0, report.getProtectedAssets());
        assertEquals(3, report.getReferencesCreated());
        assertEquals(1, report.getManualReview());
        assertEquals(source.getClass().getSimpleName(), report.getSourceSummary());
        assertEquals(0, mediaDao.insertCalls);
        assertEquals(0, mediaDao.referenceCalls);
        assertTrue(Files.exists(root.resolve("image/image_image-a.png")));
    }

    @Test
    public void applyShouldCreateMetadataAndReferencesWithoutMovingFiles() throws Exception {
        Path root = prepareFiles();
        MediaBackfillSourceDao source = mock(MediaBackfillSourceDao.class);
        when(source.loadUsers(any())).thenReturn(Collections.singletonList(userWithMissingAvatar()));
        when(source.loadArticles(any())).thenReturn(Collections.singletonList(articleWithReferences()));
        FakeMediaDao mediaDao = new FakeMediaDao();

        MediaBackfillService.BackfillReport report = service(
                root, source, mediaDao
        ).run(true);

        assertEquals(3, mediaDao.insertCalls);
        assertEquals(2, mediaDao.referenceCalls);
        assertEquals(3, report.getReferencesCreated());
        assertEquals(1, report.getManualReview());
        assertTrue(Files.exists(root.resolve("file/file_doc.pdf")));
        assertEquals(MediaStatus.ACTIVE, mediaDao.statuses.get("image-a.png"));
        assertEquals(MediaStatus.ACTIVE, mediaDao.statuses.get("doc.pdf"));
        assertEquals(MediaStatus.MISSING_BINARY, mediaDao.statuses.get("avatar-missing.png"));
    }

    @Test
    public void shouldReviewRecognizedPrefixWhenItIsInTheWrongDirectory() throws Exception {
        Path root = prepareFiles();
        Files.write(root.resolve("file/image-in-file.png"), new byte[]{7, 8, 9});
        MediaBackfillSourceDao source = mock(MediaBackfillSourceDao.class);
        when(source.loadUsers(any())).thenReturn(Collections.emptyList());
        when(source.loadArticles(any())).thenReturn(Collections.emptyList());

        MediaBackfillService.BackfillReport report = service(
                root, source, new FakeMediaDao()
        ).run(false);

        assertEquals(3, report.getFilesScanned());
        assertEquals(2, report.getProtectedAssets());
        assertEquals(1, report.getManualReview());
    }

    @Test
    public void applyShouldKeepAvatarReferencedByArticleContent() throws Exception {
        Path root = prepareFiles();
        Files.write(root.resolve("image/avatar_avatar-a.png"), new byte[]{10, 11, 12});
        MediaBackfillSourceDao source = mock(MediaBackfillSourceDao.class);
        when(source.loadUsers(any())).thenReturn(Collections.emptyList());
        when(source.loadArticles(any())).thenReturn(Collections.singletonList(
                new ArticleMediaSource(
                        10,
                        "<img src=\"/uploads/avatars/avatar-a.png\">",
                        null
                )
        ));
        FakeMediaDao mediaDao = new FakeMediaDao();

        MediaBackfillService.BackfillReport report = service(
                root, source, mediaDao
        ).run(true);

        assertEquals(0, report.getManualReview());
        assertEquals(1, report.getReferencesCreated());
        assertEquals(1, mediaDao.referenceCalls);
        assertTrue(mediaDao.articleReferenceTypes.contains(
                Collections.singleton(MediaReferenceType.ARTICLE_CONTENT)
        ));
    }

    @Test
    public void applyShouldReactivateMissingBinaryWhenPhysicalFileReturns() throws Exception {
        Path root = prepareFiles();
        MediaBackfillSourceDao source = sourceWithArticle(
                new ArticleMediaSource(10, "<img src=\"/uploads/images/image-a.png\">", null)
        );
        FakeMediaDao mediaDao = new FakeMediaDao();
        MediaAsset missing = existingAsset(
                100L, MediaStatus.MISSING_BINARY, MediaType.ARTICLE_IMAGE, "image-a.png"
        );
        missing.setOriginalName(null);
        missing.setContentType(null);
        missing.setSizeBytes(null);
        missing.setSha256(null);
        mediaDao.existing.put(new ManagedMediaKey(MediaType.ARTICLE_IMAGE, "image-a.png"), missing);

        service(root, source, mediaDao).run(true);

        assertEquals(1, mediaDao.insertCalls);
        assertEquals(1, mediaDao.reconcileCalls);
        assertEquals(MediaStatus.ACTIVE, missing.getStatus());
        assertTrue(missing.getSha256() != null && !missing.getSha256().isEmpty());
        assertEquals(1, mediaDao.referenceCalls);
    }

    @Test
    public void applyShouldKeepLegacyProtectedStateWhileFillingMissingMetadata() throws Exception {
        Path root = prepareFiles();
        MediaBackfillSourceDao source = emptySource();
        FakeMediaDao mediaDao = new FakeMediaDao();
        MediaAsset protectedAsset = existingAsset(
                101L, MediaStatus.LEGACY_PROTECTED, MediaType.ARTICLE_IMAGE, "image-a.png"
        );
        protectedAsset.setOriginalName(null);
        protectedAsset.setContentType(null);
        protectedAsset.setSizeBytes(null);
        protectedAsset.setSha256(null);
        mediaDao.existing.put(
                new ManagedMediaKey(MediaType.ARTICLE_IMAGE, "image-a.png"),
                protectedAsset
        );

        service(root, source, mediaDao).run(true);

        assertEquals(MediaStatus.LEGACY_PROTECTED, protectedAsset.getStatus());
        assertEquals(1, mediaDao.reconcileCalls);
        assertTrue(mediaDao.lastFillOnlyMissing);
        assertTrue(protectedAsset.getSha256() != null && !protectedAsset.getSha256().isEmpty());
    }

    @Test
    public void applyShouldStopOnStorageNameConflictInsteadOfRepointingAsset() throws Exception {
        Path root = prepareFiles();
        MediaBackfillSourceDao source = emptySource();
        FakeMediaDao mediaDao = new FakeMediaDao();
        MediaAsset existing = existingAsset(
                102L, MediaStatus.MISSING_BINARY, MediaType.ARTICLE_IMAGE, "image-a.png"
        );
        existing.setStorageName("image_legacy-name.png");
        mediaDao.existing.put(
                new ManagedMediaKey(MediaType.ARTICLE_IMAGE, "image-a.png"),
                existing
        );

        assertThrows(
                IllegalStateException.class,
                () -> service(root, source, mediaDao).run(true)
        );

        assertEquals(0, mediaDao.reconcileCalls);
    }

    @Test
    public void applyShouldStopOnActiveHashConflict() throws Exception {
        Path root = prepareFiles();
        MediaBackfillSourceDao source = sourceWithArticle(
                new ArticleMediaSource(10, "<img src=\"/uploads/images/image-a.png\">", null)
        );
        FakeMediaDao mediaDao = new FakeMediaDao();
        MediaAsset active = existingAsset(
                102L, MediaStatus.ACTIVE, MediaType.ARTICLE_IMAGE, "image-a.png"
        );
        active.setSha256("hash-from-another-file");
        mediaDao.existing.put(new ManagedMediaKey(MediaType.ARTICLE_IMAGE, "image-a.png"), active);

        assertThrows(
                IllegalStateException.class,
                () -> service(root, source, mediaDao).run(true)
        );

        assertEquals(0, mediaDao.reconcileCalls);
        assertEquals(0, mediaDao.referenceCalls);
    }

    @Test
    public void applyShouldStopWhenDeletedAssetReappears() throws Exception {
        Path root = prepareFiles();
        MediaBackfillSourceDao source = emptySource();
        FakeMediaDao mediaDao = new FakeMediaDao();
        MediaAsset deleted = existingAsset(
                103L, MediaStatus.DELETED, MediaType.ARTICLE_IMAGE, "image-a.png"
        );
        mediaDao.existing.put(new ManagedMediaKey(MediaType.ARTICLE_IMAGE, "image-a.png"), deleted);

        assertThrows(
                IllegalStateException.class,
                () -> service(root, source, mediaDao).run(true)
        );

        assertEquals(0, mediaDao.reconcileCalls);
        assertEquals(0, mediaDao.insertCalls);
    }

    @Test
    public void applyShouldLockExistingAssetsInStableBatches() throws Exception {
        Path root = temporaryFolder.newFolder("large-uploads").toPath();
        Files.createDirectories(root.resolve("image"));
        Files.createDirectories(root.resolve("file"));
        for (int i = 0; i < 301; i++) {
            Files.write(
                    root.resolve("image/image_asset-" + String.format("%03d", i) + ".png"),
                    new byte[]{1}
            );
        }
        FakeMediaDao mediaDao = new FakeMediaDao();

        service(root, emptySource(), mediaDao).run(true);

        assertEquals(2, mediaDao.findByKeysCalls);
        assertEquals(301, mediaDao.insertCalls);
    }

    @Test
    public void repeatedApplyShouldNotCreateDuplicateAssets() throws Exception {
        Path root = prepareFiles();
        FakeMediaDao mediaDao = new FakeMediaDao();
        MediaBackfillService service = service(root, emptySource(), mediaDao);

        service.run(true);
        int firstInsertCalls = mediaDao.insertCalls;
        service.run(true);

        assertEquals(2, firstInsertCalls);
        assertEquals(2, mediaDao.insertCalls);
    }

    @Test
    public void userSourceFailureShouldFailBeforeAnyMediaWrite() throws Exception {
        Path root = prepareFiles();
        MediaBackfillSourceDao source = mock(MediaBackfillSourceDao.class);
        when(source.loadUsers(any())).thenThrow(new java.sql.SQLException("user query failed"));
        FakeMediaDao mediaDao = new FakeMediaDao();

        assertThrows(IOException.class, () -> service(root, source, mediaDao).run(false));

        assertEquals(0, mediaDao.insertCalls);
        assertEquals(0, mediaDao.referenceCalls);
    }

    @Test
    public void articleSourceFailureShouldFailBeforeAnyMediaWrite() throws Exception {
        Path root = prepareFiles();
        MediaBackfillSourceDao source = mock(MediaBackfillSourceDao.class);
        when(source.loadUsers(any())).thenReturn(Collections.emptyList());
        when(source.loadArticles(any())).thenThrow(new java.sql.SQLException("article query failed"));
        FakeMediaDao mediaDao = new FakeMediaDao();

        assertThrows(IOException.class, () -> service(root, source, mediaDao).run(true));

        assertEquals(0, mediaDao.insertCalls);
        assertEquals(0, mediaDao.referenceCalls);
    }

    private Path prepareFiles() throws Exception {
        Path root = temporaryFolder.newFolder("uploads").toPath();
        Files.createDirectories(root.resolve("image"));
        Files.createDirectories(root.resolve("file"));
        Files.write(root.resolve("image/image_image-a.png"), new byte[]{1, 2, 3});
        Files.write(root.resolve("file/file_doc.pdf"), new byte[]{4, 5, 6});
        return root;
    }

    private UserMediaSource userWithMissingAvatar() {
        return new UserMediaSource(7, "/uploads/avatars/avatar-missing.png");
    }

    private ArticleMediaSource articleWithReferences() {
        String content =
                "<p>正文</p><img src=\"/uploads/images/image-a.png\">"
                        + "<a href=\"/api/files/doc.pdf/download\">附件</a>"
                        + "<img src=\"/uploads/images/bad%20name.png\">"
        ;
        return new ArticleMediaSource(9, content, null);
    }

    private MediaBackfillSourceDao emptySource() throws java.sql.SQLException {
        MediaBackfillSourceDao source = mock(MediaBackfillSourceDao.class);
        when(source.loadUsers(any())).thenReturn(Collections.emptyList());
        when(source.loadArticles(any())).thenReturn(Collections.emptyList());
        return source;
    }

    private MediaBackfillSourceDao sourceWithArticle(ArticleMediaSource article)
            throws java.sql.SQLException {
        MediaBackfillSourceDao source = mock(MediaBackfillSourceDao.class);
        when(source.loadUsers(any())).thenReturn(Collections.emptyList());
        when(source.loadArticles(any())).thenReturn(Collections.singletonList(article));
        return source;
    }

    private MediaAsset existingAsset(
            long id,
            MediaStatus status,
            MediaType type,
            String urlFileName
    ) {
        MediaAsset asset = new MediaAsset();
        asset.setId(id);
        asset.setMediaType(type);
        asset.setStorageName(type.getStoragePrefix() + urlFileName);
        asset.setUrlFileName(urlFileName);
        asset.setOriginalName(urlFileName);
        asset.setContentType("image/png");
        asset.setSizeBytes(3L);
        asset.setSha256("existing-hash");
        asset.setStatus(status);
        return asset;
    }

    private MediaBackfillService service(
            Path root,
            MediaBackfillSourceDao source,
            FakeMediaDao mediaDao
    ) {
        TransactionManager transactions = new TransactionManager() {
            @Override
        public <T> T inTransaction(com.blog.util.TransactionWork<T> work) {
            try {
                return work.execute(null);
            } catch (RuntimeException error) {
                throw error;
            } catch (Exception error) {
                throw new RuntimeException(error);
            }
            }
        };
        return new MediaBackfillService(
                source,
                mediaDao,
                transactions,
                root,
                Clock.fixed(Instant.parse("2026-09-01T00:00:00Z"), ZoneOffset.UTC)
        );
    }

    private static final class FakeMediaDao implements MediaDao {
        private int insertCalls;
        private int referenceCalls;
        private int reconcileCalls;
        private int findByKeysCalls;
        private long nextId = 1;
        private final Map<String, MediaStatus> statuses = new java.util.HashMap<>();
        private final List<Set<MediaReferenceType>> articleReferenceTypes = new ArrayList<>();
        private final Map<ManagedMediaKey, MediaAsset> existing = new java.util.HashMap<>();
        private boolean lastFillOnlyMissing;

        @Override
        public long insert(java.sql.Connection connection, MediaAsset asset) {
            insertCalls++;
            asset.setId(nextId++);
            statuses.put(asset.getUrlFileName(), asset.getStatus());
            existing.put(new ManagedMediaKey(asset.getMediaType(), asset.getUrlFileName()), asset);
            return asset.getId();
        }

        @Override
        public List<MediaAsset> findByKeysForUpdate(
                java.sql.Connection connection, Set<ManagedMediaKey> keys
        ) {
            findByKeysCalls++;
            List<MediaAsset> result = new ArrayList<>();
            for (ManagedMediaKey key : keys) {
                if (existing.containsKey(key)) result.add(existing.get(key));
            }
            return result;
        }

        @Override
        public void reconcileBackfillAsset(
                java.sql.Connection connection,
                MediaAsset asset,
                MediaStatus expectedStatus,
                boolean fillOnlyMissing
        ) {
            reconcileCalls++;
            lastFillOnlyMissing = fillOnlyMissing;
            MediaAsset current = existing.get(new ManagedMediaKey(
                    asset.getMediaType(), asset.getUrlFileName()
            ));
            if (current == null || current.getStatus() != expectedStatus) {
                throw new IllegalStateException("状态已变化");
            }
            if (!current.getId().equals(asset.getId())) {
                throw new IllegalStateException("回填更新缺少已有资产主键");
            }
            if (!fillOnlyMissing || current.getOriginalName() == null) {
                current.setOriginalName(asset.getOriginalName());
            }
            if (!fillOnlyMissing || current.getContentType() == null) {
                current.setContentType(asset.getContentType());
            }
            if (!fillOnlyMissing || current.getSizeBytes() == null) {
                current.setSizeBytes(asset.getSizeBytes());
            }
            if (!fillOnlyMissing || current.getSha256() == null) {
                current.setSha256(asset.getSha256());
            }
            current.setStatus(asset.getStatus());
        }

        @Override
        public void replaceAvatarReference(java.sql.Connection connection, int userId, long mediaId) {
            referenceCalls++;
        }

        @Override
        public void replaceArticleReferences(
                java.sql.Connection connection,
                int articleId,
                Map<Long, Set<MediaReferenceType>> references
        ) {
            referenceCalls++;
            for (Set<MediaReferenceType> types : references.values()) {
                articleReferenceTypes.add(types);
            }
        }

        @Override
        public int markUnreferencedAssets(
                java.sql.Connection connection,
                Instant now,
                Instant deleteAfter
        ) {
            return 0;
        }

        @Override
        public Optional<MediaAsset> claimNextDueAsset(
                java.sql.Connection connection,
                String claimToken,
                Instant now,
                Instant staleBefore
        ) {
            return Optional.empty();
        }

        @Override
        public void markDeleted(java.sql.Connection connection, long mediaId, String claimToken) {
        }

        @Override
        public void markDeleteFailed(
                java.sql.Connection connection,
                long mediaId,
                String claimToken,
                Instant retryAt,
                String safeError
        ) {
        }

        @Override
        public int recoverStaleClaims(
                java.sql.Connection connection,
                Instant staleBefore,
                Instant retryAt
        ) {
            return 0;
        }
    }
}
