package com.blog.media.storage;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;

/** 统一解析媒体根目录；解析本身不创建目录，只有上传写入阶段才允许创建目录。 */
public final class StorageRootResolver {

    public static final String STORAGE_DIRECTORY_PROPERTY = "blog.upload.dir";
    public static final String STORAGE_DIRECTORY_ENV = "BLOG_UPLOAD_DIR";
    private static final Path DEFAULT_RELATIVE_STORAGE_DIRECTORY = Paths.get("docs", "uploads");

    private StorageRootResolver() {
    }

    /**
     * 按配置优先级解析上传根目录。没有显式配置时，只能从项目标识定位，禁止使用工作目录兜底。
     */
    public static Path resolve() throws IOException {
        String configured = firstNonBlank(
                System.getProperty(STORAGE_DIRECTORY_PROPERTY),
                System.getenv(STORAGE_DIRECTORY_ENV)
        );
        if (configured != null) {
            try {
                return Path.of(configured).toAbsolutePath().normalize();
            } catch (InvalidPathException error) {
                throw new IOException("上传存储路径无效：" + printablePath(configured), error);
            }
        }

        List<Path> startingDirectories = new ArrayList<>();
        addPathIfValid(startingDirectories, System.getProperty("user.dir"));
        try {
            CodeSource codeSource = StorageRootResolver.class
                    .getProtectionDomain().getCodeSource();
            if (codeSource != null && codeSource.getLocation() != null) {
                startingDirectories.add(Path.of(codeSource.getLocation().toURI()));
            }
        } catch (URISyntaxException | InvalidPathException ignored) {
            // 类加载位置可能来自容器虚拟 URL，失败时继续尝试其他可验证的起点。
        }

        for (Path startingDirectory : startingDirectories) {
            Path resolved = findProjectRoot(startingDirectory);
            if (resolved != null) {
                return resolved.resolve(DEFAULT_RELATIVE_STORAGE_DIRECTORY)
                        .toAbsolutePath().normalize();
            }
        }
        throw configurationError();
    }

    /** 从给定起点定位项目根目录，供回填和测试复用且不会创建任何目录。 */
    public static Path resolveDefaultStorageDirectory(Path startingDirectory) throws IOException {
        Path projectRoot = findProjectRoot(startingDirectory);
        if (projectRoot == null) throw configurationError();
        return projectRoot.resolve(DEFAULT_RELATIVE_STORAGE_DIRECTORY)
                .toAbsolutePath().normalize();
    }

    /** 回填使用的只读校验；目录缺失时直接失败，避免把错误路径当成空目录。 */
    public static Path requireExistingReadableRoot(Path root) throws IOException {
        if (root == null) throw new IOException("上传存储根目录不能为空");
        Path normalized = root.toAbsolutePath().normalize();
        if (!Files.isDirectory(normalized)) {
            throw new IOException("上传存储根目录不存在或不是目录：" + normalized);
        }
        if (!Files.isReadable(normalized)) {
            throw new IOException("上传存储根目录不可读：" + normalized);
        }
        return normalized;
    }

    /** 上传写入使用的目录准备流程；与只读解析分离，避免 dry-run 改变文件系统。 */
    public static Path ensureWritableStorageDirectories(Path root) throws IOException {
        if (root == null) throw new IOException("上传存储根目录不能为空");
        Path normalized = root.toAbsolutePath().normalize();
        try {
            Files.createDirectories(normalized);
            Files.createDirectories(normalized.resolve("image"));
            Files.createDirectories(normalized.resolve("file"));
        } catch (FileAlreadyExistsException error) {
            throw new IOException("上传存储路径不是目录：" + error.getFile(), error);
        } catch (AccessDeniedException error) {
            throw new IOException("没有权限创建或访问上传存储目录：" + error.getFile(), error);
        }
        for (Path required : List.of(
                normalized,
                normalized.resolve("image"),
                normalized.resolve("file")
        )) {
            if (!Files.isDirectory(required)) {
                throw new IOException("上传存储路径不是目录：" + required);
            }
            if (!Files.isWritable(required)) {
                throw new IOException("上传存储目录不可写：" + required);
            }
        }
        return normalized;
    }

    private static void addPathIfValid(List<Path> paths, String value) {
        if (value == null || value.trim().isEmpty()) return;
        try {
            paths.add(Path.of(value));
        } catch (InvalidPathException ignored) {
            // 工作目录由运行环境控制，格式异常时让类加载位置继续参与定位。
        }
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

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return null;
    }

    private static IOException configurationError() {
        return new IOException(
                "无法定位 personal-blog-system 项目根目录；请通过 JVM 参数 -D"
                        + STORAGE_DIRECTORY_PROPERTY + "=<目录> 或环境变量 "
                        + STORAGE_DIRECTORY_ENV + " 指定上传存储目录"
        );
    }

    private static String printablePath(String value) {
        return value == null ? "null" : value.replace("\u0000", "\\0");
    }
}
