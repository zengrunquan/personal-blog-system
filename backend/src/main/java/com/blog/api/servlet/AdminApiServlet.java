package com.blog.api.servlet;

import com.blog.api.response.PageResult;
import com.blog.api.support.DtoMapper;
import com.blog.entity.Article;
import com.blog.entity.Category;
import com.blog.entity.User;
import com.blog.listener.OnlineUserListener;
import com.blog.service.ArticleService;
import com.blog.service.CategoryService;
import com.blog.service.UserService;
import com.blog.service.impl.ArticleServiceImpl;
import com.blog.service.impl.CategoryServiceImpl;
import com.blog.service.impl.UserServiceImpl;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@WebServlet(urlPatterns = {"/api/admin", "/api/admin/*"})
public class AdminApiServlet extends BaseApiServlet {

    private final UserService userService = new UserServiceImpl();
    private final ArticleService articleService = new ArticleServiceImpl();
    private final CategoryService categoryService = new CategoryServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        execute(request, response, () -> {
            requireAdmin(request);
            String path = pathInfo(request);
            switch (path) {
                case "/":
                case "/dashboard":
                    dashboard(response);
                    return;
                case "/users":
                    users(request, response);
                    return;
                case "/articles":
                    articles(request, response);
                    return;
                case "/categories":
                    categories(request, response);
                    return;
                case "/articles/export":
                    exportArticles(response);
                    return;
                default:
                    throw notFound("后台接口不存在");
            }
        });
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        execute(request, response, () -> {
            requireAdmin(request);
            String path = pathInfo(request);
            if ("/categories".equals(path)) {
                CategoryRequest body = readJson(request, CategoryRequest.class);
                String result = categoryService.add(body.name, body.description, safeSortOrder(body.sortOrder));
                if (result != null) throw validation("name", result);
                writeCreated(response, Map.of("created", true), "分类已创建");
                return;
            }
            if ("/articles/batch-delete".equals(path)) {
                BatchDeleteRequest body = readJson(request, BatchDeleteRequest.class);
                if (body.ids == null || body.ids.length == 0) throw validation("ids", "请选择文章");
                if (!articleService.batchDelete(body.ids)) throw badRequest("DELETE_FAILED", "批量删除失败");
                writeSuccess(response, Map.of("deleted", body.ids.length), "文章已批量删除");
                return;
            }
            throw notFound("后台接口不存在");
        });
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        execute(request, response, () -> {
            requireAdmin(request);
            String[] parts = segments(pathInfo(request));
            if (parts.length == 3 && "users".equals(parts[0]) && "status".equals(parts[2])) {
                int userId = positiveId(parts[1], "id");
                StatusRequest body = readJson(request, StatusRequest.class);
                int status = body.status != null && body.status == 0 ? 0 : 1;
                if (!userService.updateStatus(userId, status)) throw badRequest("UPDATE_FAILED", "状态更新失败");
                writeSuccess(response, Map.of("id", userId, "status", status), "用户状态已更新");
                return;
            }
            if (parts.length == 2 && "categories".equals(parts[0])) {
                int id = positiveId(parts[1], "id");
                Category existing = categoryService.findById(id);
                if (existing == null) throw notFound("分类不存在");
                CategoryRequest body = readJson(request, CategoryRequest.class);
                existing.setName(body.name);
                existing.setDescription(body.description);
                existing.setSortOrder(safeSortOrder(body.sortOrder));
                String result = categoryService.update(existing);
                if (result != null) throw validation("name", result);
                writeSuccess(response, DtoMapper.toCategoryDto(existing), "分类已更新");
                return;
            }
            throw notFound("后台接口不存在");
        });
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        execute(request, response, () -> {
            User admin = requireAdmin(request);
            String[] parts = segments(pathInfo(request));
            if (parts.length != 2) throw notFound("后台接口不存在");
            int id = positiveId(parts[1], "id");
            switch (parts[0]) {
                case "users":
                    if (admin.getId().equals(id)) throw forbidden("不能删除当前登录的管理员账号");
                    if (!userService.deleteUser(id)) throw badRequest("DELETE_FAILED", "用户删除失败");
                    break;
                case "articles":
                    if (!articleService.delete(id)) throw badRequest("DELETE_FAILED", "文章删除失败");
                    break;
                case "categories":
                    if (!categoryService.delete(id)) throw badRequest("DELETE_FAILED", "分类删除失败");
                    break;
                default:
                    throw notFound("后台接口不存在");
            }
            writeSuccess(response, Map.of("id", id), "删除成功");
        });
    }

    private void dashboard(HttpServletResponse response) throws IOException {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("stats", Map.of(
                "users", userService.getTotalCount(),
                "articles", articleService.getAllTotalCount(),
                "categories", categoryService.getTotalCount(),
                "online", OnlineUserListener.getOnlineCount()
        ));
        data.put("recentArticles", DtoMapper.toArticleDtos(articleService.findAll(1, 6)));
        writeSuccess(response, data);
    }

    private void users(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int page = intQuery(request, "page", 1, 1, Integer.MAX_VALUE);
        int pageSize = intQuery(request, "pageSize", 15, 1, 100);
        writeSuccess(response, new PageResult<>(
                DtoMapper.toUserDtos(userService.findByPage(page, pageSize)),
                page, pageSize, userService.getTotalCount()
        ));
    }

    private void articles(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int page = intQuery(request, "page", 1, 1, Integer.MAX_VALUE);
        int pageSize = intQuery(request, "pageSize", 15, 1, 100);
        writeSuccess(response, new PageResult<>(
                DtoMapper.toArticleDtos(articleService.findAll(page, pageSize)),
                page, pageSize, articleService.getAllTotalCount()
        ));
    }

    private void categories(HttpServletRequest request, HttpServletResponse response) throws IOException {
        writeSuccess(response, DtoMapper.toCategoryDtos(categoryService.findAllWithArticleCount()));
    }

    private void exportArticles(HttpServletResponse response) throws IOException {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=articles.csv");
        response.getOutputStream().write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
        PrintWriter writer = response.getWriter();
        writer.println("ID,标题,作者,分类,浏览次数,状态,创建时间");
        for (Article article : articleService.findAll()) {
            writer.printf("%d,\"%s\",\"%s\",\"%s\",%d,\"%s\",\"%s\"%n",
                    article.getId(), csv(article.getTitle()), csv(article.getAuthorNickname()),
                    csv(article.getCategoryName()), article.getViewCount(),
                    Integer.valueOf(1).equals(article.getStatus()) ? "已发布" : "草稿",
                    article.getCreateTime());
        }
        writer.flush();
    }

    private User requireAdmin(HttpServletRequest request) {
        User user = requireUser(request);
        if (!user.isAdmin()) throw forbidden("当前账号没有管理员权限");
        return user;
    }

    private int safeSortOrder(Integer sortOrder) {
        return sortOrder == null ? 0 : Math.max(sortOrder, 0);
    }

    private String csv(String value) {
        return value == null ? "" : value.replace("\"", "\"\"").replace("\r", " ").replace("\n", " ");
    }

    private String[] segments(String path) {
        return java.util.Arrays.stream(path.split("/"))
                .filter(part -> !part.isBlank()).toArray(String[]::new);
    }

    private static final class StatusRequest { private Integer status; }
    private static final class CategoryRequest {
        private String name;
        private String description;
        private Integer sortOrder;
    }
    private static final class BatchDeleteRequest { private Integer[] ids; }
}
