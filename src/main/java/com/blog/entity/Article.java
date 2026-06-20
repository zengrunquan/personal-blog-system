package com.blog.entity;

import java.io.Serializable;
import java.util.Date;

/**
 * 文章实体类
 *
 * @author blog-system
 */
public class Article implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 文章ID */
    private Integer id;

    /** 文章标题 */
    private String title;

    /** 文章内容 */
    private String content;

    /** 文章摘要 */
    private String summary;

    /** 封面图片路径 */
    private String coverImage;

    /** 作者ID */
    private Integer userId;

    /** 分类ID */
    private Integer categoryId;

    /** 浏览次数 */
    private Integer viewCount;

    /** 状态：0-草稿，1-已发布 */
    private Integer status;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;

    // ========== 关联查询时使用的字段 ==========

    /** 作者用户名 */
    private String authorName;

    /** 作者昵称 */
    private String authorNickname;

    /** 作者头像 */
    private String authorAvatar;

    /** 分类名称 */
    private String categoryName;

    /** 评论数量 */
    private Integer commentCount;

    // 构造方法

    public Article() {
    }

    // Getter和Setter方法

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(String coverImage) {
        this.coverImage = coverImage;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorNickname() {
        return authorNickname;
    }

    public void setAuthorNickname(String authorNickname) {
        this.authorNickname = authorNickname;
    }

    public String getAuthorAvatar() {
        return authorAvatar;
    }

    public void setAuthorAvatar(String authorAvatar) {
        this.authorAvatar = authorAvatar;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Integer getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(Integer commentCount) {
        this.commentCount = commentCount;
    }

    @Override
    public String toString() {
        return "Article{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", userId=" + userId +
                ", categoryId=" + categoryId +
                ", viewCount=" + viewCount +
                ", status=" + status +
                '}';
    }
}
