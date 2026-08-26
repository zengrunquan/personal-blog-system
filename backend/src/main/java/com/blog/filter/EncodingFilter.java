package com.blog.filter;

import javax.servlet.*;
import java.io.IOException;

/**
 * 编码过滤器
 * 统一设置请求和响应的字符编码为UTF-8
 *
 * @author blog-system
 */
// 注意：不在这里使用@WebFilter注解，因为web.xml中已经声明了该过滤器
// 如果同时使用注解和web.xml声明，过滤器会执行两次
public class EncodingFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 初始化方法，无需特殊处理
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        // 设置请求编码
        request.setCharacterEncoding("UTF-8");
        // 设置响应编码（不设置Content-Type，让后续Servlet/容器自行决定）
        // 之前设置 response.setContentType("text/html;charset=UTF-8") 会导致
        // CSS/JS/图片等静态资源的Content-Type被错误覆盖为text/html，
        // 浏览器因MIME类型不匹配而拒绝加载样式表，导致头像等图片按原始尺寸显示
        response.setCharacterEncoding("UTF-8");

        // 继续执行后续过滤器或Servlet
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // 销毁方法，无需特殊处理
    }
}
