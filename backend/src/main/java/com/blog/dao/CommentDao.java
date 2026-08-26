package com.blog.dao;

import com.blog.entity.Comment;
import java.util.List;

/**
 * 评论数据访问接口
 *
 * @author blog-system
 */
public interface CommentDao {

    /**
     * 新增评论
     *
     * @param comment 评论对象
     * @return 是否成功
     */
    boolean insert(Comment comment);

    /**
     * 根据ID查询评论
     *
     * @param id 评论ID
     * @return 评论对象
     */
    Comment findById(Integer id);

    /**
     * 查询文章的评论列表（包含用户信息）
     *
     * @param articleId 文章ID
     * @return 评论列表
     */
    List<Comment> findByArticleId(Integer articleId);

    /**
     * 获取文章的评论数量
     *
     * @param articleId 文章ID
     * @return 评论数量
     */
    int getCountByArticleId(Integer articleId);

    /**
     * 删除评论
     *
     * @param commentId 评论ID
     * @return 是否成功
     */
    boolean delete(Integer commentId);

    /**
     * 删除文章的所有评论
     *
     * @param articleId 文章ID
     * @return 是否成功
     */
    boolean deleteByArticleId(Integer articleId);

    /**
     * 查询用户的所有评论
     *
     * @param userId 用户ID
     * @return 评论列表
     */
    List<Comment> findByUserId(Integer userId);
}
