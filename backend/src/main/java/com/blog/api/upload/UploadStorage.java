package com.blog.api.upload;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import com.blog.media.storage.StorageRootResolver;

public final class UploadStorage {

    public static final String STORAGE_DIRECTORY_PROPERTY =
            StorageRootResolver.STORAGE_DIRECTORY_PROPERTY;
    public static final String STORAGE_DIRECTORY_ENV = StorageRootResolver.STORAGE_DIRECTORY_ENV;
    private static final Pattern SAFE_URL_FILE_NAME = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]*");

    private UploadStorage() {
    }

    /**
     * @deprecated 兼容旧调用方；新上传必须经过 MediaUploadService，才能登记媒体元数据并参与回收。
     */
    @Deprecated
    public static UploadResult saveImage(
            Part part,
            String subDirectory,
            HttpServletRequest request
    ) throws IOException {
        UploadPolicy.validateImage(part.getContentType(), part.getSize());
        String urlFileName = UUID.randomUUID() + UploadPolicy.imageExtension(part.getContentType());
        String fileName = imagePrefix(subDirectory) + urlFileName;
        Path directory = storageDirectory().resolve("image");
        part.write(directory.resolve(fileName).toString());
        return new UploadResult(
                fileName,
                UploadPolicy.safeOriginalName(part.getSubmittedFileName()),
                request.getContextPath() + "/uploads/" + subDirectory + "/" + urlFileName,
                part.getContentType(),
                part.getSize()
        );
    }

    /**
     * @deprecated 兼容旧调用方；新上传必须经过 MediaUploadService，才能登记媒体元数据并参与回收。
     */
    @Deprecated
    public static UploadResult saveAttachment(
            Part part,
            HttpServletRequest request
    ) throws IOException {
        UploadPolicy.validateAttachment(part.getSize());
        String original = UploadPolicy.safeOriginalName(part.getSubmittedFileName());
        String extension = extensionOf(original);
        String urlFileName = UUID.randomUUID() + extension;
        String fileName = "file_" + urlFileName;
        // 附件独立存放，同时保留物理前缀以兼容现有 URL 到磁盘文件的映射规则。
        Path directory = storageDirectory().resolve("file");
        part.write(directory.resolve(fileName).toString());
        return new UploadResult(
                fileName,
                original,
                request.getContextPath() + "/api/files/" + urlFileName + "/download",
                part.getContentType(),
                part.getSize()
        );
    }

    public static Path storageDirectory() throws IOException {
        return StorageRootResolver.ensureWritableStorageDirectories(
                StorageRootResolver.resolve()
        );
    }

    static Path resolveDefaultStorageDirectory(Path startingDirectory) throws IOException {
        return StorageRootResolver.resolveDefaultStorageDirectory(startingDirectory);
    }

    public static Path resolveAttachmentFile(String urlFileName) throws IOException {
        if (urlFileName == null || !SAFE_URL_FILE_NAME.matcher(urlFileName).matches()) {
            throw new IllegalArgumentException("非法附件 URL 文件名");
        }
        String storedName = urlFileName.startsWith("file_")
                ? urlFileName
                : "file_" + urlFileName;
        return com.blog.util.UploadFileDownloadUtil.resolveDownloadPath(
                storageDirectory().resolve("file"),
                storedName
        );
    }

    private static String imagePrefix(String subDirectory) {
        if ("avatars".equals(subDirectory)) return "avatar_";
        if ("images".equals(subDirectory)) return "image_";
        throw new IllegalArgumentException("不支持的图片存储类型");
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
