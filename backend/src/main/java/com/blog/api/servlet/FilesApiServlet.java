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
                file = UploadFileDownloadUtil.resolveDownloadPath(
                        UploadStorage.attachmentDirectory(getServletContext()),
                        parts[0]
                );
                // 迁移前附件位于公开目录；静态访问现已重定向到本接口，因此可以安全兼容旧文件。
                if (!Files.isRegularFile(file)) {
                    file = UploadFileDownloadUtil.resolveDownloadPath(
                            UploadStorage.uploadDirectory(getServletContext(), "files"),
                            parts[0]
                    );
                }
            } catch (IllegalArgumentException e) {
                throw badRequest("INVALID_FILE_NAME", e.getMessage());
            }
            if (!Files.isRegularFile(file)) throw notFound("附件不存在");
            String contentType = getServletContext().getMimeType(file.getFileName().toString());
            response.setContentType(contentType == null ? "application/octet-stream" : contentType);
            response.setHeader("Content-Disposition",
                    UploadFileDownloadUtil.buildContentDisposition(file.getFileName().toString()));
            response.setContentLengthLong(Files.size(file));
            Files.copy(file, response.getOutputStream());
            response.getOutputStream().flush();
        });
    }
}
