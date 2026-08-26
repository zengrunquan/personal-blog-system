package com.blog.service;

import com.blog.entity.Article;
import java.util.List;

/**
 * 文章服务接口
 *
 * @author blog-system
 */
public interface ArticleService {

    /**
     * 发布文章
     *
     * @param article 文章对象
     * @return 操作结果信息，成功返回null
     */
    String publish(Article article);

    /**
     * 根据ID查询文章详情
     *
     * @param id 文章ID
     * @return 文章对象
     */
    Article findById(Integer id);

    /**
     * 更新文章
     *
     * @param article 文章对象
     * @return 操作结果信息，成功返回null
     */
    String update(Article article);

    /**
     * 删除文章
     *
     * @param articleId 文章ID
     * @return 是否成功
     */
    boolean delete(Integer articleId);

    /**
     * 分页查询已发布文章
     *
     * @param page     当前页码
     * @param pageSize 每页数量
     * @return 文章列表
     */
    List<Article> findPublished(int page, int pageSize);

    /**
     * 获取已发布文章总数
     *
     * @return 文章总数
     */
    int getPublishedTotalCount();

    /**
     * 按分类查询文章
     *
     * @param categoryId 分类ID
     * @param page       当前页码
     * @param pageSize   每页数量
     * @return 文章列表
     */
    List<Article> findByCategory(Integer categoryId, int page, int pageSize);

    /**
     * 获取分类下的文章总数
     *
     * @param categoryId 分类ID
     * @return 文章总数
     */
    int getCountByCategory(Integer categoryId);

    /**
     * 搜索文章
     *
     * @param keyword  关键词
     * @param page     当前页码
     * @param pageSize 每页数量
     * @return 文章列表
     */
    List<Article> search(String keyword, int page, int pageSize);

    /**
     * 获取搜索结果总数
     *
     * @param keyword 关键词
     * @return 文章总数
     */
    int getSearchTotalCount(String keyword);

    /**
     * 查询用户的文章
     *
     * @param userId   用户ID
     * @param page     当前页码
     * @param pageSize 每页数量
     * @return 文章列表
     */
    List<Article> findByUserId(Integer userId, int page, int pageSize);

    /**
     * 获取用户的文章总数
     *
     * @param userId 用户ID
     * @return 文章总数
     */
    int getCountByUserId(Integer userId);

    /**
     * 增加浏览次数
     *
     * @param articleId 文章ID
     */
    void incrementViewCount(Integer articleId);

    /**
     * 查询所有文章（管理员功能）
     *
     * @param page     当前页码
     * @param pageSize 每页数量
     * @return 文章列表
     */
    List<Article> findAll(int page, int pageSize);

    /**
     * 获取所有文章总数
     *
     * @return 文章总数
     */
    int getAllTotalCount();

    /**
     * 批量删除文章
     *
     * @param ids 文章ID数组
     * @return 是否成功
     */
    boolean batchDelete(Integer[] ids);

    /**
     * 获取所有文章（用于导出）
     *
     * @return 文章列表
     */
    List<Article> findAll();
}
