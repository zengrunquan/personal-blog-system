package com.blog.dao;

import com.blog.entity.User;
import java.util.List;

/**
 * 用户数据访问接口
 *
 * @author blog-system
 */
public interface UserDao {

    /**
     * 用户注册
     *
     * @param user 用户对象
     * @return 是否成功
     */
    boolean insert(User user);

    /**
     * 根据用户名查询用户（登录验证）
     *
     * @param username 用户名
     * @return 用户对象，不存在返回null
     */
    User findByUsername(String username);

    /**
     * 根据ID查询用户
     *
     * @param id 用户ID
     * @return 用户对象
     */
    User findById(Integer id);

    /**
     * 检查用户名是否存在
     *
     * @param username 用户名
     * @return 是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 更新用户信息
     *
     * @param user 用户对象
     * @return 是否成功
     */
    boolean update(User user);

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
     * @param newPassword 新密码（已加密）
     * @return 是否成功
     */
    boolean updatePassword(Integer userId, String newPassword);

    /**
     * 获取所有用户列表（管理员功能）
     *
     * @return 用户列表
     */
    List<User> findAll();

    /**
     * 分页查询用户
     *
     * @param offset 偏移量
     * @param limit  每页数量
     * @return 用户列表
     */
    List<User> findByPage(int offset, int limit);

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
    boolean delete(Integer userId);

    /**
     * 更新用户状态（启用/禁用）
     *
     * @param userId 用户ID
     * @param status 状态
     * @return 是否成功
     */
    boolean updateStatus(Integer userId, Integer status);
}
