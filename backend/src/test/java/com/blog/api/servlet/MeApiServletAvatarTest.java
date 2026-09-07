package com.blog.api.servlet;

import com.blog.api.upload.UploadStorage;
import com.blog.entity.User;
import com.blog.media.service.MediaUploadService;
import com.blog.service.ArticleService;
import com.blog.service.UserService;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MeApiServletAvatarTest {

    @Test
    public void avatarSessionShouldChangeOnlyAfterUploadServiceSucceeds() throws Exception {
        UserService users = mock(UserService.class);
        ArticleService articles = mock(ArticleService.class);
        MediaUploadService uploads = mock(MediaUploadService.class);
        Part part = mock(Part.class);
        User user = new User();
        user.setId(3);
        UploadStorage.UploadResult result = new UploadStorage.UploadResult(
                "avatar_avatar-a.png", "a.png", "/uploads/avatars/avatar-a.png", "image/png", 9
        );
        when(uploads.replaceAvatar(part, 3, "")).thenReturn(result);

        MeApiServlet servlet = new MeApiServlet(users, articles, uploads);
        HttpSession session = mock(HttpSession.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(session.getAttribute("loginUser")).thenReturn(user);
        when(request.getSession(false)).thenReturn(session);
        when(request.getSession()).thenReturn(session);
        when(request.getPathInfo()).thenReturn("/avatar");
        when(request.getPart("file")).thenReturn(part);
        when(request.getContextPath()).thenReturn("");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/me/avatar");
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        servlet.doPost(request, response);

        verify(uploads).replaceAvatar(part, 3, "");
        verify(session).setAttribute(eq("loginUser"), eq(user));
        verify(response).setStatus(HttpServletResponse.SC_CREATED);
    }
}
