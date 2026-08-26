package com.blog.api.dto;

import java.util.Date;

public final class ArticleDto {
    public Integer id;
    public String title;
    public String content;
    public String summary;
    public String coverImage;
    public Integer userId;
    public Integer categoryId;
    public Integer viewCount;
    public Integer status;
    public Date createTime;
    public Date updateTime;
    public String authorName;
    public String authorNickname;
    public String authorAvatar;
    public String categoryName;
    public Integer commentCount;
}
