package com.blog.service.impl;

import com.blog.dao.ArticleDao;
import com.blog.dao.impl.ArticleDaoImpl;
import com.blog.entity.Article;
import com.blog.media.service.MediaReferenceService;
import com.blog.media.service.MediaReferenceServiceImpl;
import com.blog.service.ArticleService;
import com.blog.api.support.HtmlContentSanitizer;
import com.blog.util.JdbcTransactionManager;
import com.blog.util.TransactionException;
import com.blog.util.TransactionManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

/**
 * 文章服务实现类
 *
 * @author blog-system
 */
public class ArticleServiceImpl implements ArticleService {

    private static final Logger LOGGER = LogManager.getLogger(ArticleServiceImpl.class);
    private final ArticleDao articleDao;
    private final MediaReferenceService mediaReferenceService;
    private final TransactionManager transactionManager;

    public ArticleServiceImpl() {
        this(new ArticleDaoImpl(), new MediaReferenceServiceImpl(), new JdbcTransactionManager());
    }

    public ArticleServiceImpl(ArticleDao articleDao) {
        this(articleDao, new MediaReferenceServiceImpl(), new JdbcTransactionManager());
    }

    public ArticleServiceImpl(
            ArticleDao articleDao,
            MediaReferenceService mediaReferenceService,
            TransactionManager transactionManager
    ) {
        this.articleDao = Objects.requireNonNull(articleDao, "articleDao 不能为空");
        this.mediaReferenceService = Objects.requireNonNull(
                mediaReferenceService, "mediaReferenceService 不能为空");
        this.transactionManager = Objects.requireNonNull(
                transactionManager, "transactionManager 不能为空");
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

        // 富文本会被多个入口复用，因此在 Service 边界统一清洗，避免绕过 API 直接写入危险 HTML。
        article.setContent(HtmlContentSanitizer.sanitize(article.getContent()));

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

        try {
            transactionManager.inTransaction(connection -> {
                if (!articleDao.insert(connection, article) || article.getId() == null) {
                    throw new SQLException("新增文章未生成有效 ID");
                }
                mediaReferenceService.syncArticleReferences(
                        connection, article.getId(), article.getContent(), article.getCoverImage());
                return true;
            });
            return null;
        } catch (TransactionException e) {
            LOGGER.error("[ArticleServiceImpl#publish] 发布文章事务失败，userId={}，categoryId={}",
                    article.getUserId(), article.getCategoryId(), e);
            return "发布文章失败";
        }
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

        // 更新与新增使用相同白名单，保证历史编辑不会重新引入可执行标记。
        article.setContent(HtmlContentSanitizer.sanitize(article.getContent()));

        try {
            transactionManager.inTransaction(connection -> {
                if (!articleDao.update(connection, article)) {
                    throw new SQLException("更新文章没有影响任何行");
                }
                mediaReferenceService.syncArticleReferences(
                        connection, article.getId(), article.getContent(), article.getCoverImage());
                return true;
            });
            return null;
        } catch (TransactionException e) {
            LOGGER.error("[ArticleServiceImpl#update] 更新文章事务失败，articleId={}", article.getId(), e);
            return "更新文章失败";
        }
    }

    @Override
    public boolean delete(Integer articleId) {
        try {
            transactionManager.inTransaction(connection -> {
                if (!articleDao.delete(connection, articleId)) {
                    throw new SQLException("删除文章没有影响任何行");
                }
                return true;
            });
            return true;
        } catch (TransactionException e) {
            LOGGER.error("[ArticleServiceImpl#delete] 删除文章事务失败，articleId={}", articleId, e);
            return false;
        }
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
        try {
            transactionManager.inTransaction(connection -> {
                if (!articleDao.batchDelete(connection, ids)) {
                    throw new SQLException("批量删除文章没有影响任何行");
                }
                return true;
            });
            return true;
        } catch (TransactionException e) {
            LOGGER.error("[ArticleServiceImpl#batchDelete] 批量删除文章事务失败，batchSize={}",
                    ids.length, e);
            return false;
        }
    }

    @Override
    public List<Article> findAll() {
        return articleDao.findAll();
    }
}
