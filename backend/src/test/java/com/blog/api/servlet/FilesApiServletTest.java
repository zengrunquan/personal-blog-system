package com.blog.api.servlet;

import com.blog.api.upload.UploadStorage;
import com.blog.entity.User;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FilesApiServletTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void downloadNameShouldNotExposePhysicalFilePrefix() throws Exception {
        Path storageRoot = temporaryFolder.newFolder("uploads").toPath();
        String urlFileName = "12345678-1234-1234-1234-123456789abc.pdf";
        Path storedFile = storageRoot.resolve("file/file_" + urlFileName);
        Files.createDirectories(storedFile.getParent());
        Files.write(storedFile, new byte[]{1, 2, 3});

        FilesApiServlet servlet = initializedServlet();
        HttpServletRequest request = authenticatedRequest("/file_" + urlFileName + "/download");
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(discardingOutputStream());

        withStorageDirectory(storageRoot, () -> servlet.doGet(request, response));

        ArgumentCaptor<String> header = ArgumentCaptor.forClass(String.class);
        verify(response).setHeader(eq("Content-Disposition"), header.capture());
        assertTrue(header.getValue().contains(urlFileName));
        assertFalse(header.getValue().contains("file_" + urlFileName));
    }

    private FilesApiServlet initializedServlet() throws Exception {
        ServletContext context = mock(ServletContext.class);
        when(context.getMimeType("file_12345678-1234-1234-1234-123456789abc.pdf"))
                .thenReturn("application/pdf");
        ServletConfig config = mock(ServletConfig.class);
        when(config.getServletContext()).thenReturn(context);
        FilesApiServlet servlet = new FilesApiServlet();
        servlet.init(config);
        return servlet;
    }

    private HttpServletRequest authenticatedRequest(String pathInfo) {
        User user = new User();
        user.setId(1);
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute("loginUser")).thenReturn(user);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession(false)).thenReturn(session);
        when(request.getPathInfo()).thenReturn(pathInfo);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/files" + pathInfo);
        return request;
    }

    private ServletOutputStream discardingOutputStream() {
        return new ServletOutputStream() {
            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {
                // 单元测试同步消费少量字节，不需要容器的异步写回调。
            }

            @Override
            public void write(int value) {
                // 此测试只验证响应头，响应体由 Files.copy 的真实调用覆盖即可。
            }
        };
    }

    private void withStorageDirectory(Path storageRoot, CheckedAction action) throws Exception {
        String previous = System.getProperty(UploadStorage.STORAGE_DIRECTORY_PROPERTY);
        System.setProperty(UploadStorage.STORAGE_DIRECTORY_PROPERTY, storageRoot.toString());
        try {
            action.run();
        } finally {
            if (previous == null) {
                System.clearProperty(UploadStorage.STORAGE_DIRECTORY_PROPERTY);
            } else {
                System.setProperty(UploadStorage.STORAGE_DIRECTORY_PROPERTY, previous);
            }
        }
    }

    @FunctionalInterface
    private interface CheckedAction {
        void run() throws Exception;
    }
}
