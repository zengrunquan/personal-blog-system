package com.blog.controller;

import com.blog.entity.Article;
import com.blog.entity.Category;
import com.blog.entity.User;
import com.blog.service.ArticleService;
import com.blog.service.CategoryService;
import com.blog.service.impl.ArticleServiceImpl;
import com.blog.service.impl.CategoryServiceImpl;
import com.blog.util.PageUtil;
import com.blog.util.StringUtil;
import com.google.gson.Gson;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

/**
 * 文章控制器
 * 处理文章相关的请求：发布、编辑、删除、列表、详情等
 *
 * @author blog-system
 */
@WebServlet("/article/*")
@MultipartConfig(maxFileSize = 10 * 1024 * 1024) // 最大10MB
public class ArticleServlet extends HttpServlet {

    private final ArticleService articleService = new ArticleServiceImpl();
    private final CategoryService categoryService = new CategoryServiceImpl();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null) {
            pathInfo = "/list";
        }

        switch (pathInfo) {
            case "/list":
                listArticles(request, response);
                break;
            case "/detail":
                showDetail(request, response);
                break;
            case "/add":
                showAddForm(request, response);
                break;
            case "/edit":
                showEditForm(request, response);
                break;
            case "/category":
                listByCategory(request, response);
                break;
            case "/search":
                searchArticles(request, response);
                break;
            case "/export":
                exportArticles(request, response);
                break;
            default:
                response.sendRedirect(request.getContextPath() + "/index.jsp");
                break;
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null) {
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return;
        }

        switch (pathInfo) {
            case "/add":
                addArticle(request, response);
                break;
            case "/edit":
                updateArticle(request, response);
                break;
            case "/delete":
                deleteArticle(request, response);
                break;
            case "/uploadCover":
                uploadCover(request, response);
                break;
            case "/uploadFile":
                uploadFile(request, response);
                break;
            case "/batchUpload":
                batchUpload(request, response);
                break;
            default:
                response.sendRedirect(request.getContextPath() + "/index.jsp");
                break;
        }
    }

    /**
     * 文章列表页面
     */
    private void listArticles(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // 获取分页参数
        int page = getIntParameter(request, "page", 1);
        int pageSize = 10;

        // 查询数据
        List<Article> articles = articleService.findPublished(page, pageSize);
        int totalCount = articleService.getPublishedTotalCount();
        PageUtil pageUtil = new PageUtil(page, pageSize, totalCount);

        // 获取分类列表（用于侧边栏）
        List<Category> categories = categoryService.findAllWithArticleCount();

        // 设置属性
        request.setAttribute("articles", articles);
        request.setAttribute("pageUtil", pageUtil);
        request.setAttribute("categories", categories);

        request.getRequestDispatcher("/WEB-INF/views/article/list.jsp").forward(request, response);
    }

    /**
     * 文章详情页面
     */
    private void showDetail(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String idStr = request.getParameter("id");
        if (idStr == null || idStr.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/article/list");
            return;
        }

        try {
            int id = Integer.parseInt(idStr);
            Article article = articleService.findById(id);

            if (article == null) {
                request.setAttribute("error", "文章不存在");
                request.getRequestDispatcher("/WEB-INF/views/error/404.jsp").forward(request, response);
                return;
            }

            // 增加浏览次数
            articleService.incrementViewCount(id);

            // 获取评论列表
            com.blog.service.CommentService commentService = new com.blog.service.impl.CommentServiceImpl();
            var comments = commentService.findByArticleId(id);

            // 获取分类列表（用于侧边栏）
            List<Category> categories = categoryService.findAllWithArticleCount();

            request.setAttribute("article", article);
            request.setAttribute("comments", comments);
            request.setAttribute("categories", categories);

            request.getRequestDispatcher("/WEB-INF/views/article/detail.jsp").forward(request, response);
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/article/list");
        }
    }

    /**
     * 显示发布文章表单
     */
    private void showAddForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // 获取分类列表
        List<Category> categories = categoryService.findAll();
        request.setAttribute("categories", categories);

        // 生成CSRF Token
        HttpSession session = request.getSession();
        String token = StringUtil.generateToken();
        session.setAttribute("csrfToken", token);
        request.setAttribute("csrfToken", token);

        request.getRequestDispatcher("/WEB-INF/views/article/add.jsp").forward(request, response);
    }

    /**
     * 发布文章
     */
    private void addArticle(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // 验证CSRF Token
        if (!verifyCsrfToken(request)) {
            request.setAttribute("error", "CSRF Token验证失败");
            showAddForm(request, response);
            return;
        }

        HttpSession session = request.getSession();
        User loginUser = (User) session.getAttribute("loginUser");

        // 获取参数
        String title = request.getParameter("title");
        String content = request.getParameter("content");
        String summary = request.getParameter("summary");
        String categoryIdStr = request.getParameter("categoryId");
        String statusStr = request.getParameter("status");

        // 参数验证
        if (title == null || title.trim().isEmpty()) {
            request.setAttribute("error", "文章标题不能为空");
            request.setAttribute("title", title);
            request.setAttribute("content", content);
            request.setAttribute("summary", summary);
            showAddForm(request, response);
            return;
        }

        // 创建文章对象
        Article article = new Article();
        article.setTitle(StringUtil.escapeHtml(title));
        article.setContent(content); // 内容允许HTML
        article.setSummary(summary != null ? StringUtil.escapeHtml(summary) : null);
        article.setUserId(loginUser.getId());

        try {
            article.setCategoryId(Integer.parseInt(categoryIdStr));
        } catch (NumberFormatException e) {
            request.setAttribute("error", "请选择有效的分类");
            showAddForm(request, response);
            return;
        }

        article.setStatus(statusStr != null ? Integer.parseInt(statusStr) : 1);

        // 处理封面图片上传
        Part coverPart = request.getPart("coverImage");
        if (coverPart != null && coverPart.getSize() > 0) {
            String coverUrl = saveUploadedFile(coverPart, "covers", request);
            article.setCoverImage(coverUrl);
        }

        // 调用Service层发布文章
        String result = articleService.publish(article);

        if (result == null) {
            // 发布成功，跳转到文章列表
            response.sendRedirect(request.getContextPath() + "/user/articles");
        } else {
            request.setAttribute("error", result);
            request.setAttribute("article", article);
            showAddForm(request, response);
        }
    }

    /**
     * 显示编辑文章表单
     */
    private void showEditForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String idStr = request.getParameter("id");
        if (idStr == null) {
            response.sendRedirect(request.getContextPath() + "/user/articles");
            return;
        }

        try {
            int id = Integer.parseInt(idStr);
            Article article = articleService.findById(id);

            if (article == null) {
                request.setAttribute("error", "文章不存在");
                request.getRequestDispatcher("/WEB-INF/views/error/404.jsp").forward(request, response);
                return;
            }

            // 验证权限：只有作者可以编辑
            HttpSession session = request.getSession();
            User loginUser = (User) session.getAttribute("loginUser");
            if (!article.getUserId().equals(loginUser.getId()) && !loginUser.isAdmin()) {
                response.sendRedirect(request.getContextPath() + "/user/articles");
                return;
            }

            // 获取分类列表
            List<Category> categories = categoryService.findAll();

            request.setAttribute("article", article);
            request.setAttribute("categories", categories);

            // 生成CSRF Token
            String token = StringUtil.generateToken();
            session.setAttribute("csrfToken", token);
            request.setAttribute("csrfToken", token);

            request.getRequestDispatcher("/WEB-INF/views/article/edit.jsp").forward(request, response);
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/user/articles");
        }
    }

    /**
     * 更新文章
     */
    private void updateArticle(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // 验证CSRF Token
        if (!verifyCsrfToken(request)) {
            response.sendRedirect(request.getContextPath() + "/user/articles");
            return;
        }

        // 获取参数
        String idStr = request.getParameter("id");
        String title = request.getParameter("title");
        String content = request.getParameter("content");
        String summary = request.getParameter("summary");
        String categoryIdStr = request.getParameter("categoryId");
        String statusStr = request.getParameter("status");

        try {
            int id = Integer.parseInt(idStr);

            // 获取原文章信息
            Article article = articleService.findById(id);
            if (article == null) {
                response.sendRedirect(request.getContextPath() + "/user/articles");
                return;
            }

            // 验证权限
            HttpSession session = request.getSession();
            User loginUser = (User) session.getAttribute("loginUser");
            if (!article.getUserId().equals(loginUser.getId()) && !loginUser.isAdmin()) {
                response.sendRedirect(request.getContextPath() + "/user/articles");
                return;
            }

            // 更新文章信息
            article.setTitle(StringUtil.escapeHtml(title));
            article.setContent(content);
            article.setSummary(summary != null ? StringUtil.escapeHtml(summary) : null);
            article.setCategoryId(Integer.parseInt(categoryIdStr));
            article.setStatus(Integer.parseInt(statusStr));

            // 处理封面图片上传
            Part coverPart = request.getPart("coverImage");
            if (coverPart != null && coverPart.getSize() > 0) {
                String coverUrl = saveUploadedFile(coverPart, "covers", request);
                article.setCoverImage(coverUrl);
            }

            // 调用Service层更新
            String result = articleService.update(article);

            if (result == null) {
                response.sendRedirect(request.getContextPath() + "/user/articles");
            } else {
                request.setAttribute("error", result);
                request.setAttribute("article", article);
                showEditForm(request, response);
            }
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/user/articles");
        }
    }

    /**
     * 删除文章（Ajax接口）
     */
    private void deleteArticle(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String idStr = request.getParameter("id");

        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        try {
            int id = Integer.parseInt(idStr);

            // 验证权限
            HttpSession session = request.getSession();
            User loginUser = (User) session.getAttribute("loginUser");
            Article article = articleService.findById(id);

            if (article == null) {
                out.print(gson.toJson(Map.of("success", false, "message", "文章不存在")));
                return;
            }

            if (!article.getUserId().equals(loginUser.getId()) && !loginUser.isAdmin()) {
                out.print(gson.toJson(Map.of("success", false, "message", "没有删除权限")));
                return;
            }

            boolean success = articleService.delete(id);
            out.print(gson.toJson(Map.of("success", success)));
        } catch (NumberFormatException e) {
            out.print(gson.toJson(Map.of("success", false, "message", "参数错误")));
        }
    }

    /**
     * 按分类查看文章
     */
    private void listByCategory(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String categoryIdStr = request.getParameter("id");
        int page = getIntParameter(request, "page", 1);
        int pageSize = 10;

        try {
            int categoryId = Integer.parseInt(categoryIdStr);
            Category category = categoryService.findById(categoryId);

            if (category == null) {
                response.sendRedirect(request.getContextPath() + "/article/list");
                return;
            }

            List<Article> articles = articleService.findByCategory(categoryId, page, pageSize);
            int totalCount = articleService.getCountByCategory(categoryId);
            PageUtil pageUtil = new PageUtil(page, pageSize, totalCount);

            List<Category> categories = categoryService.findAllWithArticleCount();

            request.setAttribute("articles", articles);
            request.setAttribute("pageUtil", pageUtil);
            request.setAttribute("categories", categories);
            request.setAttribute("currentCategory", category);
            request.setAttribute("categoryId", categoryId);

            request.getRequestDispatcher("/WEB-INF/views/article/list.jsp").forward(request, response);
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/article/list");
        }
    }

    /**
     * 搜索文章
     */
    private void searchArticles(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String keyword = request.getParameter("keyword");
        int page = getIntParameter(request, "page", 1);
        int pageSize = 10;

        List<Article> articles = articleService.search(keyword, page, pageSize);
        int totalCount = articleService.getSearchTotalCount(keyword);
        PageUtil pageUtil = new PageUtil(page, pageSize, totalCount);

        List<Category> categories = categoryService.findAllWithArticleCount();

        request.setAttribute("articles", articles);
        request.setAttribute("pageUtil", pageUtil);
        request.setAttribute("categories", categories);
        // 对搜索关键词进行HTML转义，防止XSS攻击
        request.setAttribute("keyword", StringUtil.escapeHtml(keyword));

        request.getRequestDispatcher("/WEB-INF/views/article/list.jsp").forward(request, response);
    }

    /**
     * 上传封面图片（Ajax接口）
     */
    private void uploadCover(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        Part filePart = request.getPart("cover");
        if (filePart == null || filePart.getSize() == 0) {
            out.print(gson.toJson(Map.of("success", false, "message", "请选择图片")));
            return;
        }

        // 验证文件类型
        String contentType = filePart.getContentType();
        if (!contentType.startsWith("image/")) {
            out.print(gson.toJson(Map.of("success", false, "message", "只能上传图片文件")));
            return;
        }

        String coverUrl = saveUploadedFile(filePart, "covers", request);
        out.print(gson.toJson(Map.of("success", true, "url", coverUrl)));
    }

    /**
     * 上传附件（单文件）
     */
    private void uploadFile(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        Part filePart = request.getPart("file");
        if (filePart == null || filePart.getSize() == 0) {
            out.print(gson.toJson(Map.of("success", false, "message", "请选择文件")));
            return;
        }

        // 限制文件大小（10MB）
        if (filePart.getSize() > 10 * 1024 * 1024) {
            out.print(gson.toJson(Map.of("success", false, "message", "文件大小不能超过10MB")));
            return;
        }

        String fileUrl = saveUploadedFile(filePart, "files", request);
        String fileName = filePart.getSubmittedFileName();
        out.print(gson.toJson(Map.of("success", true, "url", fileUrl, "name", fileName)));
    }

    /**
     * 批量上传文件
     */
    private void batchUpload(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        // 获取所有上传的文件
        java.util.Collection<Part> parts = request.getParts();
        java.util.List<String> uploadedFiles = new java.util.ArrayList<>();

        for (Part part : parts) {
            if (part.getName().equals("files") && part.getSize() > 0) {
                String fileUrl = saveUploadedFile(part, "files", request);
                uploadedFiles.add(fileUrl);
            }
        }

        out.print(gson.toJson(Map.of("success", true, "files", uploadedFiles, "count", uploadedFiles.size())));
    }

    /**
     * 导出文章列表为CSV
     */
    private void exportArticles(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        // 获取所有文章
        List<Article> articles = articleService.findAll();

        // 设置响应头
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=articles.csv");

        // 添加BOM头，解决Excel打开中文乱码问题
        response.getOutputStream().write(0xEF);
        response.getOutputStream().write(0xBB);
        response.getOutputStream().write(0xBF);

        PrintWriter out = response.getWriter();

        // 写入CSV头
        out.println("ID,标题,作者,分类,浏览次数,状态,创建时间");

        // 写入数据
        for (Article article : articles) {
            // 防止NPE，对可能为null的字段进行处理
            String title = article.getTitle() != null ? article.getTitle().replace("\"", "\"\"") : "";
            String authorNickname = article.getAuthorNickname() != null ? article.getAuthorNickname() : "";
            String categoryName = article.getCategoryName() != null ? article.getCategoryName() : "";

            out.println(String.format("%d,\"%s\",\"%s\",\"%s\",%d,\"%s\",\"%s\"",
                    article.getId(),
                    title,
                    authorNickname,
                    categoryName,
                    article.getViewCount(),
                    article.getStatus() == 1 ? "已发布" : "草稿",
                    article.getCreateTime()));
        }

        out.flush();
    }

    /**
     * 保存上传的文件
     */
    private String saveUploadedFile(Part filePart, String subDir, HttpServletRequest request)
            throws IOException {
        // 生成唯一文件名
        String originalName = filePart.getSubmittedFileName();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }
        String fileName = System.currentTimeMillis() + "_" + (int)(Math.random() * 10000) + extension;

        // 确定保存路径
        String uploadPath = getServletContext().getRealPath("/uploads/" + subDir);
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        // 保存文件
        String filePath = uploadPath + File.separator + fileName;
        filePart.write(filePath);

        // 返回访问URL
        return request.getContextPath() + "/uploads/" + subDir + "/" + fileName;
    }

    /**
     * 验证CSRF Token
     */
    private boolean verifyCsrfToken(HttpServletRequest request) {
        String token = request.getParameter("csrfToken");
        HttpSession session = request.getSession();
        String sessionToken = (String) session.getAttribute("csrfToken");
        return sessionToken != null && sessionToken.equals(token);
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
}
