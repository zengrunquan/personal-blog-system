package com.blog.dao;

import com.blog.entity.Article;
import java.util.List;

/**
 * 文章数据访问接口
 *
 * @author blog-system
 */
public interface ArticleDao {

    /**
     * 新增文章
     *
     * @param article 文章对象
     * @return 是否成功
     */
    boolean insert(Article article);

    /**
     * 根据ID查询文章（包含作者和分类信息）
     *
     * @param id 文章ID
     * @return 文章对象
     */
    Article findById(Integer id);

    /**
     * 更新文章
     *
     * @param article 文章对象
     * @return 是否成功
     */
    boolean update(Article article);

    /**
     * 删除文章
     *
     * @param articleId 文章ID
     * @return 是否成功
     */
    boolean delete(Integer articleId);

    /**
     * 分页查询已发布文章（按时间倒序）
     *
     * @param offset 偏移量
     * @param limit  每页数量
     * @return 文章列表
     */
    List<Article> findPublishedByPage(int offset, int limit);

    /**
     * 获取已发布文章总数
     *
     * @return 文章总数
     */
    int getPublishedTotalCount();

    /**
     * 按分类分页查询文章
     *
     * @param categoryId 分类ID
     * @param offset     偏移量
     * @param limit      每页数量
     * @return 文章列表
     */
    List<Article> findByCategory(Integer categoryId, int offset, int limit);

    /**
     * 获取某分类下的文章总数
     *
     * @param categoryId 分类ID
     * @return 文章总数
     */
    int getCountByCategory(Integer categoryId);

    /**
     * 搜索文章（按标题模糊查询）
     *
     * @param keyword 关键词
     * @param offset  偏移量
     * @param limit   每页数量
     * @return 文章列表
     */
    List<Article> searchByTitle(String keyword, int offset, int limit);

    /**
     * 搜索文章总数
     *
     * @param keyword 关键词
     * @return 文章总数
     */
    int getSearchTotalCount(String keyword);

    /**
     * 查询用户的文章列表
     *
     * @param userId 用户ID
     * @param offset 偏移量
     * @param limit  每页数量
     * @return 文章列表
     */
    List<Article> findByUserId(Integer userId, int offset, int limit);

    /**
     * 获取用户的文章总数
     *
     * @param userId 用户ID
     * @return 文章总数
     */
    int getCountByUserId(Integer userId);

    /**
     * 增加文章浏览次数
     *
     * @param articleId 文章ID
     * @return 是否成功
     */
    boolean incrementViewCount(Integer articleId);

    /**
     * 获取所有文章（管理员功能）
     *
     * @param offset 偏移量
     * @param limit  每页数量
     * @return 文章列表
     */
    List<Article> findAllByPage(int offset, int limit);

    /**
     * 获取所有文章总数（管理员功能）
     *
     * @return 文章总数
     */
    int getAllTotalCount();

    /**
     * 批量删除文章（事务）
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
