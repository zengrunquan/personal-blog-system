package com.blog.service;

import com.blog.entity.Category;
import java.util.List;

/**
 * 分类服务接口
 *
 * @author blog-system
 */
public interface CategoryService {

    /**
     * 新增分类
     *
     * @param name        分类名称
     * @param description 分类描述
     * @return 操作结果信息，成功返回null
     */
    String add(String name, String description);

    /**
     * 新增分类（带排序顺序）
     *
     * @param name        分类名称
     * @param description 分类描述
     * @param sortOrder   排序顺序
     * @return 操作结果信息，成功返回null
     */
    String add(String name, String description, int sortOrder);

    /**
     * 根据ID查询分类
     *
     * @param id 分类ID
     * @return 分类对象
     */
    Category findById(Integer id);

    /**
     * 获取所有分类
     *
     * @return 分类列表
     */
    List<Category> findAll();

    /**
     * 获取分类列表（分页）
     *
     * @param page     当前页码
     * @param pageSize 每页数量
     * @return 分类列表
     */
    List<Category> findByPage(int page, int pageSize);

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
     * @return 操作结果信息，成功返回null
     */
    String update(Category category);

    /**
     * 删除分类（同时删除该分类下的文章）
     *
     * @param categoryId 分类ID
     * @return 是否成功
     */
    boolean delete(Integer categoryId);

    /**
     * 获取分类列表（包含文章数量）
     *
     * @return 分类列表
     */
    List<Category> findAllWithArticleCount();
}
