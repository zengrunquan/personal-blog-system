package com.blog.web;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@WebFilter("/*")
public class SpaRoutingFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String path = applicationPath(request);
        String decodedPath = decodedApplicationPath(request, path);

        if (!"GET".equalsIgnoreCase(request.getMethod())
                && FrontendRoutePolicy.isLegacyWriteRoute(path)) {
            response.setStatus(HttpServletResponse.SC_GONE);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"error\":{\"code\":"
                    + "\"LEGACY_ENDPOINT_DISABLED\",\"message\":\"旧页面写接口已停用，请使用新版页面\"}}");
            return;
        }

        if ("GET".equalsIgnoreCase(request.getMethod())
                && FrontendRoutePolicy.isLegacyUnsafeGet(path)) {
            response.setStatus(HttpServletResponse.SC_GONE);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"error\":{\"code\":"
                    + "\"LEGACY_GET_DISABLED\",\"message\":\"旧注销链接已停用，请使用新版页面\"}}");
            return;
        }

        if ("GET".equalsIgnoreCase(request.getMethod())
                && "/article/download".equals(path)
                && !isSafeLegacyFileName(request.getParameter("file"))) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "非法附件文件名");
            return;
        }

        if (FrontendRoutePolicy.isLegacyPublicAttachmentNamespace(path)
                || FrontendRoutePolicy.isLegacyPublicAttachmentNamespace(decodedPath)) {
            if ("GET".equalsIgnoreCase(request.getMethod())
                    && FrontendRoutePolicy.isLegacyPublicAttachment(decodedPath)) {
                String fileName = decodedPath.substring("/uploads/files/".length());
                response.sendRedirect(request.getContextPath() + "/api/files/" + fileName + "/download");
                return;
            }
            // 旧附件目录不能再回落到默认静态资源 Servlet，否则编码或矩阵参数可绕过鉴权下载。
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if ("GET".equalsIgnoreCase(request.getMethod()) && FrontendRoutePolicy.isLegacyRoute(path)) {
            response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
            response.setHeader("Location", request.getContextPath() + legacyTarget(path, request));
            return;
        }

        if ("GET".equalsIgnoreCase(request.getMethod()) && FrontendRoutePolicy.isSpaRoute(path)) {
            request.getRequestDispatcher("/index.html").forward(request, response);
            return;
        }

        chain.doFilter(request, response);
    }

    private String applicationPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String context = request.getContextPath();
        return context != null && !context.isEmpty() && uri.startsWith(context)
                ? uri.substring(context.length())
                : uri;
    }

    private String decodedApplicationPath(HttpServletRequest request, String fallback) {
        String servletPath = request.getServletPath();
        String pathInfo = request.getPathInfo();
        if ((servletPath == null || servletPath.isEmpty()) && pathInfo == null) return fallback;
        return (servletPath == null ? "" : servletPath) + (pathInfo == null ? "" : pathInfo);
    }

    private String legacyTarget(String path, HttpServletRequest request) {
        switch (path) {
            case "/home":
            case "/index.jsp":
                return "/";
            case "/login.jsp":
                return "/login";
            case "/register.jsp":
                return "/register";
            case "/article/list":
                return "/articles";
            case "/article/detail":
                return "/articles/" + safeId(request.getParameter("id"), "1");
            case "/article/category":
                return "/articles?category=" + safeId(request.getParameter("id"), "1");
            case "/article/search":
                return "/articles?q=" + encode(request.getParameter("keyword"));
            case "/article/add":
                return "/editor/new";
            case "/article/edit":
                return "/editor/" + safeId(request.getParameter("id"), "1");
            case "/article/download":
                return "/api/files/" + request.getParameter("file") + "/download";
            case "/article/export":
                return "/api/admin/articles/export";
            case "/user/profile":
                return "/me";
            case "/user/edit":
            case "/user/editProfile":
                return "/me/edit";
            case "/user/password":
            case "/user/changePassword":
                return "/me/password";
            case "/user/articles":
                return "/me/articles";
            case "/admin/dashboard":
                return "/admin";
            case "/admin/category/add":
                return "/admin/categories?create=1";
            case "/admin/category/edit":
                return "/admin/categories?edit=" + safeId(request.getParameter("id"), "1");
            default:
                return "/";
        }
    }

    private String safeId(String value, String fallback) {
        return value != null && value.matches("\\d+") ? value : fallback;
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private boolean isSafeLegacyFileName(String value) {
        return value != null && value.matches("[A-Za-z0-9._-]+");
    }

    @Override
    public void destroy() {
    }
}
