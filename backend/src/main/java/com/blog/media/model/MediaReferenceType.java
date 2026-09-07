package com.blog.media.model;

/** 引用来源用于区分正文、封面和用户头像，支持按业务边界同步引用。 */
public enum MediaReferenceType {
    USER_AVATAR,
    ARTICLE_CONTENT,
    ARTICLE_COVER
}
