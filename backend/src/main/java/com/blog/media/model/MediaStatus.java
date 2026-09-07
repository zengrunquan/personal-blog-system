package com.blog.media.model;

/** 媒体资产状态机；保护状态明确排除在自动物理删除之外。 */
public enum MediaStatus {
    TEMP,
    ACTIVE,
    DELETE_PENDING,
    DELETING,
    DELETE_FAILED,
    DELETED,
    LEGACY_PROTECTED,
    MISSING_BINARY
}
