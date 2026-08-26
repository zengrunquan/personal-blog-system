package com.blog.entity;

import java.io.Serializable;
import java.util.Date;

/**
 * 评论实体类
 *
 * @author blog-system
 */
public class Comment implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 评论ID */
    private Integer id;

    /** 评论内容 */
    private String content;

    /** 评论用户ID */
    private Integer userId;

    /** 文章ID */
    private Integer articleId;

    /** 创建时间 */
    private Date createTime;

    // ========== 关联查询时使用的字段 ==========

    /** 评论用户昵称 */
    private String userNickname;

    /** 评论用户头像 */
    private String userAvatar;

    /** 评论用户名 */
    private String username;

    // 构造方法

    public Comment() {
    }

    public Comment(String content, Integer userId, Integer articleId) {
        this.content = content;
        this.userId = userId;
        this.articleId = articleId;
    }

    // Getter和Setter方法

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getArticleId() {
        return articleId;
    }

    public void setArticleId(Integer articleId) {
        this.articleId = articleId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUserNickname() {
        return userNickname;
    }

    public void setUserNickname(String userNickname) {
        this.userNickname = userNickname;
    }

    public String getUserAvatar() {
        return userAvatar;
    }

    public void setUserAvatar(String userAvatar) {
        this.userAvatar = userAvatar;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String toString() {
        return "Comment{" +
                "id=" + id +
                ", content='" + content + '\'' +
                ", userId=" + userId +
                ", articleId=" + articleId +
                '}';
    }
}
