package com.blog.service;

import com.blog.entity.User;
import java.util.List;

/**
 * 用户服务接口
 *
 * @author blog-system
 */
public interface UserService {

    /**
     * 用户注册
     *
     * @param username 用户名
     * @param password 密码（明文）
     * @param nickname 昵称
     * @param email    邮箱
     * @return 注册结果信息，成功返回null
     */
    String register(String username, String password, String nickname, String email);

    /**
     * 用户登录
     *
     * @param username 用户名
     * @param password 密码（明文）
     * @return 登录成功返回用户对象，失败返回null
     */
    User login(String username, String password);

    /**
     * 根据ID查询用户
     *
     * @param id 用户ID
     * @return 用户对象
     */
    User findById(Integer id);

    /**
     * 更新用户信息
     *
     * @param user 用户对象
     * @return 是否成功
     */
    boolean updateProfile(User user);

    /**
     * 更新用户头像
     *
     * @param userId 用户ID
     * @param avatar 头像路径
     * @return 是否成功
     */
    boolean updateAvatar(Integer userId, String avatar);

    /**
     * 修改密码
     *
     * @param userId      用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @return 修改结果信息，成功返回null
     */
    String changePassword(Integer userId, String oldPassword, String newPassword);

    /**
     * 获取用户列表（分页）
     *
     * @param page     当前页码
     * @param pageSize 每页数量
     * @return 用户列表
     */
    List<User> findByPage(int page, int pageSize);

    /**
     * 获取用户总数
     *
     * @return 用户总数
     */
    int getTotalCount();

    /**
     * 删除用户（管理员功能）
     *
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean deleteUser(Integer userId);

    /**
     * 更新用户状态（启用/禁用）
     *
     * @param userId 用户ID
     * @param status 状态
     * @return 是否成功
     */
    boolean updateStatus(Integer userId, Integer status);
}
