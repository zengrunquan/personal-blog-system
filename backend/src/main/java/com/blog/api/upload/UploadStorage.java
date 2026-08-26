package com.blog.api.upload;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;

public final class UploadStorage {

    private UploadStorage() {
    }

    public static UploadResult saveImage(
            Part part,
            String subDirectory,
            HttpServletRequest request,
            ServletContext context
    ) throws IOException {
        UploadPolicy.validateImage(part.getContentType(), part.getSize());
        String fileName = UUID.randomUUID() + UploadPolicy.imageExtension(part.getContentType());
        Path directory = uploadDirectory(context, subDirectory);
        Files.createDirectories(directory);
        part.write(directory.resolve(fileName).toString());
        return new UploadResult(
                fileName,
                UploadPolicy.safeOriginalName(part.getSubmittedFileName()),
                request.getContextPath() + "/uploads/" + subDirectory + "/" + fileName,
                part.getContentType(),
                part.getSize()
        );
    }

    public static UploadResult saveAttachment(
            Part part,
            HttpServletRequest request,
            ServletContext context
    ) throws IOException {
        UploadPolicy.validateAttachment(part.getSize());
        String original = UploadPolicy.safeOriginalName(part.getSubmittedFileName());
        String extension = extensionOf(original);
        String fileName = UUID.randomUUID() + extension;
        // 附件可能包含 HTML 等主动内容，必须放在容器禁止直接访问的 WEB-INF 下。
        Path directory = attachmentDirectory(context);
        Files.createDirectories(directory);
        part.write(directory.resolve(fileName).toString());
        return new UploadResult(
                fileName,
                original,
                request.getContextPath() + "/api/files/" + fileName + "/download",
                part.getContentType(),
                part.getSize()
        );
    }

    public static Path uploadDirectory(ServletContext context, String subDirectory) {
        String realPath = context.getRealPath("/uploads/" + subDirectory);
        if (realPath == null) throw new IllegalStateException("当前部署方式不支持写入上传目录");
        return Path.of(realPath).toAbsolutePath().normalize();
    }

    public static Path attachmentDirectory(ServletContext context) {
        String realPath = context.getRealPath("/WEB-INF/private-uploads/files");
        if (realPath == null) throw new IllegalStateException("当前部署方式不支持写入附件目录");
        return Path.of(realPath).toAbsolutePath().normalize();
    }

    private static String extensionOf(String name) {
        int index = name.lastIndexOf('.');
        if (index < 0 || index == name.length() - 1) return "";
        String extension = name.substring(index).toLowerCase(Locale.ROOT);
        return extension.length() <= 12 && extension.matches("\\.[a-z0-9]+") ? extension : "";
    }

    public static final class UploadResult {
        public final String storedName;
        public final String originalName;
        public final String url;
        public final String contentType;
        public final long size;

        public UploadResult(
                String storedName,
                String originalName,
                String url,
                String contentType,
                long size
        ) {
            this.storedName = storedName;
            this.originalName = originalName;
            this.url = url;
            this.contentType = contentType;
            this.size = size;
        }
    }
}
