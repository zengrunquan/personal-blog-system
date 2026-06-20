package com.blog.filter;

import com.blog.entity.User;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * 管理员权限过滤器
 * 拦截管理员页面，非管理员用户跳转到首页
 *
 * @author blog-system
 */
// 注意：不在这里使用@WebFilter注解，因为web.xml中已经声明了该过滤器
// 如果同时使用注解和web.xml声明，过滤器会执行两次
public class AdminFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 初始化方法
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;

        // 获取Session
        HttpSession session = request.getSession(false);

        // 检查用户是否已登录
        if (session == null || session.getAttribute("loginUser") == null) {
            // 未登录，跳转到登录页（URL编码中文字符）
            String contextPath = request.getContextPath();
            String encodedMsg = java.net.URLEncoder.encode("请先登录", "UTF-8");
            response.sendRedirect(contextPath + "/login.jsp?msg=" + encodedMsg);
            return;
        }

        // 检查是否为管理员
        User user = (User) session.getAttribute("loginUser");
        if (user.isAdmin()) {
            // 是管理员，继续执行
            chain.doFilter(request, response);
        } else {
            // 不是管理员，跳转到首页并提示（URL编码中文字符）
            String contextPath = request.getContextPath();
            String encodedMsg = java.net.URLEncoder.encode("您没有管理员权限", "UTF-8");
            response.sendRedirect(contextPath + "/index.jsp?msg=" + encodedMsg);
        }
    }

    @Override
    public void destroy() {
        // 销毁方法
    }
}
