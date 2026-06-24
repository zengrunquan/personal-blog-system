package com.blog.controller;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class AdminUserManagementUiTest {

    private static final Path USER_LIST_VIEW =
            Paths.get("src/main/webapp/WEB-INF/views/admin/user-list.jsp");

    @Test
    public void userListShouldExposeDeleteUserButtonForOtherUsers() throws Exception {
        String userList = Files.readString(USER_LIST_VIEW);

        assertTrue("用户列表必须提供删除用户按钮", userList.contains(">删除</button>"));
        assertTrue("删除按钮必须调用 deleteUser 并传入当前行用户ID",
                userList.contains("onclick=\"deleteUser(${user.id})\""));
    }

    @Test
    public void deleteUserButtonShouldCallAdminDeleteEndpointWithCsrfToken() throws Exception {
        String userList = Files.readString(USER_LIST_VIEW);

        assertTrue("用户列表必须定义 deleteUser(userId) 前端函数",
                userList.contains("function deleteUser(userId)"));
        assertTrue("删除用户 Ajax 必须调用 AdminServlet 端点",
                userList.contains("/admin/user/delete"));
        assertTrue("删除用户 Ajax 必须提交 userId 参数",
                userList.contains("userId="));
        assertTrue("删除用户 Ajax 必须提交 CSRF Token",
                userList.contains("csrfToken="));
    }
}
