package com.blog.api.servlet;

import com.blog.api.support.DtoMapper;
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
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet("/api/home")
public class HomeApiServlet extends BaseApiServlet {

    private final ArticleService articleService = new ArticleServiceImpl();
    private final CategoryService categoryService = new CategoryServiceImpl();
    private final UserService userService = new UserServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        execute(request, response, () -> {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("featuredArticles", DtoMapper.toArticleDtos(articleService.findPublished(1, 6)));
            data.put("categories", DtoMapper.toCategoryDtos(categoryService.findAllWithArticleCount()));
            data.put("stats", Map.of(
                    "articles", articleService.getPublishedTotalCount(),
                    "categories", categoryService.getTotalCount(),
                    "authors", userService.getTotalCount()
            ));
            writeSuccess(response, data);
        });
    }
}
