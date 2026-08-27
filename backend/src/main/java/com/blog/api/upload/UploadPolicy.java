package com.blog.api.upload;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

public final class UploadPolicy {

    public static final long IMAGE_MAX_BYTES = 5L * 1024 * 1024;
    public static final long FILE_MAX_BYTES = 10L * 1024 * 1024;
    private static final Map<String, String> IMAGE_EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/gif", ".gif",
            "image/webp", ".webp"
    );

    private UploadPolicy() {
    }

    public static void validateImage(String contentType, long size) {
        if (contentType == null || !IMAGE_EXTENSIONS.containsKey(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("仅支持 JPG、PNG、GIF 或 WebP 图片");
        }
        if (size <= 0) throw new IllegalArgumentException("请选择要上传的图片");
        if (size > IMAGE_MAX_BYTES) throw new IllegalArgumentException("图片大小不能超过 5MB");
    }

    public static void validateAttachment(long size) {
        if (size <= 0) throw new IllegalArgumentException("请选择要上传的附件");
        if (size > FILE_MAX_BYTES) throw new IllegalArgumentException("附件大小不能超过 10MB");
    }

    public static String imageExtension(String contentType) {
        return IMAGE_EXTENSIONS.get(contentType.toLowerCase(Locale.ROOT));
    }

    public static String imageContentType(String fileName) {
        if (fileName == null) return null;
        String normalized = fileName.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> entry : IMAGE_EXTENSIONS.entrySet()) {
            if (normalized.endsWith(entry.getValue())) return entry.getKey();
        }
        return null;
    }

    public static String safeOriginalName(String submittedName) {
        if (submittedName == null || submittedName.isBlank()) return "attachment";
        // 浏览器可能提交 Windows 或 Unix 路径，先统一分隔符再仅保留文件名。
        String normalized = submittedName.replace('\\', '/');
        String name = Path.of(normalized).getFileName().toString();
        name = name.replaceAll("[\\r\\n\\\"]", "_");
        return name.isBlank() ? "attachment" : name;
    }
}
