package com.blog.dao;

import com.blog.entity.Category;
import java.util.List;

/**
 * 分类数据访问接口
 *
 * @author blog-system
 */
public interface CategoryDao {

    /**
     * 新增分类
     *
     * @param category 分类对象
     * @return 是否成功
     */
    boolean insert(Category category);

    /**
     * 根据ID查询分类
     *
     * @param id 分类ID
     * @return 分类对象
     */
    Category findById(Integer id);

    /**
     * 根据名称查询分类
     *
     * @param name 分类名称
     * @return 分类对象
     */
    Category findByName(String name);

    /**
     * 检查分类名是否存在
     *
     * @param name 分类名称
     * @return 是否存在
     */
    boolean existsByName(String name);

    /**
     * 获取所有分类
     *
     * @return 分类列表
     */
    List<Category> findAll();

    /**
     * 分页查询分类
     *
     * @param offset 偏移量
     * @param limit  每页数量
     * @return 分类列表
     */
    List<Category> findByPage(int offset, int limit);

    /**
     * 获取分类总数
     *
     * @return 分类总数
     */
    int getTotalCount();

    /**
     * 更新分类
     *
     * @param category 分类对象
     * @return 是否成功
     */
    boolean update(Category category);

    /**
     * 删除分类（事务：同时删除该分类下的文章）
     *
     * @param categoryId 分类ID
     * @return 是否成功
     */
    boolean delete(Integer categoryId);

    /**
     * 查询分类及其文章数量
     *
     * @return 分类列表（包含文章数量）
     */
    List<Category> findAllWithArticleCount();
}
