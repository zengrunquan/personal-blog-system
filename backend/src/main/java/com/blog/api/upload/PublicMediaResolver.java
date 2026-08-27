package com.blog.api.upload;

import com.blog.util.UploadFileDownloadUtil;

import java.nio.file.Path;

public final class PublicMediaResolver {

    private PublicMediaResolver() {
    }

    public static Path resolve(Path storageDirectory, String pathInfo) {
        if (pathInfo == null) throw new IllegalArgumentException("公共图片路径不能为空");
        String[] segments = java.util.Arrays.stream(pathInfo.split("/"))
                .filter(segment -> !segment.isBlank())
                .toArray(String[]::new);
        if (segments.length != 2) throw new IllegalArgumentException("公共图片路径无效");

        String requiredPrefix;
        if ("avatars".equals(segments[0])) {
            requiredPrefix = "avatar_";
        } else if ("images".equals(segments[0])) {
            requiredPrefix = "image_";
        } else {
            throw new IllegalArgumentException("公共图片类型无效");
        }

        String urlFileName = segments[1];
        if (urlFileName.startsWith("avatar_")
                || urlFileName.startsWith("image_")
                || urlFileName.startsWith("file_")) {
            throw new IllegalArgumentException("公共图片 URL 不应包含物理存储前缀");
        }
        return UploadFileDownloadUtil.resolveDownloadPath(
                storageDirectory.resolve("image"),
                requiredPrefix + urlFileName
        );
    }
}
