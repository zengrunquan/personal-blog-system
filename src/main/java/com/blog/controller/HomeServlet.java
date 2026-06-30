package com.blog.controller;

import com.blog.service.CategoryService;
import com.blog.service.impl.CategoryServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;

/**
 * 首页控制器。
 *
 * @author blog-system
 */
@WebServlet("/home")
public class HomeServlet extends HttpServlet {

    private static final Logger LOGGER = LogManager.getLogger(HomeServlet.class);

    private final CategoryService categoryService;

    public HomeServlet() {
        this(new CategoryServiceImpl());
    }

    HomeServlet(CategoryService categoryService) {
        this.categoryService = Objects.requireNonNull(categoryService, "categoryService 不能为空");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            loadHomeData(request);
            request.getRequestDispatcher("/index.jsp").forward(request, response);
        } catch (RuntimeException e) {
            // 首页属于公共入口，查询失败时记录完整上下文，避免静默显示错误的固定数量。
            LOGGER.error("[HomeServlet#doGet] 加载首页分类统计失败", e);
            throw new ServletException("加载首页分类统计失败，请稍后重试", e);
        }
    }

    void loadHomeData(HttpServletRequest request) {
        request.setAttribute("categories", categoryService.findAllWithArticleCount());
    }
}
