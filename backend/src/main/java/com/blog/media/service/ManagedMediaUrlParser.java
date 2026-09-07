package com.blog.media.service;

import com.blog.media.model.ManagedMediaKey;
import com.blog.media.model.MediaType;

import java.util.Optional;
import java.util.regex.Pattern;

/** 只解析站内管理 URL，避免把外链或用户输入直接当成可删除文件。 */
public class ManagedMediaUrlParser {

    private static final Pattern SAFE_SEGMENT = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]*");

    public static ManagedMediaKey parse(String url) {
        if (url == null || url.isEmpty() || !url.startsWith("/") || url.startsWith("//")) {
            throw new IllegalArgumentException("媒体 URL 必须是站内绝对路径");
        }
        if (url.indexOf('?') >= 0 || url.indexOf('#') >= 0 || url.indexOf('%') >= 0) {
            throw new IllegalArgumentException("媒体 URL 不允许查询、片段或编码路径");
        }

        String[] segments = url.substring(1).split("/", -1);
        for (String segment : segments) {
            if (!SAFE_SEGMENT.matcher(segment).matches()) {
                throw new IllegalArgumentException("媒体 URL 包含不安全路径段");
            }
        }

        for (int offset = 0; offset < segments.length; offset++) {
            if ("uploads".equals(segments[offset]) && segments.length == offset + 3) {
                return parsePublicKey(segments, offset);
            }
            if ("api".equals(segments[offset])
                    && segments.length == offset + 4
                    && "files".equals(segments[offset + 1])
                    && "download".equals(segments[offset + 3])) {
                return keyWithoutPhysicalPrefix(MediaType.ATTACHMENT, segments[offset + 2]);
            }
        }
        throw new IllegalArgumentException("媒体 URL 路径不受支持");
    }

    /**
     * 判断 URL 是否看起来像本系统的媒体路径，但因格式错误无法建立稳定媒体键。
     * 外链和普通锚点不计入人工复核，避免把正常内容误报为遗留媒体问题。
     */
    public static boolean isPotentialManagedUrl(String url) {
        if (url == null || url.isEmpty() || !url.startsWith("/") || url.startsWith("//")) {
            return false;
        }
        String[] segments = url.substring(1).split("/", -1);
        for (int index = 0; index < segments.length; index++) {
            if ("uploads".equals(segments[index])) return true;
            if ("api".equals(segments[index])
                    && index + 1 < segments.length
                    && "files".equals(segments[index + 1])) {
                return true;
            }
        }
        return false;
    }

    private static ManagedMediaKey parsePublicKey(String[] segments, int offset) {
        MediaType type;
        if ("avatars".equals(segments[offset + 1])) {
            type = MediaType.AVATAR;
        } else if ("images".equals(segments[offset + 1])) {
            type = MediaType.ARTICLE_IMAGE;
        } else {
            throw new IllegalArgumentException("公共媒体类型不受支持");
        }
        return keyWithoutPhysicalPrefix(type, segments[offset + 2]);
    }

    public static Optional<ManagedMediaKey> tryParse(String url) {
        try {
            return Optional.of(parse(url));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static ManagedMediaKey keyWithoutPhysicalPrefix(MediaType type, String urlFileName) {
        if (urlFileName.startsWith("avatar_")
                || urlFileName.startsWith("image_")
                || urlFileName.startsWith("file_")) {
            throw new IllegalArgumentException("媒体 URL 不应包含物理存储前缀");
        }
        return new ManagedMediaKey(type, urlFileName);
    }
}
