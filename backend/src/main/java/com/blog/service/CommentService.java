package com.blog.service;

import com.blog.entity.Comment;
import java.util.List;

/**
 * 评论服务接口
 *
 * @author blog-system
 */
public interface CommentService {

    /**
     * 发表评论
     *
     * @param content   评论内容
     * @param userId    用户ID
     * @param articleId 文章ID
     * @return 操作结果信息，成功返回null
     */
    String addComment(String content, Integer userId, Integer articleId);

    /**
     * 查询文章的评论列表
     *
     * @param articleId 文章ID
     * @return 评论列表
     */
    List<Comment> findByArticleId(Integer articleId);

    /**
     * 删除评论
     *
     * @param commentId 评论ID
     * @param userId    当前用户ID（验证权限）
     * @param isAdmin   是否为管理员
     * @return 是否成功
     */
    boolean delete(Integer commentId, Integer userId, boolean isAdmin);
}
