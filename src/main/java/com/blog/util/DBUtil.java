package com.blog.util;

import com.alibaba.druid.pool.DruidDataSourceFactory;

import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

/**
 * 数据库工具类
 * 使用Druid连接池管理数据库连接
 *
 * @author blog-system
 */
public class DBUtil {

    /** Druid数据源 */
    private static DataSource dataSource;

    static {
        // 使用try-with-resources确保InputStream在加载失败时也能正确关闭
        try (InputStream is = DBUtil.class.getClassLoader().getResourceAsStream("db.properties")) {
            Properties properties = new Properties();
            properties.load(is);
            // 创建Druid数据源
            dataSource = DruidDataSourceFactory.createDataSource(properties);
        } catch (Exception e) {
            throw new RuntimeException("初始化数据库连接池失败", e);
        }
    }

    /**
     * 获取数据库连接
     *
     * @return 数据库连接对象
     * @throws SQLException 获取连接异常
     */
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * 关闭数据库资源
     *
     * @param conn 数据库连接
     * @param ps   预编译语句
     * @param rs   结果集
     */
    public static void close(Connection conn, PreparedStatement ps, ResultSet rs) {
        try {
            if (rs != null) {
                rs.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            if (ps != null) {
                ps.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 关闭数据库资源（无结果集）
     *
     * @param conn 数据库连接
     * @param ps   预编译语句
     */
    public static void close(Connection conn, PreparedStatement ps) {
        close(conn, ps, null);
    }

    /**
     * 获取数据源（用于Listener获取在线人数等场景）
     *
     * @return 数据源对象
     */
    public static DataSource getDataSource() {
        return dataSource;
    }
}
