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

    public static String buildContentDisposition(String fileName) {
        String fallbackFileName = fileName
                .replace("\\", "_")
                .replace("/", "_")
                .replace("\"", "'");
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replace("+", "%20");

        return "attachment; filename=\"" + fallbackFileName + "\"; filename*=UTF-8''" + encodedFileName;
    }
}
