package com.blog.media.storage;

import com.blog.api.upload.UploadPolicy;
import com.blog.media.model.MediaAsset;
import com.blog.media.model.MediaType;
import com.blog.media.model.StoredMedia;
import com.blog.util.UploadFileDownloadUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/** 本地媒体文件仓库；先写同目录临时文件再原子移动，避免半文件被业务读取。 */
public class LocalMediaFileStore implements MediaFileStore {

    private static final Logger LOGGER = LogManager.getLogger(LocalMediaFileStore.class);
    private static final Pattern SAFE_STORAGE_NAME =
            Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]*");
    private final Path configuredRoot;

    public LocalMediaFileStore() {
        this(null);
    }

    public LocalMediaFileStore(Path storageRoot) {
        this.configuredRoot = storageRoot == null ? null : storageRoot.toAbsolutePath().normalize();
    }

    @Override
    public StoredMedia store(Part part, MediaType type) throws IOException {
        Objects.requireNonNull(part, "上传 Part 不能为空");
        Objects.requireNonNull(type, "媒体类型不能为空");
        String contentType = part.getContentType();
        if (type == MediaType.AVATAR || type == MediaType.ARTICLE_IMAGE) {
            UploadPolicy.validateImage(contentType, part.getSize());
        } else {
            UploadPolicy.validateAttachment(part.getSize());
        }

        String originalName = UploadPolicy.safeOriginalName(part.getSubmittedFileName());
        String extension = type == MediaType.AVATAR || type == MediaType.ARTICLE_IMAGE
                ? UploadPolicy.imageExtension(contentType)
                : extensionOf(originalName);
        String urlFileName = UUID.randomUUID() + extension;
        String storageName = type.getStoragePrefix() + urlFileName;
        Path directory = directoryFor(type);
        Path temporary = Files.createTempFile(directory, "." + storageName + ".", ".uploading");
        Path target = directory.resolve(storageName);
        boolean moved = false;
        try {
            part.write(temporary.toString());
            if (!Files.isRegularFile(temporary)) {
                throw new IOException("上传 Part 写入后未生成普通文件");
            }
            long actualSize = Files.size(temporary);
            validateActualSize(type, actualSize);
            moveAtomically(temporary, target);
            moved = true;
            return new StoredMedia(
                    type,
                    storageName,
                    urlFileName,
                    originalName,
                    contentType,
                    actualSize,
                    sha256(target),
                    target.toAbsolutePath().normalize()
            );
        } catch (IOException | RuntimeException e) {
            deleteQuietly(temporary, e);
            if (moved) deleteQuietly(target, e);
            throw e;
        } finally {
            if (!moved) deleteQuietly(temporary, null);
        }
    }

    @Override
    public Path resolve(MediaAsset asset) throws IOException {
        Objects.requireNonNull(asset, "媒体资产不能为空");
        MediaType type = Objects.requireNonNull(asset.getMediaType(), "媒体类型不能为空");
        String storageName = asset.getStorageName();
        if (storageName == null
                || !SAFE_STORAGE_NAME.matcher(storageName).matches()
                || !storageName.startsWith(type.getStoragePrefix())) {
            throw new IllegalArgumentException("媒体物理文件名无效");
        }
        return UploadFileDownloadUtil.resolveDownloadPath(
                directoryFor(type),
                storageName
        );
    }

    @Override
    public boolean deleteIfExists(MediaAsset asset) throws IOException {
        return Files.deleteIfExists(resolve(asset));
    }

    public Path storageRoot() throws IOException {
        return root();
    }

    private Path directoryFor(MediaType type) throws IOException {
        Path directory = root().resolve(type.getDirectoryName()).normalize();
        Files.createDirectories(directory);
        if (!Files.isDirectory(directory)) {
            throw new IOException("媒体存储路径不是目录：" + directory);
        }
        return directory;
    }

    private Path root() throws IOException {
        Path root = configuredRoot == null
                ? StorageRootResolver.resolve()
                : configuredRoot;
        return StorageRootResolver.ensureWritableStorageDirectories(root);
    }

    private void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            // 某些开发文件系统不支持 ATOMIC_MOVE，仍在同一目录内完成无覆盖移动。
            Files.move(source, target);
        }
    }

    private void validateActualSize(MediaType type, long size) throws IOException {
        if (size <= 0) throw new IOException("上传文件为空");
        long max = type == MediaType.ATTACHMENT
                ? UploadPolicy.FILE_MAX_BYTES
                : UploadPolicy.IMAGE_MAX_BYTES;
        if (size > max) throw new IOException("上传文件超过大小限制");
    }

    private String sha256(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (read > 0) digest.update(buffer, 0, read);
            }
            return toHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("JVM 不支持 SHA-256", e);
        }
    }

    private void deleteQuietly(Path path, Throwable original) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException cleanupError) {
            if (original != null) original.addSuppressed(cleanupError);
            LOGGER.error(
                    "[LocalMediaFileStore#deleteQuietly] 清理临时或异常目标文件失败，path={}",
                    path,
                    cleanupError
            );
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            result.append(Character.forDigit((value >>> 4) & 0x0f, 16));
            result.append(Character.forDigit(value & 0x0f, 16));
        }
        return result.toString();
    }

    private String extensionOf(String name) {
        int index = name.lastIndexOf('.');
        if (index < 0 || index == name.length() - 1) return "";
        String extension = name.substring(index).toLowerCase(java.util.Locale.ROOT);
        return extension.length() <= 12 && extension.matches("\\.[a-z0-9]+") ? extension : "";
    }
}
