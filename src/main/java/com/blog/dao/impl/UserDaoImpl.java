package com.blog.dao.impl;

import com.blog.dao.UserDao;
import com.blog.entity.User;
import com.blog.util.DBUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户数据访问实现类
 * 使用PreparedStatement防止SQL注入
 *
 * @author blog-system
 */
public class UserDaoImpl implements UserDao {

    private static final Logger LOGGER = LogManager.getLogger(UserDaoImpl.class);

    @Override
    public boolean insert(User user) {
        String sql = "INSERT INTO user (username, password, nickname, email, avatar, role, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getNickname());
            ps.setString(4, user.getEmail());
            ps.setString(5, user.getAvatar());
            ps.setInt(6, user.getRole() != null ? user.getRole() : 0);
            ps.setInt(7, user.getStatus() != null ? user.getStatus() : 1);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.error("[UserDaoImpl#insert] 新增用户失败", e);
            return false;
        }
    }

    @Override
    public User findByUsername(String username) {
        String sql = "SELECT * FROM user WHERE username = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.error("[UserDaoImpl#findByUsername] 按用户名查询用户失败", e);
        }
        return null;
    }

    @Override
    public User findById(Integer id) {
        String sql = "SELECT * FROM user WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.error("[UserDaoImpl#findById] 按ID查询用户失败，userId={}", id, e);
        }
        return null;
    }

    @Override
    public boolean existsByUsername(String username) {
        String sql = "SELECT COUNT(*) FROM user WHERE username = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            LOGGER.error("[UserDaoImpl#existsByUsername] 检查用户名是否存在失败", e);
        }
        return false;
    }

    @Override
    public boolean update(User user) {
        // 参数校验：防止NPE
        if (user == null || user.getId() == null) {
            return false;
        }
        String sql = "UPDATE user SET nickname = ?, email = ? WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getNickname());
            ps.setString(2, user.getEmail());
            ps.setInt(3, user.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.error("[UserDaoImpl#update] 更新用户资料失败，userId={}", user.getId(), e);
            return false;
        }
    }

    @Override
    public boolean updateAvatar(Integer userId, String avatar) {
        String sql = "UPDATE user SET avatar = ? WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, avatar);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.error("[UserDaoImpl#updateAvatar] 更新用户头像失败，userId={}", userId, e);
            return false;
        }
    }

    @Override
    public boolean updatePassword(Integer userId, String newPassword) {
        String sql = "UPDATE user SET password = ? WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newPassword);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.error("[UserDaoImpl#updatePassword] 更新用户密码失败，userId={}", userId, e);
            return false;
        }
    }

    @Override
    public List<User> findAll() {
        String sql = "SELECT * FROM user ORDER BY create_time DESC";
        List<User> users = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(mapRowToUser(rs));
            }
        } catch (SQLException e) {
            LOGGER.error("[UserDaoImpl#findAll] 查询全部用户失败", e);
        }
        return users;
    }

    @Override
    public List<User> findByPage(int offset, int limit) {
        String sql = "SELECT * FROM user ORDER BY create_time DESC LIMIT ?, ?";
        List<User> users = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, offset);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(mapRowToUser(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.error(
                    "[UserDaoImpl#findByPage] 分页查询用户失败，offset={}，limit={}",
                    offset,
                    limit,
                    e
            );
        }
        return users;
    }

    @Override
    public int getTotalCount() {
        String sql = "SELECT COUNT(*) FROM user";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.error("[UserDaoImpl#getTotalCount] 查询用户总数失败", e);
        }
        return 0;
    }

    @Override
    public boolean delete(Integer userId) {
        String sql = "DELETE FROM user WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.error("[UserDaoImpl#delete] 删除用户失败，userId={}", userId, e);
            return false;
        }
    }

    @Override
    public boolean updateStatus(Integer userId, Integer status) {
        // 参数校验：防止NPE
        if (userId == null || status == null) {
            return false;
        }
        String sql = "UPDATE user SET status = ? WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, status);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.error(
                    "[UserDaoImpl#updateStatus] 更新用户状态失败，userId={}，status={}",
                    userId,
                    status,
                    e
            );
            return false;
        }
    }

    /**
     * 将ResultSet映射为User对象
     */
    private User mapRowToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setNickname(rs.getString("nickname"));
        user.setEmail(rs.getString("email"));
        user.setAvatar(rs.getString("avatar"));
        user.setRole(rs.getInt("role"));
        user.setStatus(rs.getInt("status"));
        user.setCreateTime(rs.getTimestamp("create_time"));
        user.setUpdateTime(rs.getTimestamp("update_time"));
        return user;
    }
}
