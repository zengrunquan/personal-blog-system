package com.blog.api.servlet;

import com.blog.entity.User;
import com.blog.media.model.MediaType;
import com.blog.media.service.MediaUploadService;

import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;

@WebServlet("/api/uploads/*")
@MultipartConfig(maxFileSize = 10L * 1024 * 1024, maxRequestSize = 11L * 1024 * 1024)
public class UploadsApiServlet extends BaseApiServlet {

    private final MediaUploadService mediaUploadService;

    public UploadsApiServlet() {
        this(new MediaUploadService());
    }

    UploadsApiServlet(MediaUploadService mediaUploadService) {
        this.mediaUploadService = java.util.Objects.requireNonNull(
                mediaUploadService, "mediaUploadService 不能为空");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        execute(request, response, () -> {
            User user = requireUser(request);
            Part part = request.getPart("file");
            if (part == null) throw validation("file", "请选择文件");
            try {
                if ("/images".equals(pathInfo(request))) {
                    writeCreated(response,
                            mediaUploadService.upload(
                                    part,
                                    MediaType.ARTICLE_IMAGE,
                                    user.getId(),
                                    request.getContextPath()
                            ),
                            "图片上传成功");
                    return;
                }
                if ("/files".equals(pathInfo(request))) {
                    writeCreated(response,
                            mediaUploadService.upload(
                                    part,
                                    MediaType.ATTACHMENT,
                                    user.getId(),
                                    request.getContextPath()
                            ),
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
