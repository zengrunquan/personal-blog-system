package com.blog.controller;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HomePageCategoryUiTest {

    private static final Path HOME_VIEW = Paths.get("src/main/webapp/index.jsp");

    @Test
    public void homePageShouldRenderCategoryCountsFromControllerData() throws Exception {
        String home = Files.readString(HOME_VIEW);

        assertTrue(home.contains("<c:forEach items=\"${categories}\" var=\"category\">"));
        assertTrue(home.contains("${category.articleCount}"));
        assertFalse(home.contains("loadCategories()"));
    }
}
