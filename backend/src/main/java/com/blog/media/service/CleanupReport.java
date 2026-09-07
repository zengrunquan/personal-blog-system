package com.blog.media.service;

import java.util.LinkedHashMap;
import java.util.Map;

/** 一轮媒体清理的可审计结果；只包含数量和耗时，不暴露文件内容或敏感配置。 */
public final class CleanupReport {

    private final int staleClaimsRecovered;
    private final int assetsMarkedPending;
    private final int assetsClaimed;
    private final int filesDeleted;
    private final int filesAlreadyMissing;
    private final int deleteFailures;
    private final long elapsedMillis;

    public CleanupReport(
            int staleClaimsRecovered,
            int assetsMarkedPending,
            int assetsClaimed,
            int filesDeleted,
            int filesAlreadyMissing,
            int deleteFailures,
            long elapsedMillis
    ) {
        this.staleClaimsRecovered = nonNegative("staleClaimsRecovered", staleClaimsRecovered);
        this.assetsMarkedPending = nonNegative("assetsMarkedPending", assetsMarkedPending);
        this.assetsClaimed = nonNegative("assetsClaimed", assetsClaimed);
        this.filesDeleted = nonNegative("filesDeleted", filesDeleted);
        this.filesAlreadyMissing = nonNegative("filesAlreadyMissing", filesAlreadyMissing);
        this.deleteFailures = nonNegative("deleteFailures", deleteFailures);
        if (elapsedMillis < 0) {
            throw new IllegalArgumentException("elapsedMillis 不能为负数，实际值=" + elapsedMillis);
        }
        this.elapsedMillis = elapsedMillis;
    }

    public int getStaleClaimsRecovered() {
        return staleClaimsRecovered;
    }

    public int getAssetsMarkedPending() {
        return assetsMarkedPending;
    }

    public int getAssetsClaimed() {
        return assetsClaimed;
    }

    public int getFilesDeleted() {
        return filesDeleted;
    }

    public int getFilesAlreadyMissing() {
        return filesAlreadyMissing;
    }

    public int getDeleteFailures() {
        return deleteFailures;
    }

    public long getElapsedMillis() {
        return elapsedMillis;
    }

    public Map<String, Object> asMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("staleClaimsRecovered", staleClaimsRecovered);
        values.put("assetsMarkedPending", assetsMarkedPending);
        values.put("assetsClaimed", assetsClaimed);
        values.put("filesDeleted", filesDeleted);
        values.put("filesAlreadyMissing", filesAlreadyMissing);
        values.put("deleteFailures", deleteFailures);
        values.put("elapsedMillis", elapsedMillis);
        return values;
    }

    @Override
    public String toString() {
        return asMap().toString();
    }

    static Builder builder() {
        return new Builder();
    }

    private static int nonNegative(String name, int value) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " 不能为负数，实际值=" + value);
        }
        return value;
    }

    static final class Builder {
        private int staleClaimsRecovered;
        private int assetsMarkedPending;
        private int assetsClaimed;
        private int filesDeleted;
        private int filesAlreadyMissing;
        private int deleteFailures;
        private long elapsedMillis;

        private Builder() {
        }

        Builder addStaleClaimsRecovered(int count) {
            staleClaimsRecovered += count;
            return this;
        }

        Builder addAssetsMarkedPending(int count) {
            assetsMarkedPending += count;
            return this;
        }

        Builder incrementAssetsClaimed() {
            assetsClaimed++;
            return this;
        }

        Builder incrementFilesDeleted() {
            filesDeleted++;
            return this;
        }

        Builder incrementFilesAlreadyMissing() {
            filesAlreadyMissing++;
            return this;
        }

        Builder incrementDeleteFailures() {
            deleteFailures++;
            return this;
        }

        Builder elapsedMillis(long value) {
            elapsedMillis = Math.max(0L, value);
            return this;
        }

        CleanupReport build() {
            return new CleanupReport(
                    staleClaimsRecovered,
                    assetsMarkedPending,
                    assetsClaimed,
                    filesDeleted,
                    filesAlreadyMissing,
                    deleteFailures,
                    elapsedMillis
            );
        }
    }
}
