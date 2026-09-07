package com.blog.media.maintenance;

/** 回填所需的最小文章媒体字段，避免维护查询加载页面无关数据。 */
public final class ArticleMediaSource {

    private final int id;
    private final String content;
    private final String coverImage;

    public ArticleMediaSource(int id, String content, String coverImage) {
        this.id = id;
        this.content = content;
        this.coverImage = coverImage;
    }

    public int getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public String getCoverImage() {
        return coverImage;
    }
}
