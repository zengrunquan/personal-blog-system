package com.blog.api.servlet;

import com.blog.api.upload.PublicMediaResolver;
import com.blog.api.upload.UploadPolicy;
import com.blog.api.upload.UploadStorage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@WebServlet("/uploads/*")
public class PublicMediaServlet extends HttpServlet {

    private static final Logger LOGGER = LogManager.getLogger(PublicMediaServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Path file;
        try {
            file = PublicMediaResolver.resolve(
                    UploadStorage.storageDirectory(),
                    request.getPathInfo()
            );
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        } catch (IOException e) {
            LOGGER.error("[PublicMediaServlet#doGet] 无法访问上传存储目录，uri={}",
                    request.getRequestURI(), e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "上传存储目录不可用");
            return;
        }

        if (!Files.isRegularFile(file)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String contentType = UploadPolicy.imageContentType(file.getFileName().toString());
        if (contentType == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setContentType(contentType);
        response.setHeader("X-Content-Type-Options", "nosniff");
        // UUID 文件名不可变，允许浏览器长期缓存，减少重复读取磁盘。
        response.setHeader("Cache-Control", "public, max-age=31536000, immutable");
        response.setContentLengthLong(Files.size(file));
        try {
            Files.copy(file, response.getOutputStream());
            response.getOutputStream().flush();
        } catch (IOException e) {
            LOGGER.error("[PublicMediaServlet#doGet] 输出公共图片失败，file={}，uri={}",
                    file, request.getRequestURI(), e);
            throw e;
        }
    }
}
