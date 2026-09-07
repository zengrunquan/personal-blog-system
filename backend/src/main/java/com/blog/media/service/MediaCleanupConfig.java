package com.blog.media.service;

import java.time.Duration;

/** 清理器配置；配置错误必须显式失败，避免误把危险值降级为默认值。 */
public final class MediaCleanupConfig {

    public static final String ENABLED_PROPERTY = "blog.media.cleanup.enabled";
    public static final String ENABLED_ENV = "BLOG_MEDIA_CLEANUP_ENABLED";
    public static final String RETENTION_HOURS_PROPERTY = "blog.media.retention.hours";
    public static final String RETENTION_HOURS_ENV = "BLOG_MEDIA_RETENTION_HOURS";
    public static final String INTERVAL_MINUTES_PROPERTY = "blog.media.cleanup.interval.minutes";
    public static final String INTERVAL_MINUTES_ENV = "BLOG_MEDIA_CLEANUP_INTERVAL_MINUTES";
    public static final String BATCH_SIZE_PROPERTY = "blog.media.cleanup.batch.size";
    public static final String BATCH_SIZE_ENV = "BLOG_MEDIA_CLEANUP_BATCH_SIZE";
    public static final String CLAIM_TIMEOUT_PROPERTY = "blog.media.claim.timeout.minutes";
    public static final String CLAIM_TIMEOUT_ENV = "BLOG_MEDIA_CLAIM_TIMEOUT_MINUTES";

    public static final boolean DEFAULT_ENABLED = true;
    public static final int DEFAULT_RETENTION_HOURS = 24;
    public static final int DEFAULT_INTERVAL_MINUTES = 60;
    public static final int DEFAULT_BATCH_SIZE = 100;
    public static final int DEFAULT_CLAIM_TIMEOUT_MINUTES = 60;

    // 上限用于阻止配置错误把清理任务扩大成超长周期或超大批处理，避免运维事故被静默放大。
    public static final int MIN_RETENTION_HOURS = 1;
    public static final int MAX_RETENTION_HOURS = 8_760;
    public static final int MIN_INTERVAL_MINUTES = 1;
    public static final int MAX_INTERVAL_MINUTES = 1_440;
    public static final int MIN_BATCH_SIZE = 1;
    public static final int MAX_BATCH_SIZE = 10_000;
    public static final int MIN_CLAIM_TIMEOUT_MINUTES = 1;
    public static final int MAX_CLAIM_TIMEOUT_MINUTES = 1_440;

    private final boolean enabled;
    private final int retentionHours;
    private final int intervalMinutes;
    private final int batchSize;
    private final int claimTimeoutMinutes;

    public MediaCleanupConfig(
            boolean enabled,
            int retentionHours,
            int intervalMinutes,
            int batchSize,
            int claimTimeoutMinutes
    ) {
        validateRange(
                RETENTION_HOURS_PROPERTY,
                retentionHours,
                MIN_RETENTION_HOURS,
                MAX_RETENTION_HOURS
        );
        validateRange(
                INTERVAL_MINUTES_PROPERTY,
                intervalMinutes,
                MIN_INTERVAL_MINUTES,
                MAX_INTERVAL_MINUTES
        );
        validateRange(BATCH_SIZE_PROPERTY, batchSize, MIN_BATCH_SIZE, MAX_BATCH_SIZE);
        validateRange(
                CLAIM_TIMEOUT_PROPERTY,
                claimTimeoutMinutes,
                MIN_CLAIM_TIMEOUT_MINUTES,
                MAX_CLAIM_TIMEOUT_MINUTES
        );
        this.enabled = enabled;
        this.retentionHours = retentionHours;
        this.intervalMinutes = intervalMinutes;
        this.batchSize = batchSize;
        this.claimTimeoutMinutes = claimTimeoutMinutes;
    }

    public static MediaCleanupConfig fromSystemProperties() {
        return new MediaCleanupConfig(
                readBoolean(
                        ENABLED_PROPERTY,
                        ENABLED_ENV,
                        DEFAULT_ENABLED
                ),
                readInt(
                        RETENTION_HOURS_PROPERTY,
                        RETENTION_HOURS_ENV,
                        DEFAULT_RETENTION_HOURS,
                        MIN_RETENTION_HOURS,
                        MAX_RETENTION_HOURS
                ),
                readInt(
                        INTERVAL_MINUTES_PROPERTY,
                        INTERVAL_MINUTES_ENV,
                        DEFAULT_INTERVAL_MINUTES,
                        MIN_INTERVAL_MINUTES,
                        MAX_INTERVAL_MINUTES
                ),
                readInt(
                        BATCH_SIZE_PROPERTY,
                        BATCH_SIZE_ENV,
                        DEFAULT_BATCH_SIZE,
                        MIN_BATCH_SIZE,
                        MAX_BATCH_SIZE
                ),
                readInt(
                        CLAIM_TIMEOUT_PROPERTY,
                        CLAIM_TIMEOUT_ENV,
                        DEFAULT_CLAIM_TIMEOUT_MINUTES,
                        MIN_CLAIM_TIMEOUT_MINUTES,
                        MAX_CLAIM_TIMEOUT_MINUTES
                )
        );
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getRetentionHours() {
        return retentionHours;
    }

    public int getIntervalMinutes() {
        return intervalMinutes;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public int getClaimTimeoutMinutes() {
        return claimTimeoutMinutes;
    }

    public Duration retentionDuration() {
        return Duration.ofHours(retentionHours);
    }

    public Duration intervalDuration() {
        return Duration.ofMinutes(intervalMinutes);
    }

    public Duration claimTimeoutDuration() {
        return Duration.ofMinutes(claimTimeoutMinutes);
    }

    private static boolean readBoolean(String property, String environment, boolean defaultValue) {
        String value = configuredValue(property, environment);
        if (value == null) return defaultValue;
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        throw new IllegalArgumentException(
                "清理器配置 " + property + " 必须是 true 或 false，实际值=" + value
        );
    }

    private static int readInt(
            String property,
            String environment,
            int defaultValue,
            int minimum,
            int maximum
    ) {
        String value = configuredValue(property, environment);
        if (value == null) return defaultValue;
        try {
            int parsed = Integer.parseInt(value);
            validateRange(property, parsed, minimum, maximum);
            return parsed;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(
                    "清理器配置 " + property + " 必须是 "
                            + minimum + ".." + maximum + " 的整数，实际值=" + value,
                    error
            );
        }
    }

    private static String configuredValue(String property, String environment) {
        String propertyValue = System.getProperty(property);
        if (propertyValue != null && !propertyValue.trim().isEmpty()) return propertyValue.trim();
        String environmentValue = System.getenv(environment);
        return environmentValue == null || environmentValue.trim().isEmpty()
                ? null
                : environmentValue.trim();
    }

    private static void validateRange(String name, int value, int minimum, int maximum) {
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(
                    "清理器配置 " + name + " 必须在 " + minimum + ".." + maximum
                            + " 范围内，实际值=" + value
            );
        }
    }
}
