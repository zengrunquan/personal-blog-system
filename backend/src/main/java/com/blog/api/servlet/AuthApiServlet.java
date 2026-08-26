package com.blog.api.servlet;

import com.blog.api.dto.UserDto;
import com.blog.api.exception.ApiException;
import com.blog.api.security.AuthenticatedSessionManager;
import com.blog.api.security.CsrfTokenManager;
import com.blog.api.support.DtoMapper;
import com.blog.entity.User;
import com.blog.service.UserService;
import com.blog.service.impl.UserServiceImpl;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@WebServlet("/api/auth/*")
public class AuthApiServlet extends BaseApiServlet {

    private final UserService userService;

    public AuthApiServlet() {
        this(new UserServiceImpl());
    }

    AuthApiServlet(UserService userService) {
        this.userService = Objects.requireNonNull(userService, "userService 不能为空");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        execute(request, response, () -> {
            switch (pathInfo(request)) {
                case "/session":
                    getSession(request, response);
                    break;
                case "/check-username":
                    checkUsername(request, response);
                    break;
                default:
                    throw notFound("认证接口不存在");
            }
        });
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        execute(request, response, () -> {
            switch (pathInfo(request)) {
                case "/login":
                    login(request, response);
                    break;
                case "/logout":
                    logout(request, response);
                    break;
                case "/register":
                    register(request, response);
                    break;
                default:
                    throw notFound("认证接口不存在");
            }
        });
    }

    private void getSession(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(true);
        Object loginUser = session.getAttribute("loginUser");
        JsonObject data = new JsonObject();
        data.addProperty("authenticated", loginUser instanceof User);
        // 会话接口必须稳定返回 user 字段；使用 JsonNull 只保留这一处契约，不污染统一响应的联合类型。
        data.add("user", loginUser instanceof User
                ? gson.toJsonTree(DtoMapper.toUserDto((User) loginUser))
                : JsonNull.INSTANCE);
        data.addProperty("csrfToken", CsrfTokenManager.getOrCreate(session));
        writeSuccess(response, data);
    }

    private void login(HttpServletRequest request, HttpServletResponse response) throws IOException {
        LoginRequest body = readJson(request, LoginRequest.class);
        if (body.username == null || body.username.isBlank()) throw validation("username", "请输入用户名");
        if (body.password == null || body.password.isBlank()) throw validation("password", "请输入密码");
        User user = userService.login(body.username.trim(), body.password);
        if (user == null) {
            throw new ApiException(HttpServletResponse.SC_UNAUTHORIZED,
                    "INVALID_CREDENTIALS", "用户名或密码错误，或账号已被禁用");
        }
        HttpSession session = AuthenticatedSessionManager.start(request, user);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("user", DtoMapper.toUserDto(user));
        data.put("csrfToken", CsrfTokenManager.getOrCreate(session));
        writeSuccess(response, data, "登录成功");
    }

    private void logout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        writeSuccess(response, Map.of("authenticated", false), "已安全退出");
    }

    private void register(HttpServletRequest request, HttpServletResponse response) throws IOException {
        RegisterRequest body = readJson(request, RegisterRequest.class);
        if (body.username == null || body.username.isBlank()) throw validation("username", "请输入用户名");
        if (body.password == null || body.password.length() < 6) throw validation("password", "密码至少 6 位");
        if (body.nickname == null || body.nickname.isBlank()) throw validation("nickname", "请输入昵称");
        if (body.email == null || !body.email.contains("@")) throw validation("email", "请输入有效邮箱");
        String result = userService.register(
                body.username.trim(), body.password, body.nickname.trim(), body.email.trim()
        );
        if (result != null) throw validation("username", result);
        writeCreated(response, Map.of("username", body.username.trim()), "注册成功，请登录");
    }

    private void checkUsername(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String username = request.getParameter("username");
        boolean available = username != null
                && !username.isBlank()
                && !userService.usernameExists(username);
        writeSuccess(response, Map.of("available", available));
    }

    private static final class LoginRequest {
        private String username;
        private String password;
    }

    private static final class RegisterRequest {
        private String username;
        private String password;
        private String nickname;
        private String email;
    }
}
