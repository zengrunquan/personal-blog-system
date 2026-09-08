package com.blog.api.servlet;

import com.blog.api.upload.UploadStorage;
import com.blog.entity.User;
import com.blog.media.service.AttachmentDownloadNameService;
import com.blog.util.TransactionException;
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
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

        AttachmentDownloadNameService downloadNameService = mock(AttachmentDownloadNameService.class);
        when(downloadNameService.resolveDownloadName(urlFileName)).thenReturn(urlFileName);
        FilesApiServlet servlet = initializedServlet(downloadNameService);
        HttpServletRequest request = authenticatedRequest("/file_" + urlFileName + "/download");
        HttpServletResponse response = mock(HttpServletResponse.class);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        when(response.getOutputStream()).thenReturn(outputStream(output));

        withStorageDirectory(storageRoot, () -> servlet.doGet(request, response));

        ArgumentCaptor<String> header = ArgumentCaptor.forClass(String.class);
        verify(response).setHeader(eq("Content-Disposition"), header.capture());
        verify(downloadNameService).resolveDownloadName(urlFileName);
        assertTrue(header.getValue().contains(urlFileName));
        assertFalse(header.getValue().contains("file_" + urlFileName));
        assertArrayEquals(new byte[]{1, 2, 3}, output.toByteArray());
    }

    @Test
    public void canonicalAndCompatibleUrlsShouldResolveTheSameCanonicalName() throws Exception {
        Path storageRoot = temporaryFolder.newFolder("uploads").toPath();
        String urlFileName = "12345678-1234-1234-1234-123456789abc.pdf";
        Path storedFile = storageRoot.resolve("file/file_" + urlFileName);
        Files.createDirectories(storedFile.getParent());
        Files.write(storedFile, new byte[]{4, 5});

        AttachmentDownloadNameService downloadNameService = mock(AttachmentDownloadNameService.class);
        when(downloadNameService.resolveDownloadName(urlFileName)).thenReturn("课程笔记.pdf");
        FilesApiServlet servlet = initializedServlet(downloadNameService);

        for (String pathInfo : new String[]{
                "/" + urlFileName + "/download",
                "/file_" + urlFileName + "/download"
        }) {
            HttpServletResponse response = mock(HttpServletResponse.class);
            when(response.getOutputStream()).thenReturn(outputStream(new ByteArrayOutputStream()));
            withStorageDirectory(storageRoot, () -> servlet.doGet(
                    authenticatedRequest(pathInfo), response));
        }

        verify(downloadNameService, times(2)).resolveDownloadName(urlFileName);
    }

    @Test
    public void downloadShouldUsePersistedChineseNameAndPreserveContent() throws Exception {
        Path storageRoot = temporaryFolder.newFolder("uploads").toPath();
        String urlFileName = "12345678-1234-1234-1234-123456789abc.pdf";
        byte[] content = new byte[]{9, 8, 7, 6};
        Path storedFile = storageRoot.resolve("file/file_" + urlFileName);
        Files.createDirectories(storedFile.getParent());
        Files.write(storedFile, content);

        AttachmentDownloadNameService downloadNameService = mock(AttachmentDownloadNameService.class);
        when(downloadNameService.resolveDownloadName(urlFileName)).thenReturn("课程笔记 第1章.pdf");
        FilesApiServlet servlet = initializedServlet(downloadNameService);
        HttpServletResponse response = mock(HttpServletResponse.class);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        when(response.getOutputStream()).thenReturn(outputStream(output));
        HttpServletRequest request = authenticatedRequest("/" + urlFileName + "/download");
        when(request.getParameter("filename")).thenReturn("伪造原名.exe");
        withStorageDirectory(storageRoot, () -> servlet.doGet(request, response));

        ArgumentCaptor<String> header = ArgumentCaptor.forClass(String.class);
        verify(response).setHeader(eq("Content-Disposition"), header.capture());
        assertEquals("课程笔记 第1章.pdf", decodeFilenameStar(header.getValue()));
        assertFalse(header.getValue().contains("伪造原名.exe"));
        assertArrayEquals(content, output.toByteArray());
    }

    @Test
    public void legalFilePrefixInOriginalNameShouldBePreserved() throws Exception {
        Path storageRoot = temporaryFolder.newFolder("uploads").toPath();
        String urlFileName = "12345678-1234-1234-1234-123456789abc.pdf";
        Path storedFile = storageRoot.resolve("file/file_" + urlFileName);
        Files.createDirectories(storedFile.getParent());
        Files.write(storedFile, new byte[]{1});

        AttachmentDownloadNameService downloadNameService = mock(AttachmentDownloadNameService.class);
        when(downloadNameService.resolveDownloadName(urlFileName)).thenReturn("file_学习资料.pdf");
        FilesApiServlet servlet = initializedServlet(downloadNameService);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(outputStream(new ByteArrayOutputStream()));

        withStorageDirectory(storageRoot, () -> servlet.doGet(
                authenticatedRequest("/" + urlFileName + "/download"), response));

        ArgumentCaptor<String> header = ArgumentCaptor.forClass(String.class);
        verify(response).setHeader(eq("Content-Disposition"), header.capture());
        assertEquals("file_学习资料.pdf", decodeFilenameStar(header.getValue()));
    }

    @Test
    public void unauthenticatedDownloadShouldReturn401WithoutNameQuery() throws Exception {
        AttachmentDownloadNameService downloadNameService = mock(AttachmentDownloadNameService.class);
        FilesApiServlet servlet = initializedServlet(downloadNameService);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = errorResponse();
        when(request.getSession(false)).thenReturn(null);
        when(request.getPathInfo()).thenReturn("/missing.pdf/download");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/files/missing.pdf/download");

        servlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(downloadNameService, never()).resolveDownloadName(anyString());
    }

    @Test
    public void invalidFileNameShouldReturn400WithoutNameQuery() throws Exception {
        AttachmentDownloadNameService downloadNameService = mock(AttachmentDownloadNameService.class);
        FilesApiServlet servlet = initializedServlet(downloadNameService);
        HttpServletResponse response = errorResponse();

        servlet.doGet(authenticatedRequest("/../download"), response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        verify(downloadNameService, never()).resolveDownloadName(anyString());
    }

    @Test
    public void missingFileShouldReturn404WithoutNameQuery() throws Exception {
        Path storageRoot = temporaryFolder.newFolder("uploads").toPath();
        AttachmentDownloadNameService downloadNameService = mock(AttachmentDownloadNameService.class);
        FilesApiServlet servlet = initializedServlet(downloadNameService);
        HttpServletResponse response = errorResponse();

        withStorageDirectory(storageRoot, () -> servlet.doGet(
                authenticatedRequest("/missing.pdf/download"), response));

        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
        verify(downloadNameService, never()).resolveDownloadName(anyString());
    }

    @Test
    public void nameQueryFailureShouldReturn500BeforeWritingDownloadResponse() throws Exception {
        Path storageRoot = temporaryFolder.newFolder("uploads").toPath();
        String urlFileName = "12345678-1234-1234-1234-123456789abc.pdf";
        Path storedFile = storageRoot.resolve("file/file_" + urlFileName);
        Files.createDirectories(storedFile.getParent());
        Files.write(storedFile, new byte[]{1, 2, 3});

        AttachmentDownloadNameService downloadNameService = mock(AttachmentDownloadNameService.class);
        when(downloadNameService.resolveDownloadName(urlFileName))
                .thenThrow(new TransactionException(new SQLException("media table unavailable")));
        FilesApiServlet servlet = initializedServlet(downloadNameService);
        HttpServletResponse response = errorResponse();

        withStorageDirectory(storageRoot, () -> servlet.doGet(
                authenticatedRequest("/" + urlFileName + "/download"), response));

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        verify(response, never()).setHeader(anyString(), anyString());
        verify(response, never()).setContentLengthLong(3L);
        verify(response, never()).getOutputStream();
        assertTrue(errorBody(response).contains("INTERNAL_ERROR"));
    }

    private FilesApiServlet initializedServlet(AttachmentDownloadNameService downloadNameService)
            throws Exception {
        ServletContext context = mock(ServletContext.class);
        when(context.getMimeType("file_12345678-1234-1234-1234-123456789abc.pdf"))
                .thenReturn("application/pdf");
        ServletConfig config = mock(ServletConfig.class);
        when(config.getServletContext()).thenReturn(context);
        FilesApiServlet servlet = new FilesApiServlet(downloadNameService);
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

    private ServletOutputStream outputStream(ByteArrayOutputStream output) {
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
                output.write(value);
            }

            @Override
            public void write(byte[] bytes, int offset, int length) {
                output.write(bytes, offset, length);
            }
        };
    }

    private HttpServletResponse errorResponse() throws Exception {
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter body = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));
        errorBodies.put(response, body);
        return response;
    }

    private final java.util.Map<HttpServletResponse, StringWriter> errorBodies =
            new java.util.IdentityHashMap<>();

    private String errorBody(HttpServletResponse response) {
        return errorBodies.get(response).toString();
    }

    private String decodeFilenameStar(String header) {
        String prefix = "filename*=UTF-8''";
        int start = header.indexOf(prefix);
        if (start < 0) throw new AssertionError("响应头缺少 filename*");
        return URLDecoder.decode(header.substring(start + prefix.length()), StandardCharsets.UTF_8);
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
