package com.blog.media.model;

import java.nio.file.Path;
import java.util.Objects;

/** 文件落盘后的不可变结果，供元数据事务使用。 */
public final class StoredMedia {

    private final MediaType mediaType;
    private final String storageName;
    private final String urlFileName;
    private final String originalName;
    private final String contentType;
    private final long sizeBytes;
    private final String sha256;
    private final Path path;

    public StoredMedia(
            MediaType mediaType,
            String storageName,
            String urlFileName,
            String originalName,
            String contentType,
            long sizeBytes,
            String sha256,
            Path path
    ) {
        this.mediaType = Objects.requireNonNull(mediaType, "mediaType 不能为空");
        this.storageName = Objects.requireNonNull(storageName, "storageName 不能为空");
        this.urlFileName = Objects.requireNonNull(urlFileName, "urlFileName 不能为空");
        this.originalName = Objects.requireNonNull(originalName, "originalName 不能为空");
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.sha256 = sha256;
        this.path = Objects.requireNonNull(path, "path 不能为空");
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public String getStorageName() {
        return storageName;
    }

    public String getUrlFileName() {
        return urlFileName;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getSha256() {
        return sha256;
    }

    public Path getPath() {
        return path;
    }
}
