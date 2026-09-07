package com.blog.media.maintenance;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/** 回填专用数据源；查询必须复用调用方连接并向上传播 SQLException。 */
public interface MediaBackfillSourceDao {

    List<UserMediaSource> loadUsers(Connection connection) throws SQLException;

    List<ArticleMediaSource> loadArticles(Connection connection) throws SQLException;
}
