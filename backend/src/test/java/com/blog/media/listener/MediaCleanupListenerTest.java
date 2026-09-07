package com.blog.media.listener;

import com.blog.media.service.MediaCleanupConfig;
import com.blog.media.service.MediaCleanupService;
import org.junit.Test;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

public class MediaCleanupListenerTest {

    @Test
    public void listenerShouldStopSchedulerOnContextDestroy() {
        MediaCleanupService service = mock(MediaCleanupService.class);
        MediaCleanupListener listener = new MediaCleanupListener(
                service,
                new MediaCleanupConfig(true, 24, 1, 1, 1)
        );

        listener.contextInitialized(null);
        assertTrue(listener.isRunning());

        listener.contextDestroyed(null);
        assertFalse(listener.isRunning());
    }

    @Test
    public void disabledConfigShouldNotCreateScheduler() {
        MediaCleanupListener listener = new MediaCleanupListener(
                mock(MediaCleanupService.class),
                new MediaCleanupConfig(false, 24, 1, 1, 1)
        );

        listener.contextInitialized(null);

        assertFalse(listener.isRunning());
    }

    @Test
    public void schedulerShouldUseFixedDelayAndAwaitFiniteShutdown() throws Exception {
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        ScheduledFuture<?> task = mock(ScheduledFuture.class);
        doReturn(task).when(executor).scheduleWithFixedDelay(
                any(Runnable.class),
                eq(1L),
                eq(7L),
                eq(TimeUnit.MINUTES)
        );

        MediaCleanupListener listener = new MediaCleanupListener(
                mock(MediaCleanupService.class),
                new MediaCleanupConfig(true, 24, 7, 1, 1),
                () -> executor
        );

        listener.contextInitialized(null);
        verify(executor).scheduleWithFixedDelay(
                any(Runnable.class),
                eq(1L),
                eq(7L),
                eq(TimeUnit.MINUTES)
        );

        listener.contextDestroyed(null);
        org.mockito.InOrder shutdownOrder = inOrder(task, executor);
        shutdownOrder.verify(task).cancel(false);
        shutdownOrder.verify(executor).shutdownNow();
        shutdownOrder.verify(executor).awaitTermination(eq(5L), eq(TimeUnit.SECONDS));
    }

    @Test
    public void interruptedShutdownShouldRestoreInterruptFlag() throws Exception {
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        ScheduledFuture<?> task = mock(ScheduledFuture.class);
        doReturn(task).when(executor).scheduleWithFixedDelay(
                any(Runnable.class),
                eq(1L),
                eq(1L),
                eq(TimeUnit.MINUTES)
        );
        doThrow(new InterruptedException("container stop interrupted"))
                .when(executor).awaitTermination(eq(5L), eq(TimeUnit.SECONDS));

        MediaCleanupListener listener = new MediaCleanupListener(
                mock(MediaCleanupService.class),
                new MediaCleanupConfig(true, 24, 1, 1, 1),
                () -> executor
        );

        try {
            listener.contextInitialized(null);
            listener.contextDestroyed(null);
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }
}
