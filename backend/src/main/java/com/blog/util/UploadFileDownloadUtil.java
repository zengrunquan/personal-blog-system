package com.blog.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * 上传附件下载工具类
 */
public final class UploadFileDownloadUtil {

    private UploadFileDownloadUtil() {
    }

    public static Path resolveDownloadPath(Path uploadDir, String fileName) {
        if (uploadDir == null) {
            throw new IllegalArgumentException("非法下载文件名：上传目录不能为空");
        }

        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("非法下载文件名：文件名不能为空");
        }

        String safeFileName = fileName.trim();
        if (safeFileName.contains("..")
                || safeFileName.contains("/")
                || safeFileName.contains("\\")
                || safeFileName.contains(":")) {
            throw new IllegalArgumentException("非法下载文件名：" + fileName);
        }

        Path normalizedUploadDir = uploadDir.toAbsolutePath().normalize();
        Path downloadPath = normalizedUploadDir.resolve(safeFileName).normalize();
        if (!downloadPath.startsWith(normalizedUploadDir)) {
            throw new IllegalArgumentException("非法下载文件名：" + fileName);
        }

        return downloadPath;
    }

    public static String sanitizeDownloadName(String originalName, String fallbackName) {
        if (originalName == null || originalName.isBlank()) return fallbackName;

        String name = originalName.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        name = name.replaceAll("[\\p{Cntrl}\"<>:|?*]", "_").strip();
        name = name.replaceAll("[. ]+$", "");

        return name.isBlank() || ".".equals(name) || "..".equals(name)
                ? fallbackName
                : name;
    }

    public static String buildContentDisposition(String fileName) {
        String safeName = sanitizeDownloadName(fileName, "attachment");

        // ASCII 备用名保证容器可输出响应头，完整原名由 filename* 承载。
        String asciiName = safeName.replaceAll("[^A-Za-z0-9._ -]", "_");
        String encodedName = URLEncoder.encode(safeName, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("*", "%2A");

        return "attachment; filename=\"" + asciiName
                + "\"; filename*=UTF-8''" + encodedName;
    }
}
