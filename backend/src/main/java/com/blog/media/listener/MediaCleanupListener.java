package com.blog.media.listener;

import com.blog.media.service.CleanupReport;
import com.blog.media.service.MediaCleanupConfig;
import com.blog.media.service.MediaCleanupService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/** Web 应用级媒体清理调度器；使用 daemon 线程避免阻塞容器退出。 */
@WebListener
public class MediaCleanupListener implements ServletContextListener {

    private static final Logger LOGGER = LogManager.getLogger(MediaCleanupListener.class);
    private static final long SHUTDOWN_WAIT_SECONDS = 5L;
    private final MediaCleanupService injectedService;
    private final MediaCleanupConfig injectedConfig;
    private final SchedulerFactory schedulerFactory;
    private ScheduledExecutorService executor;
    private ScheduledFuture<?> scheduledTask;

    public MediaCleanupListener() {
        this(null, null, MediaCleanupListener::newDaemonScheduler);
    }

    MediaCleanupListener(MediaCleanupService service, MediaCleanupConfig config) {
        this(service, config, MediaCleanupListener::newDaemonScheduler);
    }

    MediaCleanupListener(
            MediaCleanupService service,
            MediaCleanupConfig config,
            SchedulerFactory schedulerFactory
    ) {
        this.injectedService = service;
        this.injectedConfig = config;
        this.schedulerFactory = Objects.requireNonNull(schedulerFactory, "schedulerFactory 不能为空");
    }

    @Override
    public void contextInitialized(ServletContextEvent event) {
        try {
            MediaCleanupConfig config = injectedConfig == null
                    ? MediaCleanupConfig.fromSystemProperties()
                    : injectedConfig;
            if (!config.isEnabled()) {
                LOGGER.info("[MediaCleanupListener#contextInitialized] 媒体清理器已禁用");
                return;
            }
            MediaCleanupService service = injectedService == null
                    ? new MediaCleanupService(config)
                    : injectedService;
            executor = schedulerFactory.create();
            scheduledTask = executor.scheduleWithFixedDelay(
                    () -> runSafely(service),
                    1,
                    config.getIntervalMinutes(),
                    TimeUnit.MINUTES
            );
            LOGGER.info(
                    "[MediaCleanupListener#contextInitialized] 媒体清理器已启动，intervalMinutes={}，batchSize={}",
                    config.getIntervalMinutes(),
                    config.getBatchSize()
            );
        } catch (RuntimeException error) {
            LOGGER.error("[MediaCleanupListener#contextInitialized] 媒体清理器配置非法，拒绝启动", error);
            shutdown();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        shutdown();
    }

    boolean isRunning() {
        return executor != null && !executor.isShutdown();
    }

    private void runSafely(MediaCleanupService service) {
        try {
            CleanupReport report = service.runOnce();
            if (report == null) {
                LOGGER.warn("[MediaCleanupListener#runSafely] 清理服务未返回轮次报告");
                return;
            }
            LOGGER.info(
                    "[MediaCleanupListener#runSafely] 媒体清理轮次完成，staleClaimsRecovered={}，"
                            + "assetsMarkedPending={}，assetsClaimed={}，filesDeleted={}，"
                            + "filesAlreadyMissing={}，deleteFailures={}，elapsedMillis={}",
                    report.getStaleClaimsRecovered(),
                    report.getAssetsMarkedPending(),
                    report.getAssetsClaimed(),
                    report.getFilesDeleted(),
                    report.getFilesAlreadyMissing(),
                    report.getDeleteFailures(),
                    report.getElapsedMillis()
            );
        } catch (RuntimeException error) {
            // 调度线程捕获每轮异常，避免一次数据库或磁盘故障导致后续轮次永久停止。
            LOGGER.error("[MediaCleanupListener#runSafely] 媒体清理轮次异常，后续轮次继续", error);
        }
    }

    private static ScheduledExecutorService newDaemonScheduler() {
        return Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "blog-media-cleanup");
            thread.setDaemon(true);
            return thread;
        });
    }

    private void shutdown() {
        ScheduledFuture<?> task = scheduledTask;
        scheduledTask = null;
        if (task != null) {
            task.cancel(false);
        }
        ScheduledExecutorService currentExecutor = executor;
        if (currentExecutor == null) return;
        executor = null;
        currentExecutor.shutdownNow();
        try {
            if (!currentExecutor.awaitTermination(SHUTDOWN_WAIT_SECONDS, TimeUnit.SECONDS)) {
                LOGGER.warn(
                        "[MediaCleanupListener#shutdown] 媒体清理线程在有限等待时间内未退出，继续交给容器回收，waitSeconds={}",
                        SHUTDOWN_WAIT_SECONDS
                );
            }
        } catch (InterruptedException error) {
            // 关闭线程被中断时恢复中断标记，避免吞掉容器停止信号。
            LOGGER.warn("[MediaCleanupListener#shutdown] 等待媒体清理线程退出时被中断", error);
            Thread.currentThread().interrupt();
        }
    }

    @FunctionalInterface
    interface SchedulerFactory {
        ScheduledExecutorService create();
    }
}
