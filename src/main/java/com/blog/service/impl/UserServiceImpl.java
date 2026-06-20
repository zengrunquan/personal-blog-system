package com.blog.service.impl;

import com.blog.dao.UserDao;
import com.blog.dao.impl.UserDaoImpl;
import com.blog.entity.User;
import com.blog.service.UserService;
import com.blog.util.MD5Util;

import java.util.List;

/**
 * 用户服务实现类
 *
 * @author blog-system
 */
public class UserServiceImpl implements UserService {

    private final UserDao userDao = new UserDaoImpl();

    @Override
    public String register(String username, String password, String nickname, String email) {
        // 参数验证
        if (username == null || username.trim().isEmpty()) {
            return "用户名不能为空";
        }
        if (password == null || password.trim().isEmpty()) {
            return "密码不能为空";
        }
        if (nickname == null || nickname.trim().isEmpty()) {
            return "昵称不能为空";
        }

        // 用户名格式验证
        if (username.length() < 3 || username.length() > 20) {
            return "用户名长度必须在3-20个字符之间";
        }
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            return "用户名只能包含字母、数字和下划线";
        }

        // 密码长度验证
        if (password.length() < 6 || password.length() > 20) {
            return "密码长度必须在6-20个字符之间";
        }

        // 检查用户名是否已存在
        if (userDao.existsByUsername(username)) {
            return "用户名已存在";
        }

        // 创建用户对象，密码加密存储
        User user = new User(username, MD5Util.md5(password), nickname, email);
        user.setRole(0);  // 普通用户
        user.setStatus(1); // 正常状态

        // 插入数据库
        boolean success = userDao.insert(user);
        return success ? null : "注册失败，请稍后重试";
    }

    @Override
    public User login(String username, String password) {
        if (username == null || password == null) {
            return null;
        }

        // 根据用户名查询用户
        User user = userDao.findByUsername(username);
        if (user == null) {
            return null;
        }

        // 验证密码
        if (!MD5Util.verify(password, user.getPassword())) {
            return null;
        }

        // 检查用户状态
        if (user.getStatus() != null && user.getStatus() == 0) {
            return null; // 用户已被禁用
        }

        // 清空密码后返回
        user.setPassword(null);
        return user;
    }

    @Override
    public User findById(Integer id) {
        User user = userDao.findById(id);
        if (user != null) {
            user.setPassword(null); // 不返回密码
        }
        return user;
    }

    @Override
    public boolean updateProfile(User user) {
        return userDao.update(user);
    }

    @Override
    public boolean updateAvatar(Integer userId, String avatar) {
        return userDao.updateAvatar(userId, avatar);
    }

    @Override
    public String changePassword(Integer userId, String oldPassword, String newPassword) {
        // 参数验证
        if (oldPassword == null || oldPassword.isEmpty()) {
            return "请输入原密码";
        }
        if (newPassword == null || newPassword.length() < 6 || newPassword.length() > 20) {
            return "新密码长度必须在6-20个字符之间";
        }

        // 查询用户
        User user = userDao.findById(userId);
        if (user == null) {
            return "用户不存在";
        }

        // 验证原密码
        if (!MD5Util.verify(oldPassword, user.getPassword())) {
            return "原密码错误";
        }

        // 更新密码
        boolean success = userDao.updatePassword(userId, MD5Util.md5(newPassword));
        return success ? null : "修改密码失败";
    }

    @Override
    public List<User> findByPage(int page, int pageSize) {
        // 分页参数校验，避免负偏移量或无效查询
        if (page <= 0) page = 1;
        if (pageSize <= 0) pageSize = 10;
        int offset = (page - 1) * pageSize;
        return userDao.findByPage(offset, pageSize);
    }

    @Override
    public int getTotalCount() {
        return userDao.getTotalCount();
    }

    @Override
    public boolean deleteUser(Integer userId) {
        return userDao.delete(userId);
    }

    @Override
    public boolean updateStatus(Integer userId, Integer status) {
        return userDao.updateStatus(userId, status);
    }
}
