package com.blog.media.model;

/** 媒体类型决定 URL 语义和物理存储目录，避免调用方自行拼接路径。 */
public enum MediaType {
    AVATAR("avatars", "avatar_", "image"),
    ARTICLE_IMAGE("images", "image_", "image"),
    ATTACHMENT("files", "file_", "file");

    private final String urlSegment;
    private final String storagePrefix;
    private final String directoryName;

    MediaType(String urlSegment, String storagePrefix, String directoryName) {
        this.urlSegment = urlSegment;
        this.storagePrefix = storagePrefix;
        this.directoryName = directoryName;
    }

    public String getUrlSegment() {
        return urlSegment;
    }

    public String getStoragePrefix() {
        return storagePrefix;
    }

    public String getDirectoryName() {
        return directoryName;
    }

    public String buildUrl(String contextPath, String urlFileName) {
        String context = contextPath == null ? "" : contextPath.trim();
        if (context.endsWith("/")) context = context.substring(0, context.length() - 1);
        if (ATTACHMENT == this) {
            return context + "/api/files/" + urlFileName + "/download";
        }
        return context + "/uploads/" + urlSegment + "/" + urlFileName;
    }
}
