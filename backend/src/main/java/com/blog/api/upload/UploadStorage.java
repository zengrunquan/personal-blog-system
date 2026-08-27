package com.blog.api.upload;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

public final class UploadStorage {

    public static final String STORAGE_DIRECTORY_PROPERTY = "blog.upload.dir";
    public static final String STORAGE_DIRECTORY_ENV = "BLOG_UPLOAD_DIR";
    private static final Path DEFAULT_RELATIVE_STORAGE_DIRECTORY = Path.of("docs", "uploads");
    private static final Pattern SAFE_URL_FILE_NAME = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]*");

    private UploadStorage() {
    }

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
        String configured = firstNonBlank(
                System.getProperty(STORAGE_DIRECTORY_PROPERTY),
                System.getenv(STORAGE_DIRECTORY_ENV)
        );
        Path directory;
        try {
            directory = configured == null
                    ? resolveDefaultStorageDirectory()
                    : Path.of(configured).toAbsolutePath().normalize();
            Files.createDirectories(directory);
            Files.createDirectories(directory.resolve("image"));
            Files.createDirectories(directory.resolve("file"));
        } catch (InvalidPathException e) {
            throw new IOException("上传存储路径无效：" + printablePath(configured), e);
        } catch (FileAlreadyExistsException e) {
            throw new IOException("上传存储路径不是目录：" + e.getFile(), e);
        } catch (AccessDeniedException e) {
            throw new IOException("没有权限创建或访问上传存储目录：" + e.getFile(), e);
        }
        for (Path requiredDirectory : List.of(
                directory,
                directory.resolve("image"),
                directory.resolve("file")
        )) {
            if (!Files.isDirectory(requiredDirectory)) {
                throw new IOException("上传存储路径不是目录：" + requiredDirectory);
            }
            if (!Files.isWritable(requiredDirectory)) {
                throw new IOException("上传存储目录不可写：" + requiredDirectory);
            }
        }
        return directory;
    }

    private static Path resolveDefaultStorageDirectory() throws IOException {
        List<Path> startingDirectories = new ArrayList<>();
        String workingDirectory = System.getProperty("user.dir");
        if (workingDirectory != null && !workingDirectory.isBlank()) {
            try {
                startingDirectories.add(Path.of(workingDirectory));
            } catch (InvalidPathException ignored) {
                // 当前工作目录由运行环境控制，格式异常时继续使用类加载位置定位项目。
            }
        }

        try {
            CodeSource codeSource = UploadStorage.class.getProtectionDomain().getCodeSource();
            if (codeSource != null && codeSource.getLocation() != null) {
                startingDirectories.add(Path.of(codeSource.getLocation().toURI()));
            }
        } catch (URISyntaxException | InvalidPathException ignored) {
            // IDEA/Tomcat 的类加载 URL 可能不是文件路径，失败时给出统一的可操作配置提示。
        }

        for (Path startingDirectory : startingDirectories) {
            Path projectRoot = findProjectRoot(startingDirectory);
            if (projectRoot != null) {
                return projectRoot.resolve(DEFAULT_RELATIVE_STORAGE_DIRECTORY)
                        .toAbsolutePath()
                        .normalize();
            }
        }
        throw storageDirectoryConfigurationError();
    }

    static Path resolveDefaultStorageDirectory(Path startingDirectory) throws IOException {
        Path projectRoot = findProjectRoot(startingDirectory);
        if (projectRoot == null) {
            throw storageDirectoryConfigurationError();
        }
        return projectRoot.resolve(DEFAULT_RELATIVE_STORAGE_DIRECTORY)
                .toAbsolutePath()
                .normalize();
    }

    private static Path findProjectRoot(Path startingDirectory) {
        if (startingDirectory == null) return null;
        Path current = startingDirectory.toAbsolutePath().normalize();
        if (Files.isRegularFile(current)) current = current.getParent();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("backend/pom.xml"))
                    && Files.isRegularFile(current.resolve("frontend/package.json"))) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }

    private static IOException storageDirectoryConfigurationError() {
        return new IOException(
                "无法定位 personal-blog-system 项目根目录；请通过 JVM 参数 -D"
                        + STORAGE_DIRECTORY_PROPERTY + "=<目录> 或环境变量 "
                        + STORAGE_DIRECTORY_ENV + " 指定上传存储目录"
        );
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

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value.trim();
        }
        return null;
    }

    private static String printablePath(String value) {
        return value == null ? "null" : value.replace("\u0000", "\\0");
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
