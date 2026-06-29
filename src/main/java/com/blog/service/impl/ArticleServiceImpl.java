package com.blog.service.impl;

import com.blog.dao.ArticleDao;
import com.blog.dao.impl.ArticleDaoImpl;
import com.blog.entity.Article;
import com.blog.service.ArticleService;

import java.util.List;
import java.util.Objects;

/**
 * 文章服务实现类
 *
 * @author blog-system
 */
public class ArticleServiceImpl implements ArticleService {

    private final ArticleDao articleDao;

    public ArticleServiceImpl() {
        this(new ArticleDaoImpl());
    }

    public ArticleServiceImpl(ArticleDao articleDao) {
        this.articleDao = Objects.requireNonNull(articleDao, "articleDao 不能为空");
    }

    @Override
    public String publish(Article article) {
        // 参数验证
        if (article.getTitle() == null || article.getTitle().trim().isEmpty()) {
            return "文章标题不能为空";
        }
        if (article.getContent() == null || article.getContent().trim().isEmpty()) {
            return "文章内容不能为空";
        }
        if (article.getCategoryId() == null) {
            return "请选择文章分类";
        }
        if (article.getUserId() == null) {
            return "用户未登录";
        }

        // 标题长度验证
        if (article.getTitle().length() > 200) {
            return "文章标题不能超过200个字符";
        }

        // 自动生成摘要（如果未提供）
        if (article.getSummary() == null || article.getSummary().trim().isEmpty()) {
            String content = article.getContent();
            // 去除HTML标签
            String plainText = content.replaceAll("<[^>]+>", "");
            // 截取前200个字符作为摘要
            article.setSummary(plainText.length() > 200 ? plainText.substring(0, 200) + "..." : plainText);
        }

        // 设置默认状态
        if (article.getStatus() == null) {
            article.setStatus(1); // 默认发布
        }

        // 插入数据库
        boolean success = articleDao.insert(article);
        return success ? null : "发布文章失败";
    }

    @Override
    public Article findById(Integer id) {
        if (id == null) {
            return null;
        }
        return articleDao.findById(id);
    }

    @Override
    public String update(Article article) {
        // 参数验证
        if (article.getId() == null) {
            return "文章ID不能为空";
        }
        if (article.getTitle() == null || article.getTitle().trim().isEmpty()) {
            return "文章标题不能为空";
        }
        if (article.getContent() == null || article.getContent().trim().isEmpty()) {
            return "文章内容不能为空";
        }
        if (article.getCategoryId() == null) {
            return "请选择文章分类";
        }

        boolean success = articleDao.update(article);
        return success ? null : "更新文章失败";
    }

    @Override
    public boolean delete(Integer articleId) {
        return articleDao.delete(articleId);
    }

    @Override
    public List<Article> findPublished(int page, int pageSize) {
        // 分页参数校验，避免负偏移量或无效查询
        if (page <= 0) page = 1;
        if (pageSize <= 0) pageSize = 10;
        int offset = (page - 1) * pageSize;
        return articleDao.findPublishedByPage(offset, pageSize);
    }

    @Override
    public int getPublishedTotalCount() {
        return articleDao.getPublishedTotalCount();
    }

    @Override
    public List<Article> findByCategory(Integer categoryId, int page, int pageSize) {
        // 分页参数校验，避免负偏移量或无效查询
        if (page <= 0) page = 1;
        if (pageSize <= 0) pageSize = 10;
        int offset = (page - 1) * pageSize;
        return articleDao.findByCategory(categoryId, offset, pageSize);
    }

    @Override
    public int getCountByCategory(Integer categoryId) {
        return articleDao.getCountByCategory(categoryId);
    }

    @Override
    public List<Article> search(String keyword, int page, int pageSize) {
        // 分页参数校验，避免负偏移量或无效查询
        if (page <= 0) page = 1;
        if (pageSize <= 0) pageSize = 10;
        if (keyword == null || keyword.trim().isEmpty()) {
            return findPublished(page, pageSize);
        }
        int offset = (page - 1) * pageSize;
        return articleDao.searchByTitle(keyword.trim(), offset, pageSize);
    }

    @Override
    public int getSearchTotalCount(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getPublishedTotalCount();
        }
        return articleDao.getSearchTotalCount(keyword.trim());
    }

    @Override
    public List<Article> findByUserId(Integer userId, int page, int pageSize) {
        // 分页参数校验，避免负偏移量或无效查询
        if (page <= 0) page = 1;
        if (pageSize <= 0) pageSize = 10;
        int offset = (page - 1) * pageSize;
        return articleDao.findByUserId(userId, offset, pageSize);
    }

    @Override
    public int getCountByUserId(Integer userId) {
        return articleDao.getCountByUserId(userId);
    }

    @Override
    public void incrementViewCount(Integer articleId) {
        articleDao.incrementViewCount(articleId);
    }

    @Override
    public List<Article> findAll(int page, int pageSize) {
        // 分页参数校验，避免负偏移量或无效查询
        if (page <= 0) page = 1;
        if (pageSize <= 0) pageSize = 10;
        int offset = (page - 1) * pageSize;
        return articleDao.findAllByPage(offset, pageSize);
    }

    @Override
    public int getAllTotalCount() {
        return articleDao.getAllTotalCount();
    }

    @Override
    public boolean batchDelete(Integer[] ids) {
        // 参数校验：空数组或null直接返回false
        if (ids == null || ids.length == 0) {
            return false;
        }
        return articleDao.batchDelete(ids);
    }

    @Override
    public List<Article> findAll() {
        return articleDao.findAll();
    }
}
