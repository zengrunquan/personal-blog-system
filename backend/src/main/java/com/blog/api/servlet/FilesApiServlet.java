package com.blog.api.servlet;

import com.blog.api.upload.UploadStorage;
import com.blog.util.UploadFileDownloadUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@WebServlet("/api/files/*")
public class FilesApiServlet extends BaseApiServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        execute(request, response, () -> {
            requireUser(request);
            String[] parts = java.util.Arrays.stream(pathInfo(request).split("/"))
                    .filter(part -> !part.isBlank()).toArray(String[]::new);
            if (parts.length != 2 || !"download".equals(parts[1])) throw notFound("下载接口不存在");
            Path file;
            try {
                file = UploadStorage.resolveAttachmentFile(parts[0]);
            } catch (IllegalArgumentException e) {
                throw badRequest("INVALID_FILE_NAME", e.getMessage());
            }
            if (!Files.isRegularFile(file)) throw notFound("附件不存在");
            String contentType = getServletContext().getMimeType(file.getFileName().toString());
            response.setContentType(contentType == null ? "application/octet-stream" : contentType);
            String storedFileName = file.getFileName().toString();
            if (!storedFileName.startsWith("file_") || storedFileName.length() == "file_".length()) {
                throw new IOException("附件物理文件名无效：" + storedFileName);
            }
            // 下载名从已校验的服务端物理路径派生，避免把原始请求文本写入响应头。
            String downloadFileName = storedFileName.substring("file_".length());
            response.setHeader("Content-Disposition",
                    UploadFileDownloadUtil.buildContentDisposition(downloadFileName));
            response.setContentLengthLong(Files.size(file));
            Files.copy(file, response.getOutputStream());
            response.getOutputStream().flush();
        });
    }
}
