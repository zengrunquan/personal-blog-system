package com.blog.controller;

import com.blog.entity.Category;
import com.blog.service.CategoryService;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class HomeServletTest {

    @Test
    public void homePageShouldExposeCategoriesWithRealArticleCounts() {
        Category technology = category(1, "技术分享", 3);
        Category life = category(2, "生活随笔", 2);
        List<Category> categories = List.of(technology, life);

        CategoryService categoryService = (CategoryService) Proxy.newProxyInstance(
                CategoryService.class.getClassLoader(),
                new Class<?>[]{CategoryService.class},
                (proxy, method, args) -> {
                    if ("findAllWithArticleCount".equals(method.getName())) {
                        return categories;
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );

        Map<String, Object> attributes = new HashMap<>();
        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, method, args) -> {
                    if ("setAttribute".equals(method.getName())) {
                        attributes.put((String) args[0], args[1]);
                        return null;
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );

        HomeServlet servlet = new HomeServlet(categoryService);
        servlet.loadHomeData(request);

        assertSame(categories, attributes.get("categories"));
        assertEquals(3, ((Category) categories.get(0)).getArticleCount().intValue());
        assertEquals(2, ((Category) categories.get(1)).getArticleCount().intValue());
    }

    private static Category category(int id, String name, int articleCount) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setArticleCount(articleCount);
        return category;
    }
}
