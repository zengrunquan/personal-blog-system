package com.blog.media;

import com.blog.media.dao.MediaDao;
import com.blog.media.model.ManagedMediaKey;
import com.blog.media.model.MediaAsset;
import com.blog.media.model.MediaReferenceType;
import com.blog.media.model.MediaStatus;
import com.blog.media.service.MediaCleanupConfig;
import com.blog.media.service.CleanupReport;
import com.blog.media.service.MediaCleanupService;
import com.blog.media.storage.MediaFileStore;
import com.blog.util.TransactionManager;
import org.junit.Test;

import javax.servlet.http.Part;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MediaCleanupServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-01T00:00:00Z");

    @Test
    public void claimFailureShouldFailRoundAndAllowNextRoundToRecover() {
        FakeMediaDao dao = new FakeMediaDao(null);
        FakeFileStore files = new FakeFileStore();
        MediaCleanupService service = service(dao, files, enabledConfig());
        dao.claimFailure = new IllegalStateException("认领 SQL 执行失败");
        RuntimeException error = org.junit.Assert.assertThrows(RuntimeException.class, service::runOnce);
        org.junit.Assert.assertSame(dao.claimFailure, error.getCause());
        assertEquals(0, files.deleteCalls);
        dao.claimFailure = null;
        assertEquals(0, service.runOnce().getAssetsClaimed());
    }

    @Test
    public void preparationFailureShouldAbortBeforeClaiming() {
        FakeMediaDao dao = new FakeMediaDao(asset(5L));
        FakeFileStore files = new FakeFileStore();
        dao.recoveryFailure = new IllegalStateException("恢复认领失败");
        org.junit.Assert.assertThrows(RuntimeException.class,
                () -> service(dao, files, enabledConfig()).runOnce());
        assertEquals(0, dao.claimCalls);
        assertEquals(0, files.deleteCalls);
    }

    @Test
    public void dueAssetShouldBeClaimedDeletedAndMarkedDeleted() {
        FakeMediaDao dao = new FakeMediaDao(asset(1L));
        FakeFileStore files = new FakeFileStore();
        MediaCleanupService service = service(dao, files, new MediaCleanupConfig(
                true, 24, 60, 100, 60
        ));

        CleanupReport report = service.runOnce();
        assertEquals(1, report.getAssetsClaimed());
        assertEquals(1, report.getFilesDeleted());
        assertEquals(0, report.getFilesAlreadyMissing());
        assertEquals(0, report.getDeleteFailures());
        assertTrue(report.getElapsedMillis() >= 0);
        assertEquals(1, files.deleteCalls);
        assertEquals(1, dao.markDeletedCalls);
        assertNotNull(dao.claimToken);
        assertEquals(MediaStatus.DELETED, dao.asset.getStatus());
    }

    @Test
    public void missingFileShouldStillBeMarkedDeleted() {
        FakeMediaDao dao = new FakeMediaDao(asset(2L));
        FakeFileStore files = new FakeFileStore();
        files.deleteResult = false;

        CleanupReport report = service(dao, files, enabledConfig()).runOnce();
        assertEquals(1, report.getAssetsClaimed());
        assertEquals(0, report.getFilesDeleted());
        assertEquals(1, report.getFilesAlreadyMissing());
        assertEquals(0, report.getDeleteFailures());
        assertEquals(1, dao.markDeletedCalls);
        assertEquals(0, dao.markFailedCalls);
    }

    @Test
    public void ioFailureShouldScheduleBoundedRetryAndContinue() {
        FakeMediaDao dao = new FakeMediaDao(asset(3L));
        FakeFileStore files = new FakeFileStore();
        files.failure = new IOException("disk is temporarily unavailable");

        CleanupReport report = service(dao, files, enabledConfig()).runOnce();
        assertEquals(1, report.getAssetsClaimed());
        assertEquals(0, report.getFilesDeleted());
        assertEquals(0, report.getFilesAlreadyMissing());
        assertEquals(1, report.getDeleteFailures());
        assertEquals(0, dao.markDeletedCalls);
        assertEquals(1, dao.markFailedCalls);
        assertEquals(NOW.plusSeconds(2 * 60 * 60), dao.retryAt);
        assertTrue(dao.safeError.length() <= 1000);
    }

    @Test
    public void disabledCleanupShouldNotTouchDaoOrFiles() {
        FakeMediaDao dao = new FakeMediaDao(asset(4L));
        FakeFileStore files = new FakeFileStore();

        CleanupReport report = service(dao, files, new MediaCleanupConfig(
                false, 24, 60, 100, 60
        )).runOnce();
        assertEquals(0, report.getAssetsClaimed());
        assertEquals(0, report.getFilesDeleted());
        assertEquals(0, dao.claimCalls);
        assertEquals(0, files.deleteCalls);
    }

    @Test
    public void cleanupRoundShouldRecoverClaimsAndMarkUnreferencedAssets() {
        FakeMediaDao dao = new FakeMediaDao(null);
        FakeFileStore files = new FakeFileStore();

        CleanupReport report = service(dao, files, enabledConfig()).runOnce();
        assertEquals(0, report.getAssetsClaimed());
        assertEquals(0, report.getStaleClaimsRecovered());
        assertEquals(0, report.getAssetsMarkedPending());
        assertEquals(1, dao.recoverCalls);
        assertEquals(1, dao.markUnreferencedCalls);
        assertEquals(NOW.plusSeconds(24 * 60 * 60), dao.deleteAfter);
    }

    private MediaCleanupService service(
            FakeMediaDao dao,
            FakeFileStore files,
            MediaCleanupConfig config
    ) {
        TransactionManager transactions = new TransactionManager() {
            @Override
            public <T> T inTransaction(com.blog.util.TransactionWork<T> work) {
                try {
                    return work.execute(null);
                } catch (Exception error) {
                    throw new RuntimeException(error);
                }
            }
        };
        return new MediaCleanupService(
                dao,
                files,
                transactions,
                Clock.fixed(NOW, ZoneOffset.UTC),
                config
        );
    }

    private MediaCleanupConfig enabledConfig() {
        return new MediaCleanupConfig(true, 24, 60, 100, 60);
    }

    private MediaAsset asset(long id) {
        MediaAsset asset = new MediaAsset();
        asset.setId(id);
        asset.setMediaType(com.blog.media.model.MediaType.ARTICLE_IMAGE);
        asset.setStorageName("image_asset-" + id + ".png");
        asset.setUrlFileName("asset-" + id + ".png");
        asset.setOriginalName("asset.png");
        asset.setStatus(MediaStatus.DELETE_PENDING);
        asset.setDeleteAfter(NOW.minusSeconds(1));
        asset.setDeleteAttempts(0);
        return asset;
    }

    private static final class FakeFileStore implements MediaFileStore {
        private int deleteCalls;
        private boolean deleteResult = true;
        private IOException failure;

        @Override
        public com.blog.media.model.StoredMedia store(Part part, com.blog.media.model.MediaType type) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Path resolve(MediaAsset asset) {
            return null;
        }

        @Override
        public boolean deleteIfExists(MediaAsset asset) throws IOException {
            deleteCalls++;
            if (failure != null) throw failure;
            return deleteResult;
        }
    }

    private static final class FakeMediaDao implements MediaDao {
        private final MediaAsset asset;
        private int claimCalls;
        private RuntimeException claimFailure;
        private RuntimeException recoveryFailure;
        private int recoverCalls;
        private int markUnreferencedCalls;
        private int markDeletedCalls;
        private int markFailedCalls;
        private String claimToken;
        private Instant retryAt;
        private Instant deleteAfter;
        private String safeError;

        private FakeMediaDao(MediaAsset asset) {
            this.asset = asset;
        }

        @Override
        public long insert(java.sql.Connection connection, MediaAsset value) {
            return value.getId() == null ? 1L : value.getId();
        }

        @Override
        public List<MediaAsset> findByKeysForUpdate(
                java.sql.Connection connection, Set<ManagedMediaKey> keys
        ) {
            return Collections.emptyList();
        }

        @Override
        public void replaceAvatarReference(java.sql.Connection connection, int userId, long mediaId) {
        }

        @Override
        public void replaceArticleReferences(
                java.sql.Connection connection,
                int articleId,
                Map<Long, Set<MediaReferenceType>> references
        ) {
        }

        @Override
        public int markUnreferencedAssets(
                java.sql.Connection connection, Instant now, Instant deleteAfter
        ) {
            markUnreferencedCalls++;
            this.deleteAfter = deleteAfter;
            return 0;
        }

        @Override
        public Optional<MediaAsset> claimNextDueAsset(
                java.sql.Connection connection,
                String token,
                Instant now,
                Instant staleBefore
        ) {
            claimCalls++;
            if (claimFailure != null) throw claimFailure;
            if (asset == null
                    || asset.getStatus() == MediaStatus.DELETED
                    || asset.getStatus() == MediaStatus.DELETE_FAILED) {
                return Optional.empty();
            }
            claimToken = token;
            asset.setStatus(MediaStatus.DELETING);
            asset.setClaimToken(token);
            asset.setDeleteAttempts(asset.getDeleteAttempts() + 1);
            return Optional.of(asset);
        }

        @Override
        public void markDeleted(java.sql.Connection connection, long mediaId, String token) {
            markDeletedCalls++;
            asset.setStatus(MediaStatus.DELETED);
        }

        @Override
        public void markDeleteFailed(
                java.sql.Connection connection,
                long mediaId,
                String token,
                Instant retryAt,
                String safeError
        ) {
            markFailedCalls++;
            this.retryAt = retryAt;
            this.safeError = safeError;
            asset.setStatus(MediaStatus.DELETE_FAILED);
        }

        @Override
        public int recoverStaleClaims(
                java.sql.Connection connection, Instant staleBefore, Instant retryAt
        ) {
            recoverCalls++;
            if (recoveryFailure != null) throw recoveryFailure;
            return 0;
        }
    }
}
