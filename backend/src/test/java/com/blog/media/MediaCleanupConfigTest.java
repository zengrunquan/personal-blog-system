package com.blog.media;

import com.blog.media.service.MediaCleanupConfig;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class MediaCleanupConfigTest {

    private static final String[] PROPERTIES = {
            MediaCleanupConfig.ENABLED_PROPERTY,
            MediaCleanupConfig.RETENTION_HOURS_PROPERTY,
            MediaCleanupConfig.INTERVAL_MINUTES_PROPERTY,
            MediaCleanupConfig.BATCH_SIZE_PROPERTY,
            MediaCleanupConfig.CLAIM_TIMEOUT_PROPERTY
    };

    @After
    public void clearProperties() {
        for (String property : PROPERTIES) System.clearProperty(property);
    }

    @Test
    public void systemPropertiesShouldOverrideDefaults() {
        System.setProperty(MediaCleanupConfig.ENABLED_PROPERTY, "false");
        System.setProperty(MediaCleanupConfig.RETENTION_HOURS_PROPERTY, "12");
        System.setProperty(MediaCleanupConfig.INTERVAL_MINUTES_PROPERTY, "15");
        System.setProperty(MediaCleanupConfig.BATCH_SIZE_PROPERTY, "9");
        System.setProperty(MediaCleanupConfig.CLAIM_TIMEOUT_PROPERTY, "7");

        MediaCleanupConfig config = MediaCleanupConfig.fromSystemProperties();

        assertFalse(config.isEnabled());
        assertEquals(12, config.getRetentionHours());
        assertEquals(15, config.getIntervalMinutes());
        assertEquals(9, config.getBatchSize());
        assertEquals(7, config.getClaimTimeoutMinutes());
    }

    @Test
    public void invalidValuesShouldFailInsteadOfFallingBack() {
        System.setProperty(MediaCleanupConfig.BATCH_SIZE_PROPERTY, "0");
        try {
            MediaCleanupConfig.fromSystemProperties();
        } catch (IllegalArgumentException error) {
            assertTrue(error.getMessage().contains("batch.size"));
            return;
        }
        throw new AssertionError("非法清理配置必须拒绝启动");
    }

    @Test
    public void defaultsShouldBePositiveAndEnabledWhenEnvironmentIsUnset() {
        for (String property : PROPERTIES) System.clearProperty(property);
        MediaCleanupConfig config = MediaCleanupConfig.fromSystemProperties();

        assertTrue(config.isEnabled());
        assertTrue(config.getRetentionHours() > 0);
        assertTrue(config.getIntervalMinutes() > 0);
        assertTrue(config.getBatchSize() > 0);
        assertTrue(config.getClaimTimeoutMinutes() > 0);
    }

    @Test
    public void documentedMaximumValuesShouldBeAccepted() {
        MediaCleanupConfig config = new MediaCleanupConfig(
                true,
                MediaCleanupConfig.MAX_RETENTION_HOURS,
                MediaCleanupConfig.MAX_INTERVAL_MINUTES,
                MediaCleanupConfig.MAX_BATCH_SIZE,
                MediaCleanupConfig.MAX_CLAIM_TIMEOUT_MINUTES
        );

        assertEquals(MediaCleanupConfig.MAX_RETENTION_HOURS, config.getRetentionHours());
        assertEquals(MediaCleanupConfig.MAX_INTERVAL_MINUTES, config.getIntervalMinutes());
        assertEquals(MediaCleanupConfig.MAX_BATCH_SIZE, config.getBatchSize());
        assertEquals(MediaCleanupConfig.MAX_CLAIM_TIMEOUT_MINUTES, config.getClaimTimeoutMinutes());
    }

    @Test
    public void valuesAboveDocumentedMaximumShouldBeRejected() {
        assertThrows(IllegalArgumentException.class, () -> new MediaCleanupConfig(
                true,
                MediaCleanupConfig.MAX_RETENTION_HOURS + 1,
                60,
                100,
                60
        ));
        assertThrows(IllegalArgumentException.class, () -> new MediaCleanupConfig(
                true,
                24,
                MediaCleanupConfig.MAX_INTERVAL_MINUTES + 1,
                100,
                60
        ));
        assertThrows(IllegalArgumentException.class, () -> new MediaCleanupConfig(
                true,
                24,
                60,
                MediaCleanupConfig.MAX_BATCH_SIZE + 1,
                60
        ));
        assertThrows(IllegalArgumentException.class, () -> new MediaCleanupConfig(
                true,
                24,
                60,
                100,
                MediaCleanupConfig.MAX_CLAIM_TIMEOUT_MINUTES + 1
        ));
    }

    @Test
    public void integerMaximumShouldBeRejectedWithConfigurationRange() {
        System.setProperty(
                MediaCleanupConfig.BATCH_SIZE_PROPERTY,
                String.valueOf(Integer.MAX_VALUE)
        );

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                MediaCleanupConfig::fromSystemProperties
        );

        assertTrue(error.getMessage().contains(MediaCleanupConfig.BATCH_SIZE_PROPERTY));
        assertTrue(error.getMessage().contains("1.." + MediaCleanupConfig.MAX_BATCH_SIZE));
    }
}
