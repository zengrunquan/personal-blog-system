package com.blog.api.security;

import com.blog.api.response.ApiResponse;
import com.blog.entity.User;
import com.google.gson.Gson;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebFilter(urlPatterns = "/api/*", asyncSupported = true)
public class ApiSecurityFilter implements Filter {

    private final Gson gson = new Gson();

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String path = getApplicationPath(request);
        String method = request.getMethod();
        User loginUser = getLoginUser(request);

        if (requiresAuthentication(path, method) && loginUser == null) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "AUTH_REQUIRED", "登录状态已失效，请重新登录");
            return;
        }

        if (path.startsWith("/api/admin/") || path.equals("/api/admin")) {
            if (loginUser == null || !loginUser.isAdmin()) {
                writeError(response, HttpServletResponse.SC_FORBIDDEN,
                        "ADMIN_REQUIRED", "当前账号没有管理员权限");
                return;
            }
        }

        if (isWriteMethod(method) && !CsrfTokenManager.isValid(request)) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN,
                    "CSRF_INVALID", "安全令牌已失效，请刷新页面后重试");
            return;
        }

        chain.doFilter(request, response);
    }

    private String getApplicationPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        return contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)
                ? uri.substring(contextPath.length())
                : uri;
    }

    private User getLoginUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return null;
        Object user = session.getAttribute("loginUser");
        return user instanceof User ? (User) user : null;
    }

    private boolean requiresAuthentication(String path, String method) {
        if (path.equals("/api/auth/logout")) return true;
        if (path.equals("/api/files") || path.startsWith("/api/files/")) return true;
        if (path.equals("/api/me") || path.startsWith("/api/me/")) return true;
        if (path.equals("/api/uploads") || path.startsWith("/api/uploads/")) return true;
        if (path.equals("/api/admin") || path.startsWith("/api/admin/")) return true;
        if (path.startsWith("/api/comments/")) return true;
        return path.startsWith("/api/articles") && isWriteMethod(method);
    }

    private boolean isWriteMethod(String method) {
        return "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method);
    }

    private void writeError(HttpServletResponse response, int status, String code, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(gson.toJson(ApiResponse.error(code, message)));
        response.getWriter().flush();
    }

    @Override
    public void destroy() {
    }
}
