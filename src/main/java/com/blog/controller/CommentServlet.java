package com.blog.controller;

import com.blog.entity.User;
import com.blog.service.CommentService;
import com.blog.service.impl.CommentServiceImpl;
import com.blog.util.StringUtil;
import com.google.gson.Gson;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

/**
 * 评论控制器
 * 处理评论相关的请求：发表评论、删除评论
 *
 * @author blog-system
 */
@WebServlet("/comment/*")
public class CommentServlet extends HttpServlet {

    private final CommentService commentService = new CommentServiceImpl();
    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null) {
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return;
        }

        switch (pathInfo) {
            case "/add":
                addComment(request, response);
                break;
            case "/delete":
                deleteComment(request, response);
                break;
            default:
                response.sendRedirect(request.getContextPath() + "/index.jsp");
                break;
        }
    }

    /**
     * 发表评论（Ajax接口）
     */
    private void addComment(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        // 获取登录用户
        HttpSession session = request.getSession();
        User loginUser = (User) session.getAttribute("loginUser");

        if (loginUser == null) {
            out.print(gson.toJson(Map.of("success", false, "message", "请先登录")));
            return;
        }

        // 获取参数
        String content = request.getParameter("content");
        String articleIdStr = request.getParameter("articleId");

        // XSS防护
        if (content != null) {
            content = StringUtil.escapeHtml(content);
        }

        try {
            int articleId = Integer.parseInt(articleIdStr);

            // 调用Service层发表评论
            String result = commentService.addComment(content, loginUser.getId(), articleId);

            if (result == null) {
                // 发表成功，返回成功信息
                out.print(gson.toJson(Map.of(
                        "success", true,
                        "message", "评论发表成功",
                        "nickname", loginUser.getNickname(),
                        "avatar", loginUser.getAvatar() != null ? loginUser.getAvatar() : request.getContextPath() + "/static/images/default-avatar.png"
                )));
            } else {
                out.print(gson.toJson(Map.of("success", false, "message", result)));
            }
        } catch (NumberFormatException e) {
            out.print(gson.toJson(Map.of("success", false, "message", "参数错误")));
        }
    }

    /**
     * 删除评论（Ajax接口）
     */
    private void deleteComment(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        // 获取登录用户
        HttpSession session = request.getSession();
        User loginUser = (User) session.getAttribute("loginUser");

        if (loginUser == null) {
            out.print(gson.toJson(Map.of("success", false, "message", "请先登录")));
            return;
        }

        // 获取参数
        String commentIdStr = request.getParameter("commentId");

        try {
            int commentId = Integer.parseInt(commentIdStr);

            // 调用Service层删除评论
            boolean success = commentService.delete(commentId, loginUser.getId(), loginUser.isAdmin());

            if (success) {
                out.print(gson.toJson(Map.of("success", true, "message", "评论删除成功")));
            } else {
                out.print(gson.toJson(Map.of("success", false, "message", "删除失败，可能没有权限")));
            }
        } catch (NumberFormatException e) {
            out.print(gson.toJson(Map.of("success", false, "message", "参数错误")));
        }
    }
}
