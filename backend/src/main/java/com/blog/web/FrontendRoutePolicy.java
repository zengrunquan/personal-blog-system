package com.blog.web;

import java.util.Locale;
import java.util.Set;

public final class FrontendRoutePolicy {

    private static final Set<String> LEGACY_ROUTES = Set.of(
            "/home", "/index.jsp", "/login.jsp", "/register.jsp",
            "/article/list", "/article/detail", "/article/category", "/article/search",
            "/article/add", "/article/edit", "/article/download", "/article/export",
            "/user/profile", "/user/edit",
            "/user/editProfile", "/user/password", "/user/changePassword", "/user/articles",
            "/admin/dashboard", "/admin/category/add", "/admin/category/edit"
    );

    private FrontendRoutePolicy() {
    }

    public static boolean isSpaRoute(String path) {
        if (path == null) return false;
        String normalized = path.toLowerCase(Locale.ROOT);
        if (isUnder(normalized, "/api")
                || isUnder(normalized, "/uploads")
                || isUnder(normalized, "/assets")
                || isUnder(normalized, "/static")
                || isUnder(normalized, "/web-inf")
                || isUnder(normalized, "/meta-inf")
                || isUnder(normalized, "/error")) {
            return false;
        }
        if (normalized.contains("/download") || normalized.contains("/export")) return false;
        // 带扩展名的请求应由容器按静态资源处理；其余 GET 交给 Vue 的全局路由和 404 页面。
        return !normalized.matches(".*\\.[a-z0-9]{1,12}$");
    }

    public static boolean isLegacyRoute(String path) {
        return path != null && LEGACY_ROUTES.contains(path);
    }

    public static boolean isLegacyWriteRoute(String path) {
        if (path == null || path.startsWith("/api/")) return false;
        return path.startsWith("/article/")
                || path.startsWith("/comment/")
                || path.startsWith("/user/")
                || path.startsWith("/admin/")
                || "/login.jsp".equals(path)
                || "/register.jsp".equals(path);
    }

    public static boolean isLegacyUnsafeGet(String path) {
        // 注销会改变 Session，不能为了兼容旧链接重新开放 GET 状态变更。
        return "/user/logout".equals(path);
    }

    public static boolean isLegacyPublicAttachment(String path) {
        return path != null && path.matches("/uploads/files/[A-Za-z0-9._-]+");
    }

    public static boolean isLegacyPublicAttachmentNamespace(String path) {
        if (path == null) return false;
        String root = "/uploads/files";
        // 矩阵参数可能在容器映射静态资源前被移除，因此也必须视为旧公开目录的一部分。
        return path.equals(root) || path.startsWith(root + "/") || path.startsWith(root + ";");
    }

    private static boolean isUnder(String path, String root) {
        return path.equals(root) || path.startsWith(root + "/");
    }
}
