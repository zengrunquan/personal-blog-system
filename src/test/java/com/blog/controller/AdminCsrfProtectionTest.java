package com.blog.controller;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class AdminCsrfProtectionTest {

    private static final Path ADMIN_SERVLET =
            Paths.get("src/main/java/com/blog/controller/AdminServlet.java");
    private static final Path ADMIN_VIEW_DIR =
            Paths.get("src/main/webapp/WEB-INF/views/admin");

    @Test
    public void adminServletShouldValidateCsrfBeforeDispatchingPostActions() throws Exception {
        String source = Files.readString(ADMIN_SERVLET);
        int doPostIndex = source.indexOf("protected void doPost");
        int switchIndex = source.indexOf("switch (pathInfo)", doPostIndex);
        int verifyIndex = source.indexOf("verifyCsrfToken(request)", doPostIndex);

        assertTrue("AdminServlet#doPost 必须在分发危险操作前校验 CSRF Token",
                doPostIndex >= 0 && verifyIndex > doPostIndex && verifyIndex < switchIndex);
    }

    @Test
    public void adminPagesShouldSubmitCsrfTokenWithFormsAndAjax() throws Exception {
        String categoryForm = readAdminView("category-form.jsp");
        String userList = readAdminView("user-list.jsp");
        String articleList = readAdminView("article-list.jsp");
        String categoryList = readAdminView("category-list.jsp");

        assertTrue("分类表单必须提交 CSRF Token", categoryForm.contains("name=\"csrfToken\""));
        assertTrue("用户状态 Ajax 必须携带 CSRF Token", userList.contains("csrfToken="));
        assertTrue("文章删除 Ajax 必须携带 CSRF Token", articleList.contains("csrfToken="));
        assertTrue("分类删除 Ajax 必须携带 CSRF Token", categoryList.contains("csrfToken="));
    }

    @Test
    public void adminArticlePageShouldCallAdminArticleEndpoints() throws Exception {
        String articleList = readAdminView("article-list.jsp");

        assertTrue("后台单篇文章删除必须调用 AdminServlet 端点",
                articleList.contains("/admin/article/delete"));
        assertTrue("后台批量删除必须调用 AdminServlet 端点",
                articleList.contains("/admin/article/batchDelete"));
    }

    private String readAdminView(String fileName) throws Exception {
        return Files.readString(ADMIN_VIEW_DIR.resolve(fileName));
    }
}
