package com.blog.media.model;

import java.util.Objects;
import java.util.regex.Pattern;

/** URL 中稳定的媒体键，不包含磁盘目录和物理前缀。 */
public final class ManagedMediaKey {

    private static final Pattern SAFE_URL_FILE_NAME =
            Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]*");

    private final MediaType mediaType;
    private final String urlFileName;

    public ManagedMediaKey(MediaType mediaType, String urlFileName) {
        this.mediaType = Objects.requireNonNull(mediaType, "mediaType 不能为空");
        if (urlFileName == null || !SAFE_URL_FILE_NAME.matcher(urlFileName).matches()) {
            throw new IllegalArgumentException("URL 文件名不安全");
        }
        this.urlFileName = urlFileName;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public String getUrlFileName() {
        return urlFileName;
    }

    public String getStorageName() {
        return mediaType.getStoragePrefix() + urlFileName;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ManagedMediaKey)) return false;
        ManagedMediaKey that = (ManagedMediaKey) other;
        return mediaType == that.mediaType && urlFileName.equals(that.urlFileName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mediaType, urlFileName);
    }

    @Override
    public String toString() {
        return mediaType + ":" + urlFileName;
    }
}
