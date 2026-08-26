package com.blog.api.servlet;

import com.blog.api.dto.ArticleDto;
import com.blog.api.response.PageResult;
import com.blog.api.support.DtoMapper;
import com.blog.entity.Article;
import com.blog.entity.User;
import com.blog.service.ArticleService;
import com.blog.service.CategoryService;
import com.blog.service.CommentService;
import com.blog.service.impl.ArticleServiceImpl;
import com.blog.service.impl.CategoryServiceImpl;
import com.blog.service.impl.CommentServiceImpl;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@WebServlet(urlPatterns = {"/api/articles", "/api/articles/*"})
public class ArticlesApiServlet extends BaseApiServlet {

    private final ArticleService articleService;
    private final CategoryService categoryService;
    private final CommentService commentService;

    public ArticlesApiServlet() {
        this(new ArticleServiceImpl(), new CategoryServiceImpl(), new CommentServiceImpl());
    }

    ArticlesApiServlet(
            ArticleService articleService,
            CategoryService categoryService,
            CommentService commentService
    ) {
        this.articleService = Objects.requireNonNull(articleService, "articleService 不能为空");
        this.categoryService = Objects.requireNonNull(categoryService, "categoryService 不能为空");
        this.commentService = Objects.requireNonNull(commentService, "commentService 不能为空");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        execute(request, response, () -> {
            String path = pathInfo(request);
            if ("/".equals(path)) {
                list(request, response);
                return;
            }
            String[] parts = segments(path);
            if (parts.length == 1) {
                detail(request, response, positiveId(parts[0], "id"));
                return;
            }
            throw notFound("文章接口不存在");
        });
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        execute(request, response, () -> {
            String[] parts = segments(pathInfo(request));
            if (parts.length == 0) {
                create(request, response);
                return;
            }
            if (parts.length == 2 && "comments".equals(parts[1])) {
                addComment(request, response, positiveId(parts[0], "articleId"));
                return;
            }
            throw notFound("文章接口不存在");
        });
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        execute(request, response, () -> {
            String[] parts = segments(pathInfo(request));
            if (parts.length != 1) throw notFound("文章接口不存在");
            update(request, response, positiveId(parts[0], "id"));
        });
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        execute(request, response, () -> {
            String[] parts = segments(pathInfo(request));
            if (parts.length != 1) throw notFound("文章接口不存在");
            delete(request, response, positiveId(parts[0], "id"));
        });
    }

    private void list(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int page = intQuery(request, "page", 1, 1, Integer.MAX_VALUE);
        int pageSize = intQuery(request, "pageSize", 8, 1, 50);
        String keyword = request.getParameter("q");
        Integer categoryId = nullableId(request.getParameter("category"));
        List<Article> articles;
        int total;
        if (keyword != null && !keyword.isBlank()) {
            articles = articleService.search(keyword, page, pageSize);
            total = articleService.getSearchTotalCount(keyword);
        } else if (categoryId != null) {
            articles = articleService.findByCategory(categoryId, page, pageSize);
            total = articleService.getCountByCategory(categoryId);
        } else {
            articles = articleService.findPublished(page, pageSize);
            total = articleService.getPublishedTotalCount();
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("articles", new PageResult<>(DtoMapper.toArticleDtos(articles), page, pageSize, total));
        data.put("categories", DtoMapper.toCategoryDtos(categoryService.findAllWithArticleCount()));
        writeSuccess(response, data);
    }

    private void detail(HttpServletRequest request, HttpServletResponse response, int id) throws IOException {
        Article article = articleService.findById(id);
        if (article == null) throw notFound("文章不存在");
        User viewer = optionalUser(request);
        if (!Integer.valueOf(1).equals(article.getStatus())
                && (viewer == null || (!viewer.isAdmin() && !viewer.getId().equals(article.getUserId())))) {
            throw notFound("文章不存在");
        }
        if (Integer.valueOf(1).equals(article.getStatus())) articleService.incrementViewCount(id);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("article", DtoMapper.toArticleDto(article));
        data.put("comments", DtoMapper.toCommentDtos(commentService.findByArticleId(id)));
        writeSuccess(response, data);
    }

    private void create(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User user = requireUser(request);
        ArticleRequest body = readJson(request, ArticleRequest.class);
        Article article = toArticle(body);
        article.setUserId(user.getId());
        String result = articleService.publish(article);
        if (result != null) throw validation("content", result);
        writeCreated(response, DtoMapper.toArticleDto(article), "文章已保存");
    }

    private void update(HttpServletRequest request, HttpServletResponse response, int id) throws IOException {
        User user = requireUser(request);
        Article article = articleService.findById(id);
        if (article == null) throw notFound("文章不存在");
        requireArticleOwner(user, article);
        ArticleRequest body = readJson(request, ArticleRequest.class);
        article.setTitle(cleanText(body.title));
        article.setContent(body.content);
        article.setSummary(cleanText(body.summary));
        article.setCategoryId(body.categoryId);
        article.setStatus(normalizeStatus(body.status));
        article.setCoverImage(cleanText(body.coverImage));
        String result = articleService.update(article);
        if (result != null) throw validation("content", result);
        writeSuccess(response, DtoMapper.toArticleDto(article), "文章已更新");
    }

    private void delete(HttpServletRequest request, HttpServletResponse response, int id) throws IOException {
        User user = requireUser(request);
        Article article = articleService.findById(id);
        if (article == null) throw notFound("文章不存在");
        requireArticleOwner(user, article);
        if (!articleService.delete(id)) throw badRequest("DELETE_FAILED", "删除文章失败");
        writeSuccess(response, Map.of("id", id), "文章已删除");
    }

    private void addComment(HttpServletRequest request, HttpServletResponse response, int articleId)
            throws IOException {
        User user = requireUser(request);
        if (articleService.findById(articleId) == null) throw notFound("文章不存在");
        CommentRequest body = readJson(request, CommentRequest.class);
        String result = commentService.addComment(cleanText(body.content), user.getId(), articleId);
        if (result != null) throw validation("content", result);
        writeCreated(response, Map.of("articleId", articleId), "评论已发表");
    }

    private Article toArticle(ArticleRequest body) {
        Article article = new Article();
        article.setTitle(cleanText(body.title));
        article.setContent(body.content);
        article.setSummary(cleanText(body.summary));
        article.setCategoryId(body.categoryId);
        article.setStatus(normalizeStatus(body.status));
        article.setCoverImage(cleanText(body.coverImage));
        return article;
    }

    private void requireArticleOwner(User user, Article article) {
        if (!user.isAdmin() && !user.getId().equals(article.getUserId())) {
            throw forbidden("只能操作自己的文章");
        }
    }

    private User optionalUser(HttpServletRequest request) {
        Object user = request.getSession(false) == null
                ? null
                : request.getSession(false).getAttribute("loginUser");
        return user instanceof User ? (User) user : null;
    }

    private Integer nullableId(String value) {
        return value == null || value.isBlank() ? null : positiveId(value, "category");
    }

    private int normalizeStatus(Integer status) {
        return status != null && status == 0 ? 0 : 1;
    }

    private String cleanText(String value) {
        return value == null ? null : value.trim();
    }

    private String[] segments(String path) {
        return java.util.Arrays.stream(path.split("/"))
                .filter(part -> !part.isBlank())
                .toArray(String[]::new);
    }

    private static final class ArticleRequest {
        private String title;
        private String content;
        private String summary;
        private String coverImage;
        private Integer categoryId;
        private Integer status;
    }

    private static final class CommentRequest {
        private String content;
    }
}
