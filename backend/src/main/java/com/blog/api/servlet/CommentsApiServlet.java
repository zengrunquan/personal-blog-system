package com.blog.api.servlet;

import com.blog.entity.User;
import com.blog.service.CommentService;
import com.blog.service.impl.CommentServiceImpl;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@WebServlet("/api/comments/*")
public class CommentsApiServlet extends BaseApiServlet {

    private final CommentService commentService = new CommentServiceImpl();

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        execute(request, response, () -> {
            User user = requireUser(request);
            int id = positiveId(pathInfo(request).replace("/", ""), "id");
            if (!commentService.delete(id, user.getId(), user.isAdmin())) {
                throw forbidden("评论不存在或没有删除权限");
            }
            writeSuccess(response, Map.of("id", id), "评论已删除");
        });
    }
}
