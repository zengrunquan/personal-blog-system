package com.blog.api;

import com.blog.api.upload.UploadStorage;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UploadStorageTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void configuredStorageDirectoryShouldBeCreatedAutomatically() throws Exception {
        Path configured = temporaryFolder.getRoot().toPath().resolve("nested/docs/uploads");

        Path actual = withStorageDirectory(configured, UploadStorage::storageDirectory);

        assertEquals(configured.toAbsolutePath().normalize(), actual);
        assertTrue("配置的上传目录不存在时必须自动创建", Files.isDirectory(actual));
        assertTrue("图片子目录不存在时必须自动创建", Files.isDirectory(actual.resolve("image")));
        assertTrue("附件子目录不存在时必须自动创建", Files.isDirectory(actual.resolve("file")));
    }

    @Test
    public void avatarShouldBeWrittenIntoImageDirectory() throws Exception {
        Path configured = temporaryFolder.getRoot().toPath().resolve("docs/uploads");
        byte[] content = "avatar-content".getBytes(StandardCharsets.UTF_8);
        Part part = writablePart("头像.jpg", "image/jpeg", content);

        UploadStorage.UploadResult result = withStorageDirectory(
                configured,
                () -> UploadStorage.saveImage(part, "avatars", request())
        );

        Path storedFile = configured.resolve("image").resolve(result.storedName);
        assertTrue(result.storedName.startsWith("avatar_"));
        assertEquals(configured.resolve("image").toAbsolutePath().normalize(), storedFile.getParent());
        assertArrayEquals(content, Files.readAllBytes(storedFile));
        assertEquals(
                "/app/uploads/avatars/" + result.storedName.substring("avatar_".length()),
                result.url
        );
    }

    @Test
    public void attachmentShouldBeWrittenIntoFileDirectory() throws Exception {
        Path configured = temporaryFolder.getRoot().toPath().resolve("docs/uploads");
        byte[] content = "attachment-content".getBytes(StandardCharsets.UTF_8);
        Part part = writablePart("说明.pdf", "application/pdf", content);

        UploadStorage.UploadResult result = withStorageDirectory(
                configured,
                () -> UploadStorage.saveAttachment(part, request())
        );

        Path storedFile = configured.resolve("file").resolve(result.storedName);
        assertTrue(result.storedName.startsWith("file_"));
        assertEquals(configured.resolve("file").toAbsolutePath().normalize(), storedFile.getParent());
        assertArrayEquals(content, Files.readAllBytes(storedFile));
        String urlFileName = result.storedName.substring("file_".length());
        assertEquals("/app/api/files/" + urlFileName + "/download", result.url);
        assertEquals(storedFile, withStorageDirectory(
                configured,
                () -> UploadStorage.resolveAttachmentFile(urlFileName)
        ));
    }

    @Test
    public void articleImageShouldUseImagePrefixOnlyOnPhysicalFile() throws Exception {
        Path configured = temporaryFolder.getRoot().toPath().resolve("docs/uploads");
        Part part = writablePart("正文.png", "image/png", "image".getBytes(StandardCharsets.UTF_8));

        UploadStorage.UploadResult result = withStorageDirectory(
                configured,
                () -> UploadStorage.saveImage(part, "images", request())
        );

        assertTrue(result.storedName.startsWith("image_"));
        assertTrue(Files.isRegularFile(configured.resolve("image").resolve(result.storedName)));
        assertEquals(
                "/app/uploads/images/" + result.storedName.substring("image_".length()),
                result.url
        );
    }

    @Test
    public void existingRegularFileShouldBeReportedAsInvalidStorageDirectory() throws Exception {
        Path configured = temporaryFolder.newFile("uploads").toPath();

        Exception error = assertThrows(
                java.io.IOException.class,
                () -> withStorageDirectory(configured, UploadStorage::storageDirectory)
        );

        assertTrue(error.getMessage().contains("不是目录"));
        assertTrue(error.getMessage().contains(configured.toString()));
    }

    @Test
    public void regularFileOccupyingImageDirectoryShouldBeReportedClearly() throws Exception {
        Path configured = temporaryFolder.newFolder("uploads-with-invalid-image").toPath();
        Path invalidImageDirectory = Files.createFile(configured.resolve("image"));

        Exception error = assertThrows(
                java.io.IOException.class,
                () -> withStorageDirectory(configured, UploadStorage::storageDirectory)
        );

        assertTrue(error.getMessage().contains("不是目录"));
        assertTrue(error.getMessage().contains(invalidImageDirectory.toString()));
    }

    @Test
    public void invalidConfiguredPathShouldBeReportedAsStorageConfigurationError() {
        Exception error = assertThrows(
                java.io.IOException.class,
                () -> withStorageDirectory(Path.of("."), () -> {
                    System.setProperty(UploadStorage.STORAGE_DIRECTORY_PROPERTY, "invalid\u0000path");
                    return UploadStorage.storageDirectory();
                })
        );

        assertTrue(error.getMessage().contains("路径无效"));
    }

    @Test
    public void attachmentUrlFileNameWithHeaderControlCharactersShouldBeRejected() throws Exception {
        Path configured = temporaryFolder.getRoot().toPath().resolve("docs/uploads");

        IllegalArgumentException controlCharacterError = assertThrows(
                IllegalArgumentException.class,
                () -> withStorageDirectory(
                configured,
                () -> UploadStorage.resolveAttachmentFile("safe.pdf\r\nX-Test")
        ));
        IllegalArgumentException quoteError = assertThrows(
                IllegalArgumentException.class,
                () -> withStorageDirectory(
                configured,
                () -> UploadStorage.resolveAttachmentFile("\"quoted.pdf\"")
        ));

        assertTrue("控制字符必须由业务校验拒绝，不能依赖操作系统路径规则",
                !(controlCharacterError instanceof InvalidPathException));
        assertTrue("引号必须由业务校验拒绝，不能依赖操作系统路径规则",
                !(quoteError instanceof InvalidPathException));
    }

    private Part writablePart(String originalName, String contentType, byte[] content) throws Exception {
        Part part = mock(Part.class);
        when(part.getSubmittedFileName()).thenReturn(originalName);
        when(part.getContentType()).thenReturn(contentType);
        when(part.getSize()).thenReturn((long) content.length);
        doAnswer(invocation -> {
            Files.write(Path.of(invocation.getArgument(0, String.class)), content);
            return null;
        }).when(part).write(anyString());
        return part;
    }

    private HttpServletRequest request() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn("/app");
        return request;
    }

    private <T> T withStorageDirectory(Path directory, CheckedSupplier<T> action) throws Exception {
        String previous = System.getProperty(UploadStorage.STORAGE_DIRECTORY_PROPERTY);
        System.setProperty(UploadStorage.STORAGE_DIRECTORY_PROPERTY, directory.toString());
        try {
            return action.get();
        } finally {
            if (previous == null) {
                System.clearProperty(UploadStorage.STORAGE_DIRECTORY_PROPERTY);
            } else {
                System.setProperty(UploadStorage.STORAGE_DIRECTORY_PROPERTY, previous);
            }
        }
    }

    @FunctionalInterface
    private interface CheckedSupplier<T> {
        T get() throws Exception;
    }
}
