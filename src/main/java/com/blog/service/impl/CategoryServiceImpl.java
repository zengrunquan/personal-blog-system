package com.blog.service.impl;

import com.blog.dao.CategoryDao;
import com.blog.dao.impl.CategoryDaoImpl;
import com.blog.entity.Category;
import com.blog.service.CategoryService;

import java.util.List;
import java.util.Objects;

/**
 * 分类服务实现类
 *
 * @author blog-system
 */
public class CategoryServiceImpl implements CategoryService {

    private final CategoryDao categoryDao;

    public CategoryServiceImpl() {
        this(new CategoryDaoImpl());
    }

    public CategoryServiceImpl(CategoryDao categoryDao) {
        this.categoryDao = Objects.requireNonNull(categoryDao, "categoryDao 不能为空");
    }

    @Override
    public String add(String name, String description) {
        return add(name, description, 0);
    }

    @Override
    public String add(String name, String description, int sortOrder) {
        // 参数验证
        if (name == null || name.trim().isEmpty()) {
            return "分类名称不能为空";
        }

        // 检查分类名是否已存在
        if (categoryDao.existsByName(name)) {
            return "分类名称已存在";
        }

        // 创建分类对象
        Category category = new Category(name.trim(), description);
        category.setSortOrder(sortOrder);

        // 插入数据库
        boolean success = categoryDao.insert(category);
        return success ? null : "添加分类失败";
    }

    @Override
    public Category findById(Integer id) {
        return categoryDao.findById(id);
    }

    @Override
    public List<Category> findAll() {
        return categoryDao.findAll();
    }

    @Override
    public List<Category> findByPage(int page, int pageSize) {
        // 分页参数校验，避免负偏移量或无效查询
        if (page <= 0) page = 1;
        if (pageSize <= 0) pageSize = 10;
        int offset = (page - 1) * pageSize;
        return categoryDao.findByPage(offset, pageSize);
    }

    @Override
    public int getTotalCount() {
        return categoryDao.getTotalCount();
    }

    @Override
    public String update(Category category) {
        // 参数验证
        if (category.getName() == null || category.getName().trim().isEmpty()) {
            return "分类名称不能为空";
        }

        // 检查分类名是否已被其他分类使用
        Category existing = categoryDao.findByName(category.getName());
        if (existing != null && !existing.getId().equals(category.getId())) {
            return "分类名称已存在";
        }

        boolean success = categoryDao.update(category);
        return success ? null : "更新分类失败";
    }

    @Override
    public boolean delete(Integer categoryId) {
        return categoryDao.delete(categoryId);
    }

    @Override
    public List<Category> findAllWithArticleCount() {
        return categoryDao.findAllWithArticleCount();
    }
}
