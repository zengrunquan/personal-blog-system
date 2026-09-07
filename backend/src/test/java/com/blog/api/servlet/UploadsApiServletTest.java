package com.blog.api.servlet;

import com.blog.api.upload.UploadStorage;
import com.blog.entity.User;
import com.blog.media.model.MediaType;
import com.blog.media.service.MediaUploadService;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UploadsApiServletTest {

    private StringWriter output;

    @Test
    public void imageUploadShouldKeepCreatedResponseAndContract() throws Exception {
        MediaUploadService uploads = mock(MediaUploadService.class);
        Part part = mock(Part.class);
        UploadStorage.UploadResult result = new UploadStorage.UploadResult(
                "image_image-a.png", "a.png", "/blog/uploads/images/image-a.png", "image/png", 12
        );
        when(uploads.upload(part, MediaType.ARTICLE_IMAGE, 7, "/blog")).thenReturn(result);

        UploadsApiServlet servlet = new UploadsApiServlet(uploads);
        HttpServletRequest request = authenticatedRequest("/images", part);
        HttpServletResponse response = responseWithBody();

        servlet.doPost(request, response);

        verify(uploads).upload(part, MediaType.ARTICLE_IMAGE, 7, "/blog");
        verify(response).setStatus(HttpServletResponse.SC_CREATED);
        assertTrue(responseBody(response).contains("\"storedName\":\"image_image-a.png\""));
        assertTrue(responseBody(response).contains("\"url\":\"/blog/uploads/images/image-a.png\""));
    }

    private HttpServletRequest authenticatedRequest(String path, Part part) throws Exception {
        User user = new User();
        user.setId(7);
        HttpSession session = mock(HttpSession.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(session.getAttribute("loginUser")).thenReturn(user);
        when(request.getSession(false)).thenReturn(session);
        when(request.getPathInfo()).thenReturn(path);
        when(request.getPart("file")).thenReturn(part);
        when(request.getContextPath()).thenReturn("/blog");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/blog/api/uploads" + path);
        return request;
    }

    private HttpServletResponse responseWithBody() throws Exception {
        HttpServletResponse response = mock(HttpServletResponse.class);
        output = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(output));
        return response;
    }

    private String responseBody(HttpServletResponse response) throws Exception {
        output.flush();
        return output.toString();
    }
}
