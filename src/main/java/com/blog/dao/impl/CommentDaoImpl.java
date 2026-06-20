package com.blog.dao.impl;

import com.blog.dao.CommentDao;
import com.blog.entity.Comment;
import com.blog.util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 评论数据访问实现类
 * 包含多表关联查询（评论+用户）
 *
 * @author blog-system
 */
public class CommentDaoImpl implements CommentDao {

    @Override
    public boolean insert(Comment comment) {
        // 参数校验：防止NPE
        if (comment == null) {
            return false;
        }
        String sql = "INSERT INTO comment (content, user_id, article_id) VALUES (?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, comment.getContent());
            ps.setInt(2, comment.getUserId());
            ps.setInt(3, comment.getArticleId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Comment findById(Integer id) {
        String sql = "SELECT cm.*, u.username, u.nickname AS user_nickname, u.avatar AS user_avatar " +
                     "FROM comment cm " +
                     "LEFT JOIN user u ON cm.user_id = u.id " +
                     "WHERE cm.id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToComment(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Comment> findByArticleId(Integer articleId) {
        String sql = "SELECT cm.*, u.username, u.nickname AS user_nickname, u.avatar AS user_avatar " +
                     "FROM comment cm " +
                     "LEFT JOIN user u ON cm.user_id = u.id " +
                     "WHERE cm.article_id = ? " +
                     "ORDER BY cm.create_time ASC";
        List<Comment> comments = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, articleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    comments.add(mapRowToComment(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return comments;
    }

    @Override
    public int getCountByArticleId(Integer articleId) {
        String sql = "SELECT COUNT(*) FROM comment WHERE article_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, articleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public boolean delete(Integer commentId) {
        String sql = "DELETE FROM comment WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, commentId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean deleteByArticleId(Integer articleId) {
        String sql = "DELETE FROM comment WHERE article_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, articleId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<Comment> findByUserId(Integer userId) {
        String sql = "SELECT cm.*, u.username, u.nickname AS user_nickname, u.avatar AS user_avatar " +
                     "FROM comment cm " +
                     "LEFT JOIN user u ON cm.user_id = u.id " +
                     "WHERE cm.user_id = ? " +
                     "ORDER BY cm.create_time DESC";
        List<Comment> comments = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    comments.add(mapRowToComment(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return comments;
    }

    /**
     * 将ResultSet映射为Comment对象（包含关联字段）
     */
    private Comment mapRowToComment(ResultSet rs) throws SQLException {
        Comment comment = new Comment();
        comment.setId(rs.getInt("id"));
        comment.setContent(rs.getString("content"));
        comment.setUserId(rs.getInt("user_id"));
        comment.setArticleId(rs.getInt("article_id"));
        comment.setCreateTime(rs.getTimestamp("create_time"));

        // 关联字段
        comment.setUsername(rs.getString("username"));
        comment.setUserNickname(rs.getString("user_nickname"));
        comment.setUserAvatar(rs.getString("user_avatar"));

        return comment;
    }
}
