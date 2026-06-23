package com.blog.controller;

import com.blog.entity.Article;
import com.blog.entity.Category;
import com.blog.entity.User;
import com.blog.service.ArticleService;
import com.blog.service.CategoryService;
import com.blog.service.UserService;
import com.blog.service.impl.ArticleServiceImpl;
import com.blog.service.impl.CategoryServiceImpl;
import com.blog.service.impl.UserServiceImpl;
import com.blog.util.PageUtil;
import com.blog.util.StringUtil;
import com.google.gson.Gson;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

/**
 * 管理员控制器
 * 处理后台管理相关的请求：用户管理、文章管理、分类管理
 *
 * @author blog-system
 */
@WebServlet("/admin/*")
public class AdminServlet extends HttpServlet {

    private final UserService userService = new UserServiceImpl();
    private final ArticleService articleService = new ArticleServiceImpl();
    private final CategoryService categoryService = new CategoryServiceImpl();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null) {
            pathInfo = "/dashboard";
        }

        switch (pathInfo) {
            case "/dashboard":
                showDashboard(request, response);
                break;
            case "/users":
                listUsers(request, response);
                break;
            case "/articles":
                listArticles(request, response);
                break;
            case "/categories":
                listCategories(request, response);
                break;
            case "/category/add":
                showAddCategoryForm(request, response);
                break;
            case "/category/edit":
                showEditCategoryForm(request, response);
                break;
            default:
                response.sendRedirect(request.getContextPath() + "/admin/dashboard");
                break;
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null) {
            response.sendRedirect(request.getContextPath() + "/admin/dashboard");
            return;
        }

        if (!verifyCsrfToken(request)) {
            handleInvalidCsrfToken(request, response, pathInfo);
            return;
        }

        switch (pathInfo) {
            case "/user/status":
                updateUserStatus(request, response);
                break;
            case "/user/delete":
                deleteUser(request, response);
                break;
            case "/article/delete":
                deleteArticle(request, response);
                break;
            case "/article/batchDelete":
                batchDeleteArticles(request, response);
                break;
            case "/category/add":
                addCategory(request, response);
                break;
            case "/category/edit":
                updateCategory(request, response);
                break;
            case "/category/delete":
                deleteCategory(request, response);
                break;
            default:
                response.sendRedirect(request.getContextPath() + "/admin/dashboard");
                break;
        }
    }

    /**
     * 显示控制台首页
     */
    private void showDashboard(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // 统计数据
        int userCount = userService.getTotalCount();
        int articleCount = articleService.getAllTotalCount();
        int categoryCount = categoryService.getTotalCount();

        // 获取在线人数（从Listener设置的ServletContext属性）
        Object onlineCountObj = getServletContext().getAttribute("onlineCount");
        int onlineCount = onlineCountObj != null ? (Integer) onlineCountObj : 0;

        // 获取最新文章
        List<Article> recentArticles = articleService.findPublished(1, 5);

        request.setAttribute("userCount", userCount);
        request.setAttribute("articleCount", articleCount);
        request.setAttribute("categoryCount", categoryCount);
        request.setAttribute("commentCount", 0); // 暂时设为0，后续可从CommentService获取
        request.setAttribute("onlineCount", onlineCount);
        request.setAttribute("recentArticles", recentArticles);

        request.getRequestDispatcher("/WEB-INF/views/admin/dashboard.jsp").forward(request, response);
    }

    /**
     * 用户管理列表
     */
    private void listUsers(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int page = getIntParameter(request, "page", 1);
        int pageSize = 15;

        ensureCsrfToken(request);

        List<User> users = userService.findByPage(page, pageSize);
        int totalCount = userService.getTotalCount();
        PageUtil pageUtil = new PageUtil(page, pageSize, totalCount);

        request.setAttribute("users", users);
        request.setAttribute("pageUtil", pageUtil);

        request.getRequestDispatcher("/WEB-INF/views/admin/user-list.jsp").forward(request, response);
    }

    /**
     * 文章管理列表
     */
    private void listArticles(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int page = getIntParameter(request, "page", 1);
        int pageSize = 15;

        ensureCsrfToken(request);

        List<Article> articles = articleService.findAll(page, pageSize);
        int totalCount = articleService.getAllTotalCount();
        PageUtil pageUtil = new PageUtil(page, pageSize, totalCount);

        request.setAttribute("articles", articles);
        request.setAttribute("pageUtil", pageUtil);

        request.getRequestDispatcher("/WEB-INF/views/admin/article-list.jsp").forward(request, response);
    }

    /**
     * 分类管理列表
     */
    private void listCategories(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        ensureCsrfToken(request);

        List<Category> categories = categoryService.findAllWithArticleCount();
        request.setAttribute("categories", categories);

        request.getRequestDispatcher("/WEB-INF/views/admin/category-list.jsp").forward(request, response);
    }

    /**
     * 显示添加分类表单
     */
    private void showAddCategoryForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        ensureCsrfToken(request);
        request.getRequestDispatcher("/WEB-INF/views/admin/category-form.jsp").forward(request, response);
    }

    /**
     * 显示编辑分类表单
     */
    private void showEditCategoryForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String idStr = request.getParameter("id");
        if (idStr == null) {
            response.sendRedirect(request.getContextPath() + "/admin/categories");
            return;
        }

        try {
            int id = Integer.parseInt(idStr);
            Category category = categoryService.findById(id);

            if (category == null) {
                response.sendRedirect(request.getContextPath() + "/admin/categories");
                return;
            }

            ensureCsrfToken(request);
            request.setAttribute("category", category);
            request.getRequestDispatcher("/WEB-INF/views/admin/category-form.jsp").forward(request, response);
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/admin/categories");
        }
    }

    /**
     * 更新用户状态（Ajax接口）
     */
    private void updateUserStatus(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        // 兼容两种参数名：id 和 userId
        String userIdStr = request.getParameter("id");
        if (userIdStr == null) {
            userIdStr = request.getParameter("userId");
        }
        String statusStr = request.getParameter("status");

        // 参数校验：防止NPE
        if (userIdStr == null || statusStr == null) {
            out.print(gson.toJson(Map.of("success", false, "message", "参数错误")));
            return;
        }

        try {
            int userId = Integer.parseInt(userIdStr);
            int status = Integer.parseInt(statusStr);

            boolean success = userService.updateStatus(userId, status);
            out.print(gson.toJson(Map.of("success", success)));
        } catch (NumberFormatException e) {
            out.print(gson.toJson(Map.of("success", false, "message", "参数错误")));
        }
    }

    /**
     * 删除用户（Ajax接口）
     */
    private void deleteUser(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String userIdStr = request.getParameter("userId");

        // 参数校验：防止NPE
        if (userIdStr == null) {
            out.print(gson.toJson(Map.of("success", false, "message", "参数错误")));
            return;
        }

        try {
            int userId = Integer.parseInt(userIdStr);

            // 不能删除自己
            HttpSession session = request.getSession();
            User loginUser = (User) session.getAttribute("loginUser");
            if (loginUser.getId() == userId) {
                out.print(gson.toJson(Map.of("success", false, "message", "不能删除自己的账号")));
                return;
            }

            boolean success = userService.deleteUser(userId);
            out.print(gson.toJson(Map.of("success", success)));
        } catch (NumberFormatException e) {
            out.print(gson.toJson(Map.of("success", false, "message", "参数错误")));
        }
    }

    /**
     * 删除文章（Ajax接口）
     */
    private void deleteArticle(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        // 兼容两种参数名：id 和 articleId
        String articleIdStr = request.getParameter("id");
        if (articleIdStr == null) {
            articleIdStr = request.getParameter("articleId");
        }

        // 参数校验：防止NPE
        if (articleIdStr == null) {
            out.print(gson.toJson(Map.of("success", false, "message", "参数错误")));
            return;
        }

        try {
            int articleId = Integer.parseInt(articleIdStr);
            boolean success = articleService.delete(articleId);
            out.print(gson.toJson(Map.of("success", success)));
        } catch (NumberFormatException e) {
            out.print(gson.toJson(Map.of("success", false, "message", "参数错误")));
        }
    }

    /**
     * 批量删除文章（Ajax接口）
     */
    private void batchDeleteArticles(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        // 前端可能发送 ids=1,2,3（逗号分隔）或 ids=1&ids=2&ids=3（多个参数）
        String[] idStrings = request.getParameterValues("ids");
        if (idStrings == null || idStrings.length == 0) {
            out.print(gson.toJson(Map.of("success", false, "message", "请选择要删除的文章")));
            return;
        }

        // 如果是逗号分隔的单个参数，需要拆分
        java.util.List<String> idList = new java.util.ArrayList<>();
        for (String idStr : idStrings) {
            if (idStr.contains(",")) {
                for (String part : idStr.split(",")) {
                    if (!part.trim().isEmpty()) {
                        idList.add(part.trim());
                    }
                }
            } else {
                idList.add(idStr.trim());
            }
        }

        try {
            Integer[] ids = new Integer[idList.size()];
            for (int i = 0; i < idList.size(); i++) {
                ids[i] = Integer.parseInt(idList.get(i));
            }

            boolean success = articleService.batchDelete(ids);
            out.print(gson.toJson(Map.of("success", success)));
        } catch (NumberFormatException e) {
            out.print(gson.toJson(Map.of("success", false, "message", "参数错误")));
        }
    }

    /**
     * 添加分类
     */
    private void addCategory(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String name = request.getParameter("name");
        String description = request.getParameter("description");
        String sortOrderStr = request.getParameter("sortOrder");

        int sortOrder = 0;
        if (sortOrderStr != null && !sortOrderStr.isEmpty()) {
            try {
                sortOrder = Integer.parseInt(sortOrderStr);
            } catch (NumberFormatException e) {
                // 排序值无效时使用默认值0
            }
        }

        String result = categoryService.add(name, description, sortOrder);

        if (result == null) {
            request.setAttribute("success", "分类添加成功");
        } else {
            request.setAttribute("error", result);
        }
        ensureCsrfToken(request);
        request.getRequestDispatcher("/WEB-INF/views/admin/category-form.jsp").forward(request, response);
    }

    /**
     * 更新分类
     */
    private void updateCategory(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String idStr = request.getParameter("id");
        String name = request.getParameter("name");
        String description = request.getParameter("description");
        String sortOrderStr = request.getParameter("sortOrder");

        try {
            int id = Integer.parseInt(idStr);
            Category category = new Category(name, description);
            category.setId(id);

            // 读取排序顺序
            if (sortOrderStr != null && !sortOrderStr.isEmpty()) {
                try {
                    category.setSortOrder(Integer.parseInt(sortOrderStr));
                } catch (NumberFormatException e) {
                    category.setSortOrder(0);
                }
            } else {
                category.setSortOrder(0);
            }

            String result = categoryService.update(category);

            if (result == null) {
                request.setAttribute("success", "分类更新成功");
            } else {
                request.setAttribute("error", result);
                request.setAttribute("category", category);
            }
        } catch (NumberFormatException e) {
            request.setAttribute("error", "参数错误");
        }

        ensureCsrfToken(request);
        request.getRequestDispatcher("/WEB-INF/views/admin/category-form.jsp").forward(request, response);
    }

    /**
     * 删除分类（Ajax接口）
     */
    private void deleteCategory(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        // 兼容两种参数名：id 和 categoryId
        String categoryIdStr = request.getParameter("id");
        if (categoryIdStr == null) {
            categoryIdStr = request.getParameter("categoryId");
        }

        // 参数校验：防止NPE
        if (categoryIdStr == null) {
            out.print(gson.toJson(Map.of("success", false, "message", "参数错误")));
            return;
        }

        try {
            int categoryId = Integer.parseInt(categoryIdStr);
            boolean success = categoryService.delete(categoryId);
            out.print(gson.toJson(Map.of("success", success)));
        } catch (NumberFormatException e) {
            out.print(gson.toJson(Map.of("success", false, "message", "参数错误")));
        }
    }

    /**
     * 获取整数参数
     */
    private int getIntParameter(HttpServletRequest request, String name, int defaultValue) {
        String value = request.getParameter(name);
        if (value != null && !value.isEmpty()) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * 确保后台页面都有同一个会话级Token，避免刷新页面后旧表单立即失效
     */
    private String ensureCsrfToken(HttpServletRequest request) {
        HttpSession session = request.getSession();
        String token = (String) session.getAttribute("csrfToken");
        if (token == null || token.isEmpty()) {
            token = StringUtil.generateToken();
            session.setAttribute("csrfToken", token);
        }
        request.setAttribute("csrfToken", token);
        return token;
    }

    /**
     * 后台所有写操作统一校验Token，避免每个分支遗漏安全边界
     */
    private boolean verifyCsrfToken(HttpServletRequest request) {
        String token = request.getParameter("csrfToken");
        HttpSession session = request.getSession(false);
        if (session == null || token == null || token.isEmpty()) {
            return false;
        }
        String sessionToken = (String) session.getAttribute("csrfToken");
        return sessionToken != null && sessionToken.equals(token);
    }

    /**
     * 表单和Ajax失败响应不同，避免前端把HTML错误页当JSON解析
     */
    private void handleInvalidCsrfToken(HttpServletRequest request, HttpServletResponse response, String pathInfo)
            throws ServletException, IOException {
        if ("/category/add".equals(pathInfo) || "/category/edit".equals(pathInfo)) {
            ensureCsrfToken(request);
            request.setAttribute("error", "CSRF Token验证失败，请刷新页面后重试");
            restoreCategoryForInvalidRequest(request);
            request.getRequestDispatcher("/WEB-INF/views/admin/category-form.jsp").forward(request, response);
            return;
        }

        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.print(gson.toJson(Map.of("success", false, "message", "CSRF Token验证失败，请刷新页面后重试")));
    }

    /**
     * 编辑分类提交失败时尽量保留原分类，减少用户重新定位的成本
     */
    private void restoreCategoryForInvalidRequest(HttpServletRequest request) {
        String idStr = request.getParameter("id");
        if (idStr == null || idStr.isEmpty()) {
            return;
        }
        try {
            Category category = categoryService.findById(Integer.parseInt(idStr));
            if (category != null) {
                request.setAttribute("category", category);
            }
        } catch (NumberFormatException ignored) {
            // 非法ID只展示Token错误，避免额外错误信息干扰用户判断
        }
    }
}
