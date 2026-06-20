package com.blog.controller;

import com.blog.entity.User;
import com.blog.service.UserService;
import com.blog.service.impl.UserServiceImpl;
import com.blog.util.StringUtil;
import com.google.gson.Gson;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户控制器
 * 处理用户相关的请求：注册、登录、注销、个人信息管理等
 *
 * @author blog-system
 */
@WebServlet("/user/*")
@MultipartConfig(maxFileSize = 5 * 1024 * 1024) // 最大5MB
public class UserServlet extends HttpServlet {

    private final UserService userService = new UserServiceImpl();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null) {
            pathInfo = "/profile";
        }

        switch (pathInfo) {
            case "/profile":
                showProfile(request, response);
                break;
            case "/edit":
                showEditForm(request, response);
                break;
            case "/password":
                showPasswordForm(request, response);
                break;
            case "/logout":
                logout(request, response);
                break;
            case "/articles":
                showMyArticles(request, response);
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
            case "/register":
                register(request, response);
                break;
            case "/login":
                login(request, response);
                break;
            case "/update":
                updateProfile(request, response);
                break;
            case "/avatar":
                uploadAvatar(request, response);
                break;
            case "/password":
                changePassword(request, response);
                break;
            case "/checkUsername":
                checkUsername(request, response);
                break;
            default:
                response.sendRedirect(request.getContextPath() + "/index.jsp");
                break;
        }
    }

    /**
     * 用户注册
     */
    private void register(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // 获取CSRF Token并验证
        String token = request.getParameter("csrfToken");
        HttpSession session = request.getSession();
        String sessionToken = (String) session.getAttribute("csrfToken");

        if (sessionToken == null || !sessionToken.equals(token)) {
            request.setAttribute("error", "CSRF Token验证失败");
            request.getRequestDispatcher("/register.jsp").forward(request, response);
            return;
        }

        // 获取参数
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String nickname = request.getParameter("nickname");
        String email = request.getParameter("email");

        // 调用Service层注册
        String result = userService.register(username, password, nickname, email);

        if (result == null) {
            // 注册成功，跳转到登录页
            request.setAttribute("success", "注册成功，请登录");
            request.getRequestDispatcher("/login.jsp").forward(request, response);
        } else {
            // 注册失败，返回注册页并显示错误信息
            request.setAttribute("error", result);
            request.setAttribute("username", username);
            request.setAttribute("nickname", nickname);
            request.setAttribute("email", email);
            request.getRequestDispatcher("/register.jsp").forward(request, response);
        }
    }

    /**
     * 用户登录
     */
    private void login(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // 获取参数
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String remember = request.getParameter("remember");

        // 调用Service层登录
        User user = userService.login(username, password);

        if (user != null) {
            // 登录成功
            HttpSession session = request.getSession();
            session.setAttribute("loginUser", user);

            // 设置"记住我"Cookie
            if ("on".equals(remember)) {
                Cookie usernameCookie = new Cookie("rememberedUser", username);
                usernameCookie.setMaxAge(7 * 24 * 60 * 60); // 7天
                usernameCookie.setPath(request.getContextPath());
                response.addCookie(usernameCookie);
            }

            // 获取登录前的URL，如果有则跳转回去
            String redirectUrl = (String) session.getAttribute("redirectUrl");
            session.removeAttribute("redirectUrl");

            if (redirectUrl != null && !redirectUrl.isEmpty()) {
                response.sendRedirect(redirectUrl);
            } else {
                // 根据角色跳转到不同页面
                if (user.isAdmin()) {
                    response.sendRedirect(request.getContextPath() + "/admin/dashboard");
                } else {
                    response.sendRedirect(request.getContextPath() + "/index.jsp");
                }
            }
        } else {
            // 登录失败
            request.setAttribute("error", "用户名或密码错误，或账号已被禁用");
            request.setAttribute("username", username);
            request.getRequestDispatcher("/login.jsp").forward(request, response);
        }
    }

    /**
     * 用户注销
     */
    private void logout(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        response.sendRedirect(request.getContextPath() + "/index.jsp");
    }

    /**
     * 显示个人信息页面
     */
    private void showProfile(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("loginUser");

        // 防止NPE：如果用户未登录，跳转到登录页
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        // 重新从数据库获取最新信息
        user = userService.findById(user.getId());
        session.setAttribute("loginUser", user);

        request.getRequestDispatcher("/WEB-INF/views/user/profile.jsp").forward(request, response);
    }

    /**
     * 显示编辑个人信息表单
     */
    private void showEditForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/views/user/edit-profile.jsp").forward(request, response);
    }

    /**
     * 更新个人信息
     */
    private void updateProfile(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        User loginUser = (User) session.getAttribute("loginUser");

        // 防止NPE：如果用户未登录，跳转到登录页
        if (loginUser == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        String nickname = request.getParameter("nickname");
        String email = request.getParameter("email");

        // 参数验证
        if (nickname == null || nickname.trim().isEmpty()) {
            request.setAttribute("error", "昵称不能为空");
            request.getRequestDispatcher("/WEB-INF/views/user/edit-profile.jsp").forward(request, response);
            return;
        }

        User user = new User();
        user.setId(loginUser.getId());
        user.setNickname(StringUtil.escapeHtml(nickname));
        user.setEmail(email);

        boolean success = userService.updateProfile(user);
        if (success) {
            // 更新Session中的用户信息
            loginUser.setNickname(user.getNickname());
            loginUser.setEmail(user.getEmail());
            session.setAttribute("loginUser", loginUser);

            request.setAttribute("success", "个人信息更新成功");
        } else {
            request.setAttribute("error", "更新失败，请稍后重试");
        }
        request.getRequestDispatcher("/WEB-INF/views/user/edit-profile.jsp").forward(request, response);
    }

    /**
     * 上传头像
     */
    private void uploadAvatar(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        User loginUser = (User) session.getAttribute("loginUser");

        // 防止NPE：如果用户未登录，返回错误
        if (loginUser == null) {
            response.setContentType("application/json;charset=UTF-8");
            PrintWriter out = response.getWriter();
            out.print(gson.toJson(Map.of("success", false, "message", "用户未登录")));
            return;
        }

        // 获取上传的文件
        Part filePart = request.getPart("avatar");
        if (filePart == null || filePart.getSize() == 0) {
            response.setContentType("application/json;charset=UTF-8");
            PrintWriter out = response.getWriter();
            out.print(gson.toJson(Map.of("success", false, "message", "请选择要上传的头像")));
            return;
        }

        // 验证文件类型
        String contentType = filePart.getContentType();
        if (!contentType.equals("image/jpeg") && !contentType.equals("image/png") && !contentType.equals("image/gif")) {
            response.setContentType("application/json;charset=UTF-8");
            PrintWriter out = response.getWriter();
            out.print(gson.toJson(Map.of("success", false, "message", "只支持jpg、png、gif格式的图片")));
            return;
        }

        // 生成文件名
        String fileName = "avatar_" + loginUser.getId() + "_" + System.currentTimeMillis() +
                          getExtension(filePart.getSubmittedFileName());

        // 保存文件
        String uploadPath = getServletContext().getRealPath("/uploads/avatars");
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        String filePath = uploadPath + File.separator + fileName;
        filePart.write(filePath);

        // 更新数据库
        String avatarUrl = request.getContextPath() + "/uploads/avatars/" + fileName;
        boolean success = userService.updateAvatar(loginUser.getId(), avatarUrl);

        if (success) {
            loginUser.setAvatar(avatarUrl);
            session.setAttribute("loginUser", loginUser);
            response.setContentType("application/json;charset=UTF-8");
            PrintWriter out = response.getWriter();
            out.print(gson.toJson(Map.of("success", true, "avatar", avatarUrl)));
        } else {
            response.setContentType("application/json;charset=UTF-8");
            PrintWriter out = response.getWriter();
            out.print(gson.toJson(Map.of("success", false, "message", "上传失败")));
        }
    }

    /**
     * 显示修改密码表单
     */
    private void showPasswordForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/views/user/change-password.jsp").forward(request, response);
    }

    /**
     * 修改密码
     */
    private void changePassword(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        User loginUser = (User) session.getAttribute("loginUser");

        // 防止NPE：如果用户未登录，跳转到登录页
        if (loginUser == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        String oldPassword = request.getParameter("oldPassword");
        String newPassword = request.getParameter("newPassword");
        String confirmPassword = request.getParameter("confirmPassword");

        // 验证新密码
        if (!newPassword.equals(confirmPassword)) {
            request.setAttribute("error", "两次输入的新密码不一致");
            request.getRequestDispatcher("/WEB-INF/views/user/change-password.jsp").forward(request, response);
            return;
        }

        String result = userService.changePassword(loginUser.getId(), oldPassword, newPassword);

        if (result == null) {
            request.setAttribute("success", "密码修改成功，请重新登录");
            // 注销用户，要求重新登录
            session.invalidate();
            request.getRequestDispatcher("/login.jsp").forward(request, response);
        } else {
            request.setAttribute("error", result);
            request.getRequestDispatcher("/WEB-INF/views/user/change-password.jsp").forward(request, response);
        }
    }

    /**
     * 检查用户名是否可用（Ajax接口）
     */
    private void checkUsername(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String username = request.getParameter("username");
        // 通过DAO层检查用户名是否已存在
        boolean available = !new com.blog.dao.impl.UserDaoImpl().existsByUsername(username);

        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.print(gson.toJson(Map.of("available", available)));
    }

    /**
     * 显示我的文章列表
     */
    private void showMyArticles(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        User loginUser = (User) session.getAttribute("loginUser");

        // 防止NPE：如果用户未登录，跳转到登录页
        if (loginUser == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        // 获取分页参数
        int page = 1;
        int pageSize = 10;
        try {
            String pageStr = request.getParameter("page");
            if (pageStr != null && !pageStr.isEmpty()) {
                page = Integer.parseInt(pageStr);
            }
        } catch (NumberFormatException e) {
            page = 1;
        }

        // 查询数据
        com.blog.service.ArticleService articleService = new com.blog.service.impl.ArticleServiceImpl();
        int totalCount = articleService.getCountByUserId(loginUser.getId());
        int totalPages = (totalCount + pageSize - 1) / pageSize;
        var articles = articleService.findByUserId(loginUser.getId(), page, pageSize);

        // 设置属性
        request.setAttribute("articles", articles);
        request.setAttribute("currentPage", page);
        request.setAttribute("totalPages", totalPages);
        request.setAttribute("totalCount", totalCount);

        request.getRequestDispatcher("/WEB-INF/views/user/my-articles.jsp").forward(request, response);
    }

    /**
     * 获取文件扩展名
     */
    private String getExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot >= 0) {
            return fileName.substring(lastDot);
        }
        return "";
    }
}
