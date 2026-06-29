package com.blog.dao.impl;

import com.blog.dao.ArticleDao;
import com.blog.entity.Article;
import com.blog.util.DBUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 文章数据访问实现类
 * 包含多表关联查询（文章+用户+分类）
 *
 * @author blog-system
 */
public class ArticleDaoImpl implements ArticleDao {

    private static final Logger LOGGER = LogManager.getLogger(ArticleDaoImpl.class);

    @Override
    public boolean insert(Article article) {
        String sql = "INSERT INTO article (title, content, summary, cover_image, user_id, category_id, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, article.getTitle());
            ps.setString(2, article.getContent());
            ps.setString(3, article.getSummary());
            ps.setString(4, article.getCoverImage());
            ps.setInt(5, article.getUserId());
            ps.setInt(6, article.getCategoryId());
            ps.setInt(7, article.getStatus() != null ? article.getStatus() : 1);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.error(
                    "[ArticleDaoImpl#insert] 新增文章失败，userId={}，categoryId={}",
                    article.getUserId(),
                    article.getCategoryId(),
                    e
            );
            return false;
        }
    }

    @Override
    public Article findById(Integer id) {
        // 多表关联查询：文章+用户+分类
        String sql = "SELECT a.*, u.username AS author_name, u.nickname AS author_nickname, " +
                     "u.avatar AS author_avatar, c.name AS category_name, " +
                     "(SELECT COUNT(*) FROM comment WHERE article_id = a.id) AS comment_count " +
                     "FROM article a " +
                     "LEFT JOIN user u ON a.user_id = u.id " +
                     "LEFT JOIN category c ON a.category_id = c.id " +
                     "WHERE a.id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToArticle(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.error("[ArticleDaoImpl#findById] 按ID查询文章失败，articleId={}", id, e);
        }
        return null;
    }

    @Override
    public boolean update(Article article) {
        String sql = "UPDATE article SET title = ?, content = ?, summary = ?, cover_image = ?, category_id = ?, status = ? WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, article.getTitle());
            ps.setString(2, article.getContent());
            ps.setString(3, article.getSummary());
            ps.setString(4, article.getCoverImage());
            ps.setInt(5, article.getCategoryId());
            ps.setInt(6, article.getStatus() != null ? article.getStatus() : 1);
            ps.setInt(7, article.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.error(
                    "[ArticleDaoImpl#update] 更新文章失败，articleId={}",
                    article.getId(),
                    e
            );
            return false;
        }
    }

    @Override
    public boolean delete(Integer articleId) {
        Connection conn = null;
        PreparedStatement psComment = null;
        PreparedStatement psArticle = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            // 先删除文章的评论
            String sqlDeleteComments = "DELETE FROM comment WHERE article_id = ?";
            psComment = conn.prepareStatement(sqlDeleteComments);
            psComment.setInt(1, articleId);
            psComment.executeUpdate();

            // 再删除文章
            String sqlDeleteArticle = "DELETE FROM article WHERE id = ?";
            psArticle = conn.prepareStatement(sqlDeleteArticle);
            psArticle.setInt(1, articleId);
            int result = psArticle.executeUpdate();

            conn.commit();
            return result > 0;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    LOGGER.error(
                            "[ArticleDaoImpl#delete] 回滚删除文章事务失败，articleId={}",
                            articleId,
                            ex
                    );
                }
            }
            LOGGER.error(
                    "[ArticleDaoImpl#delete] 删除文章事务失败，articleId={}",
                    articleId,
                    e
            );
            return false;
        } finally {
            // 恢复自动提交（放在独立的try-catch中，避免影响资源关闭）
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    LOGGER.error(
                            "[ArticleDaoImpl#delete] 恢复自动提交失败，articleId={}",
                            articleId,
                            e
                    );
                }
            }
            // 关闭资源（每个close都在独立的try-catch中，确保全部执行）
            if (psComment != null) {
                try {
                    psComment.close();
                } catch (SQLException e) {
                    LOGGER.error(
                            "[ArticleDaoImpl#delete] 关闭评论删除语句失败，articleId={}",
                            articleId,
                            e
                    );
                }
            }
            if (psArticle != null) {
                try {
                    psArticle.close();
                } catch (SQLException e) {
                    LOGGER.error(
                            "[ArticleDaoImpl#delete] 关闭文章删除语句失败，articleId={}",
                            articleId,
                            e
                    );
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    LOGGER.error(
                            "[ArticleDaoImpl#delete] 关闭数据库连接失败，articleId={}",
                            articleId,
                            e
                    );
                }
            }
        }
    }

    @Override
    public List<Article> findPublishedByPage(int offset, int limit) {
        String sql = "SELECT a.*, u.username AS author_name, u.nickname AS author_nickname, " +
                     "u.avatar AS author_avatar, c.name AS category_name, " +
                     "(SELECT COUNT(*) FROM comment WHERE article_id = a.id) AS comment_count " +
                     "FROM article a " +
                     "LEFT JOIN user u ON a.user_id = u.id " +
                     "LEFT JOIN category c ON a.category_id = c.id " +
                     "WHERE a.status = 1 " +
                     "ORDER BY a.create_time DESC LIMIT ?, ?";
        return executeQuery(sql, offset, limit);
    }

    @Override
    public int getPublishedTotalCount() {
        String sql = "SELECT COUNT(*) FROM article WHERE status = 1";
        return getCount(sql);
    }

    @Override
    public List<Article> findByCategory(Integer categoryId, int offset, int limit) {
        String sql = "SELECT a.*, u.username AS author_name, u.nickname AS author_nickname, " +
                     "u.avatar AS author_avatar, c.name AS category_name, " +
                     "(SELECT COUNT(*) FROM comment WHERE article_id = a.id) AS comment_count " +
                     "FROM article a " +
                     "LEFT JOIN user u ON a.user_id = u.id " +
                     "LEFT JOIN category c ON a.category_id = c.id " +
                     "WHERE a.status = 1 AND a.category_id = ? " +
                     "ORDER BY a.create_time DESC LIMIT ?, ?";
        List<Article> articles = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, categoryId);
            ps.setInt(2, offset);
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    articles.add(mapRowToArticle(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.error(
                    "[ArticleDaoImpl#findByCategory] 分页查询分类文章失败，categoryId={}，offset={}，limit={}",
                    categoryId,
                    offset,
                    limit,
                    e
            );
        }
        return articles;
    }

    @Override
    public int getCountByCategory(Integer categoryId) {
        String sql = "SELECT COUNT(*) FROM article WHERE status = 1 AND category_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            LOGGER.error(
                    "[ArticleDaoImpl#getCountByCategory] 查询分类文章数失败，categoryId={}",
                    categoryId,
                    e
            );
        }
        return 0;
    }

    @Override
    public List<Article> searchByTitle(String keyword, int offset, int limit) {
        String sql = "SELECT a.*, u.username AS author_name, u.nickname AS author_nickname, " +
                     "u.avatar AS author_avatar, c.name AS category_name, " +
                     "(SELECT COUNT(*) FROM comment WHERE article_id = a.id) AS comment_count " +
                     "FROM article a " +
                     "LEFT JOIN user u ON a.user_id = u.id " +
                     "LEFT JOIN category c ON a.category_id = c.id " +
                     "WHERE a.status = 1 AND a.title LIKE ? " +
                     "ORDER BY a.create_time DESC LIMIT ?, ?";
        List<Article> articles = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            // 对keyword中的SQL通配符进行转义，避免非预期的搜索结果
            String escapedKeyword = keyword.replace("%", "\\%").replace("_", "\\_");
            ps.setString(1, "%" + escapedKeyword + "%");
            ps.setInt(2, offset);
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    articles.add(mapRowToArticle(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.error(
                    "[ArticleDaoImpl#searchByTitle] 搜索文章失败，keywordLength={}，offset={}，limit={}",
                    keyword == null ? 0 : keyword.length(),
                    offset,
                    limit,
                    e
            );
        }
        return articles;
    }

    @Override
    public int getSearchTotalCount(String keyword) {
        String sql = "SELECT COUNT(*) FROM article WHERE status = 1 AND title LIKE ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            // 对keyword中的SQL通配符进行转义，避免非预期的搜索结果
            String escapedKeyword = keyword.replace("%", "\\%").replace("_", "\\_");
            ps.setString(1, "%" + escapedKeyword + "%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            LOGGER.error(
                    "[ArticleDaoImpl#getSearchTotalCount] 查询搜索结果总数失败，keywordLength={}",
                    keyword == null ? 0 : keyword.length(),
                    e
            );
        }
        return 0;
    }

    @Override
    public List<Article> findByUserId(Integer userId, int offset, int limit) {
        String sql = "SELECT a.*, u.username AS author_name, u.nickname AS author_nickname, " +
                     "u.avatar AS author_avatar, c.name AS category_name, " +
                     "(SELECT COUNT(*) FROM comment WHERE article_id = a.id) AS comment_count " +
                     "FROM article a " +
                     "LEFT JOIN user u ON a.user_id = u.id " +
                     "LEFT JOIN category c ON a.category_id = c.id " +
                     "WHERE a.user_id = ? " +
                     "ORDER BY a.create_time DESC LIMIT ?, ?";
        List<Article> articles = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, offset);
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    articles.add(mapRowToArticle(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.error(
                    "[ArticleDaoImpl#findByUserId] 分页查询用户文章失败，userId={}，offset={}，limit={}",
                    userId,
                    offset,
                    limit,
                    e
            );
        }
        return articles;
    }

    @Override
    public int getCountByUserId(Integer userId) {
        String sql = "SELECT COUNT(*) FROM article WHERE user_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            LOGGER.error(
                    "[ArticleDaoImpl#getCountByUserId] 查询用户文章数失败，userId={}",
                    userId,
                    e
            );
        }
        return 0;
    }

    @Override
    public boolean incrementViewCount(Integer articleId) {
        String sql = "UPDATE article SET view_count = view_count + 1 WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, articleId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.error(
                    "[ArticleDaoImpl#incrementViewCount] 增加文章浏览量失败，articleId={}",
                    articleId,
                    e
            );
            return false;
        }
    }

    @Override
    public List<Article> findAllByPage(int offset, int limit) {
        String sql = "SELECT a.*, u.username AS author_name, u.nickname AS author_nickname, " +
                     "u.avatar AS author_avatar, c.name AS category_name, " +
                     "(SELECT COUNT(*) FROM comment WHERE article_id = a.id) AS comment_count " +
                     "FROM article a " +
                     "LEFT JOIN user u ON a.user_id = u.id " +
                     "LEFT JOIN category c ON a.category_id = c.id " +
                     "ORDER BY a.create_time DESC LIMIT ?, ?";
        return executeQuery(sql, offset, limit);
    }

    @Override
    public int getAllTotalCount() {
        String sql = "SELECT COUNT(*) FROM article";
        return getCount(sql);
    }

    @Override
    public boolean batchDelete(Integer[] ids) {
        // 参数校验：空数组或null直接返回false
        if (ids == null || ids.length == 0) {
            return false;
        }

        Connection conn = null;
        PreparedStatement psComment = null;
        PreparedStatement psArticle = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            // 先删除评论
            String sqlDeleteComments = "DELETE FROM comment WHERE article_id = ?";
            psComment = conn.prepareStatement(sqlDeleteComments);
            for (Integer id : ids) {
                // 跳过null元素，防止NPE
                if (id == null) {
                    continue;
                }
                psComment.setInt(1, id);
                psComment.addBatch();
            }
            psComment.executeBatch();

            // 再删除文章
            String sqlDeleteArticle = "DELETE FROM article WHERE id = ?";
            psArticle = conn.prepareStatement(sqlDeleteArticle);
            for (Integer id : ids) {
                // 跳过null元素，防止NPE
                if (id == null) {
                    continue;
                }
                psArticle.setInt(1, id);
                psArticle.addBatch();
            }
            psArticle.executeBatch();

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    LOGGER.error(
                            "[ArticleDaoImpl#batchDelete] 回滚批量删除事务失败，batchSize={}",
                            ids.length,
                            ex
                    );
                }
            }
            LOGGER.error(
                    "[ArticleDaoImpl#batchDelete] 批量删除文章事务失败，batchSize={}",
                    ids.length,
                    e
            );
            return false;
        } finally {
            // 恢复自动提交（放在独立的try-catch中，避免影响资源关闭）
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    LOGGER.error(
                            "[ArticleDaoImpl#batchDelete] 恢复自动提交失败，batchSize={}",
                            ids.length,
                            e
                    );
                }
            }
            // 关闭资源（每个close都在独立的try-catch中，确保全部执行）
            if (psComment != null) {
                try {
                    psComment.close();
                } catch (SQLException e) {
                    LOGGER.error(
                            "[ArticleDaoImpl#batchDelete] 关闭评论批量删除语句失败，batchSize={}",
                            ids.length,
                            e
                    );
                }
            }
            if (psArticle != null) {
                try {
                    psArticle.close();
                } catch (SQLException e) {
                    LOGGER.error(
                            "[ArticleDaoImpl#batchDelete] 关闭文章批量删除语句失败，batchSize={}",
                            ids.length,
                            e
                    );
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    LOGGER.error(
                            "[ArticleDaoImpl#batchDelete] 关闭数据库连接失败，batchSize={}",
                            ids.length,
                            e
                    );
                }
            }
        }
    }

    @Override
    public List<Article> findAll() {
        String sql = "SELECT a.*, u.username AS author_name, u.nickname AS author_nickname, " +
                     "u.avatar AS author_avatar, c.name AS category_name, " +
                     "(SELECT COUNT(*) FROM comment WHERE article_id = a.id) AS comment_count " +
                     "FROM article a " +
                     "LEFT JOIN user u ON a.user_id = u.id " +
                     "LEFT JOIN category c ON a.category_id = c.id " +
                     "ORDER BY a.create_time DESC";
        List<Article> articles = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                articles.add(mapRowToArticle(rs));
            }
        } catch (SQLException e) {
            LOGGER.error("[ArticleDaoImpl#findAll] 查询全部文章失败", e);
        }
        return articles;
    }

    /**
     * 执行查询的通用方法
     */
    private List<Article> executeQuery(String sql, int offset, int limit) {
        List<Article> articles = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, offset);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    articles.add(mapRowToArticle(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.error(
                    "[ArticleDaoImpl#executeQuery] 执行分页文章查询失败，offset={}，limit={}",
                    offset,
                    limit,
                    e
            );
        }
        return articles;
    }

    /**
     * 获取总数的通用方法
     */
    private int getCount(String sql) {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.error("[ArticleDaoImpl#getCount] 执行文章计数查询失败", e);
        }
        return 0;
    }

    /**
     * 将ResultSet映射为Article对象（包含关联字段）
     */
    private Article mapRowToArticle(ResultSet rs) throws SQLException {
        Article article = new Article();
        article.setId(rs.getInt("id"));
        article.setTitle(rs.getString("title"));
        article.setContent(rs.getString("content"));
        article.setSummary(rs.getString("summary"));
        article.setCoverImage(rs.getString("cover_image"));
        article.setUserId(rs.getInt("user_id"));
        article.setCategoryId(rs.getInt("category_id"));
        article.setViewCount(rs.getInt("view_count"));
        article.setStatus(rs.getInt("status"));
        article.setCreateTime(rs.getTimestamp("create_time"));
        article.setUpdateTime(rs.getTimestamp("update_time"));

        // 关联字段
        article.setAuthorName(rs.getString("author_name"));
        article.setAuthorNickname(rs.getString("author_nickname"));
        article.setAuthorAvatar(rs.getString("author_avatar"));
        article.setCategoryName(rs.getString("category_name"));
        article.setCommentCount(rs.getInt("comment_count"));

        return article;
    }
}
