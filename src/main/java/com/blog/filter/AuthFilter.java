package com.blog.filter;

import com.blog.entity.User;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 登录拦截过滤器
 * 拦截需要登录才能访问的页面，未登录用户跳转到登录页
 *
 * @author blog-system
 */
// 注意：不在这里使用@WebFilter注解，因为web.xml中已经声明了该过滤器
// 如果同时使用注解和web.xml声明，过滤器会执行两次
public class AuthFilter implements Filter {

    // 不需要登录就可以访问的路径
    private static final List<String> EXCLUDE_PATHS = Arrays.asList(
        "/user/login",
        "/user/register",
        "/user/checkUsername"
    );

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 初始化方法
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;

        // 获取请求路径
        String pathInfo = request.getPathInfo();
        String servletPath = request.getServletPath();
        String fullPath = servletPath + (pathInfo != null ? pathInfo : "");

        // 检查是否是排除的路径（登录、注册、检查用户名等）
        if (EXCLUDE_PATHS.contains(fullPath)) {
            // 不需要登录验证，直接放行
            chain.doFilter(request, response);
            return;
        }

        // 获取Session
        HttpSession session = request.getSession(false);

        // 检查用户是否已登录
        boolean isLoggedIn = (session != null && session.getAttribute("loginUser") != null);

        if (isLoggedIn) {
            // 已登录，继续执行
            chain.doFilter(request, response);
        } else {
            // 未登录，保存当前请求URL，登录后跳转回来
            String requestURI = request.getRequestURI();
            String queryString = request.getQueryString();
            String targetUrl = requestURI;
            if (queryString != null && !queryString.isEmpty()) {
                targetUrl += "?" + queryString;
            }

            if (session == null) {
                session = request.getSession(true);
            }
            session.setAttribute("redirectUrl", targetUrl);

            // 跳转到登录页面（URL编码中文字符）
            String contextPath = request.getContextPath();
            String encodedMsg = java.net.URLEncoder.encode("请先登录", "UTF-8");
            response.sendRedirect(contextPath + "/login.jsp?msg=" + encodedMsg);
        }
    }

    @Override
    public void destroy() {
        // 销毁方法
    }
}
