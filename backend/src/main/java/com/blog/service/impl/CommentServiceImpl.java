package com.blog.service.impl;

import com.blog.dao.CommentDao;
import com.blog.dao.impl.CommentDaoImpl;
import com.blog.entity.Comment;
import com.blog.service.CommentService;

import java.util.List;
import java.util.Objects;

/**
 * 评论服务实现类
 *
 * @author blog-system
 */
public class CommentServiceImpl implements CommentService {

    private final CommentDao commentDao;

    public CommentServiceImpl() {
        this(new CommentDaoImpl());
    }

    public CommentServiceImpl(CommentDao commentDao) {
        this.commentDao = Objects.requireNonNull(commentDao, "commentDao 不能为空");
    }

    @Override
    public String addComment(String content, Integer userId, Integer articleId) {
        // 参数验证
        if (content == null || content.trim().isEmpty()) {
            return "评论内容不能为空";
        }
        if (userId == null) {
            return "用户未登录";
        }
        if (articleId == null) {
            return "文章不存在";
        }

        // 内容长度验证
        if (content.length() > 1000) {
            return "评论内容不能超过1000个字符";
        }

        // 创建评论对象
        Comment comment = new Comment(content.trim(), userId, articleId);

        // 插入数据库
        boolean success = commentDao.insert(comment);
        return success ? null : "发表评论失败";
    }

    @Override
    public List<Comment> findByArticleId(Integer articleId) {
        return commentDao.findByArticleId(articleId);
    }

    @Override
    public boolean delete(Integer commentId, Integer userId, boolean isAdmin) {
        // 查询评论
        Comment comment = commentDao.findById(commentId);
        if (comment == null) {
            return false;
        }

        // 验证权限：只有评论作者或管理员可以删除
        if (!isAdmin && !comment.getUserId().equals(userId)) {
            return false;
        }

        return commentDao.delete(commentId);
    }
}
