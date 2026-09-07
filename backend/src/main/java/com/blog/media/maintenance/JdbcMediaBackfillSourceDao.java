package com.blog.media.maintenance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** 回填专用 JDBC 查询；只读取建立媒体引用所需的列，不复用页面 DAO 的吞错语义。 */
public class JdbcMediaBackfillSourceDao implements MediaBackfillSourceDao {

    @Override
    public List<UserMediaSource> loadUsers(Connection connection) throws SQLException {
        Objects.requireNonNull(connection, "回填查询连接不能为空");
        List<UserMediaSource> users = new ArrayList<>();
        String sql = "SELECT id, avatar FROM `user` ORDER BY id ASC";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                users.add(new UserMediaSource(
                        resultSet.getInt("id"),
                        resultSet.getString("avatar")
                ));
            }
        }
        return users;
    }

    @Override
    public List<ArticleMediaSource> loadArticles(Connection connection) throws SQLException {
        Objects.requireNonNull(connection, "回填查询连接不能为空");
        List<ArticleMediaSource> articles = new ArrayList<>();
        String sql = "SELECT id, content, cover_image FROM article ORDER BY id ASC";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                articles.add(new ArticleMediaSource(
                        resultSet.getInt("id"),
                        resultSet.getString("content"),
                        resultSet.getString("cover_image")
                ));
            }
        }
        return articles;
    }
}
