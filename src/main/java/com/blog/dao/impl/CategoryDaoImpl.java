package com.blog.dao.impl;

import com.blog.dao.CategoryDao;
import com.blog.entity.Category;
import com.blog.util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 分类数据访问实现类
 *
 * @author blog-system
 */
public class CategoryDaoImpl implements CategoryDao {

    @Override
    public boolean insert(Category category) {
        String sql = "INSERT INTO category (name, description, sort_order) VALUES (?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, category.getName());
            ps.setString(2, category.getDescription());
            ps.setInt(3, category.getSortOrder() != null ? category.getSortOrder() : 0);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Category findById(Integer id) {
        String sql = "SELECT * FROM category WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToCategory(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Category findByName(String name) {
        String sql = "SELECT * FROM category WHERE name = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToCategory(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean existsByName(String name) {
        String sql = "SELECT COUNT(*) FROM category WHERE name = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public List<Category> findAll() {
        String sql = "SELECT * FROM category ORDER BY sort_order ASC, create_time DESC";
        List<Category> categories = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                categories.add(mapRowToCategory(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return categories;
    }

    @Override
    public List<Category> findByPage(int offset, int limit) {
        String sql = "SELECT * FROM category ORDER BY sort_order ASC, create_time DESC LIMIT ?, ?";
        List<Category> categories = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, offset);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    categories.add(mapRowToCategory(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return categories;
    }

    @Override
    public int getTotalCount() {
        String sql = "SELECT COUNT(*) FROM category";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public boolean update(Category category) {
        String sql = "UPDATE category SET name = ?, description = ?, sort_order = ? WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, category.getName());
            ps.setString(2, category.getDescription());
            ps.setInt(3, category.getSortOrder() != null ? category.getSortOrder() : 0);
            ps.setInt(4, category.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean delete(Integer categoryId) {
        Connection conn = null;
        PreparedStatement psComment = null;
        PreparedStatement psArticle = null;
        PreparedStatement psCategory = null;
        try {
            conn = DBUtil.getConnection();
            // 开启事务
            conn.setAutoCommit(false);

            // 先删除该分类下所有文章的评论（级联删除，避免外键约束或孤立数据）
            String sqlDeleteComments = "DELETE FROM comment WHERE article_id IN (SELECT id FROM article WHERE category_id = ?)";
            psComment = conn.prepareStatement(sqlDeleteComments);
            psComment.setInt(1, categoryId);
            psComment.executeUpdate();

            // 再删除该分类下的所有文章
            String sqlDeleteArticles = "DELETE FROM article WHERE category_id = ?";
            psArticle = conn.prepareStatement(sqlDeleteArticles);
            psArticle.setInt(1, categoryId);
            psArticle.executeUpdate();

            // 最后删除分类
            String sqlDeleteCategory = "DELETE FROM category WHERE id = ?";
            psCategory = conn.prepareStatement(sqlDeleteCategory);
            psCategory.setInt(1, categoryId);
            int result = psCategory.executeUpdate();

            // 提交事务
            conn.commit();
            return result > 0;
        } catch (SQLException e) {
            // 回滚事务
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
            return false;
        } finally {
            // 恢复自动提交（放在独立的try-catch中，避免影响资源关闭）
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            // 关闭资源（每个close都在独立的try-catch中，确保全部执行）
            if (psComment != null) {
                try {
                    psComment.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (psArticle != null) {
                try {
                    psArticle.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (psCategory != null) {
                try {
                    psCategory.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public List<Category> findAllWithArticleCount() {
        String sql = "SELECT c.*, COUNT(a.id) AS article_count FROM category c " +
                     "LEFT JOIN article a ON c.id = a.category_id " +
                     "GROUP BY c.id ORDER BY c.sort_order ASC";
        List<Category> categories = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Category category = mapRowToCategory(rs);
                category.setArticleCount(rs.getInt("article_count"));
                categories.add(category);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return categories;
    }

    /**
     * 将ResultSet映射为Category对象
     */
    private Category mapRowToCategory(ResultSet rs) throws SQLException {
        Category category = new Category();
        category.setId(rs.getInt("id"));
        category.setName(rs.getString("name"));
        category.setDescription(rs.getString("description"));
        category.setSortOrder(rs.getInt("sort_order"));
        category.setCreateTime(rs.getTimestamp("create_time"));
        return category;
    }
}
