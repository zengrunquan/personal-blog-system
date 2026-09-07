package com.blog.media.service;

import com.blog.media.dao.MediaDao;
import com.blog.media.dao.MediaDaoImpl;
import com.blog.media.model.MediaAsset;
import com.blog.media.storage.LocalMediaFileStore;
import com.blog.media.storage.MediaFileStore;
import com.blog.util.JdbcTransactionManager;
import com.blog.util.TransactionManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** 延迟媒体回收服务；数据库认领和物理删除分离，支持多实例并发和崩溃恢复。 */
public class MediaCleanupService {

    private static final Logger LOGGER = LogManager.getLogger(MediaCleanupService.class);
    private static final long MAX_RETRY_HOURS = 24L;
    private final MediaDao mediaDao;
    private final MediaFileStore fileStore;
    private final TransactionManager transactionManager;
    private final Clock clock;
    private final MediaCleanupConfig config;

    public MediaCleanupService() {
        this(MediaCleanupConfig.fromSystemProperties());
    }

    public MediaCleanupService(MediaCleanupConfig config) {
        this(
                new MediaDaoImpl(),
                new LocalMediaFileStore(),
                new JdbcTransactionManager(),
                Clock.systemUTC(),
                config
        );
    }

    public MediaCleanupService(
            MediaDao mediaDao,
            MediaFileStore fileStore,
            TransactionManager transactionManager,
            Clock clock,
            MediaCleanupConfig config
    ) {
        this.mediaDao = Objects.requireNonNull(mediaDao, "mediaDao 不能为空");
        this.fileStore = Objects.requireNonNull(fileStore, "fileStore 不能为空");
        this.transactionManager = Objects.requireNonNull(
                transactionManager, "transactionManager 不能为空");
        this.clock = Objects.requireNonNull(clock, "clock 不能为空");
        this.config = Objects.requireNonNull(config, "config 不能为空");
    }

    /** 执行一轮清理并返回可审计摘要；单个文件失败不会中断后续认领。 */
    public CleanupReport runOnce() {
        long startedNanos = System.nanoTime();
        CleanupReport.Builder report = CleanupReport.builder();
        if (!config.isEnabled()) {
            return report.elapsedMillis(elapsedMillis(startedNanos)).build();
        }
        Instant now = clock.instant();
        try {
            PreparationResult preparation = recoverAndMarkUnreferenced(now);
            report.addStaleClaimsRecovered(preparation.staleClaimsRecovered)
                    .addAssetsMarkedPending(preparation.assetsMarkedPending);

            int processed = 0;
            while (processed < config.getBatchSize()) {
                Optional<MediaAsset> claimed = claim(now);
                if (!claimed.isPresent()) break;
                MediaAsset asset = claimed.get();
                processed++;
                report.incrementAssetsClaimed();
                deleteClaimedAsset(asset, now, report);
            }
        } finally {
            report.elapsedMillis(elapsedMillis(startedNanos));
        }
        return report.build();
    }

    private PreparationResult recoverAndMarkUnreferenced(Instant now) {
        try {
            PreparationResult result = transactionManager.inTransaction(connection -> {
                Instant staleBefore = now.minus(config.claimTimeoutDuration());
                int recovered = mediaDao.recoverStaleClaims(connection, staleBefore, now);
                int markedPending = mediaDao.markUnreferencedAssets(
                        connection,
                        now,
                        now.plus(config.retentionDuration())
                );
                return new PreparationResult(recovered, markedPending);
            });
            return result == null ? PreparationResult.EMPTY : result;
        } catch (RuntimeException error) {
            LOGGER.error(
                    "[MediaCleanupService#recoverAndMarkUnreferenced] 恢复认领或标记无引用资产失败，now={}",
                    now,
                    error
            );
            throw error;
        }
    }

    private Optional<MediaAsset> claim(Instant now) {
        String token = UUID.randomUUID().toString();
        try {
            Optional<MediaAsset> result = transactionManager.inTransaction(connection ->
                    mediaDao.claimNextDueAsset(
                            connection,
                            token,
                            now,
                            now.minus(config.claimTimeoutDuration())
                    )
            );
            return result == null ? Optional.empty() : result;
        } catch (RuntimeException error) {
            LOGGER.error(
                    "[MediaCleanupService#claim] 认领媒体资产失败，claimToken={}",
                    token,
                    error
            );
            // 数据库失败不等于没有待清理资产；由调度器记录轮次异常并保留后续调度。
            throw error;
        }
    }

    private void deleteClaimedAsset(
            MediaAsset asset,
            Instant now,
            CleanupReport.Builder report
    ) {
        String token = asset.getClaimToken();
        try {
            if (fileStore.deleteIfExists(asset)) {
                report.incrementFilesDeleted();
            } else {
                report.incrementFilesAlreadyMissing();
            }
            markDeleted(asset, token);
        } catch (Exception deleteError) {
            report.incrementDeleteFailures();
            LOGGER.error(
                    "[MediaCleanupService#deleteClaimedAsset] 删除媒体物理文件失败，mediaId={}，storageName={}，claimToken={}",
                    asset.getId(),
                    asset.getStorageName(),
                    token,
                    deleteError
            );
            markDeleteFailed(asset, token, now, deleteError);
        }
    }

    private void markDeleted(MediaAsset asset, String token) {
        transactionManager.inTransaction(connection -> {
            mediaDao.markDeleted(connection, asset.getId(), token);
            return true;
        });
    }

    private void markDeleteFailed(
            MediaAsset asset,
            String token,
            Instant now,
            Exception deleteError
    ) {
        Instant retryAt = now.plus(retryDelay(asset.getDeleteAttempts()));
        String safeError = safeError(deleteError);
        try {
            transactionManager.inTransaction(connection -> {
                mediaDao.markDeleteFailed(
                        connection,
                        asset.getId(),
                        token,
                        retryAt,
                        safeError
                );
                return true;
            });
        } catch (RuntimeException markError) {
            // 删除已经失败且状态更新也失败时，两份异常都保留，后续由 stale claim 恢复。
            markError.addSuppressed(deleteError);
            LOGGER.error(
                    "[MediaCleanupService#markDeleteFailed] 写入删除失败状态也失败，mediaId={}，retryAt={}",
                    asset.getId(),
                    retryAt,
                    markError
            );
        }
    }

    private Duration retryDelay(int deleteAttempts) {
        int exponent = Math.max(0, Math.min(deleteAttempts, 5));
        long hours = Math.min(MAX_RETRY_HOURS, 1L << exponent);
        return Duration.ofHours(hours);
    }

    private String safeError(Throwable error) {
        String message = error.getClass().getSimpleName() + ": " +
                (error.getMessage() == null ? "无错误消息" : error.getMessage());
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }

    private long elapsedMillis(long startedNanos) {
        return Math.max(0L, (System.nanoTime() - startedNanos) / 1_000_000L);
    }

    private static final class PreparationResult {
        private static final PreparationResult EMPTY = new PreparationResult(0, 0);
        private final int staleClaimsRecovered;
        private final int assetsMarkedPending;

        private PreparationResult(int staleClaimsRecovered, int assetsMarkedPending) {
            this.staleClaimsRecovered = staleClaimsRecovered;
            this.assetsMarkedPending = assetsMarkedPending;
        }
    }
}
