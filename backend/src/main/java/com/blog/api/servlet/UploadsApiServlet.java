package com.blog.api.servlet;

import com.blog.api.upload.UploadStorage;

import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;

@WebServlet("/api/uploads/*")
@MultipartConfig(maxFileSize = 10L * 1024 * 1024, maxRequestSize = 11L * 1024 * 1024)
public class UploadsApiServlet extends BaseApiServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        execute(request, response, () -> {
            requireUser(request);
            Part part = request.getPart("file");
            if (part == null) throw validation("file", "请选择文件");
            try {
                if ("/images".equals(pathInfo(request))) {
                    writeCreated(response,
                            UploadStorage.saveImage(part, "images", request, getServletContext()),
                            "图片上传成功");
                    return;
                }
                if ("/files".equals(pathInfo(request))) {
                    writeCreated(response,
                            UploadStorage.saveAttachment(part, request, getServletContext()),
                            "附件上传成功");
                    return;
                }
                throw notFound("上传接口不存在");
            } catch (IllegalArgumentException e) {
                throw validation("file", e.getMessage());
            }
        });
    }
}
