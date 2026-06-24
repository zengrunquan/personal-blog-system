package com.blog.util;

import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class UploadFileDownloadUtilTest {

    @Test
    public void resolveDownloadPathShouldStayInsideUploadDirectory() {
        Path uploadDir = Paths.get("build", "uploads", "files").toAbsolutePath().normalize();

        Path resolvedPath = UploadFileDownloadUtil.resolveDownloadPath(uploadDir, "note.txt");

        assertEquals(uploadDir.resolve("note.txt").normalize(), resolvedPath);
        assertTrue("下载路径必须限制在附件上传目录内", resolvedPath.startsWith(uploadDir));
    }

    @Test
    public void resolveDownloadPathShouldRejectUnsafeFileNames() {
        Path uploadDir = Paths.get("build", "uploads", "files").toAbsolutePath().normalize();
        List<String> unsafeFileNames = Arrays.asList(
                null,
                "",
                "   ",
                "../db.properties",
                "nested/a.txt",
                "..\\a.txt",
                "C:\\tmp\\a.txt",
                "/tmp/a.txt"
        );

        for (String fileName : unsafeFileNames) {
            try {
                UploadFileDownloadUtil.resolveDownloadPath(uploadDir, fileName);
                fail("不安全的文件名必须被拒绝: " + fileName);
            } catch (IllegalArgumentException expected) {
                assertTrue("错误消息需要包含调试上下文",
                        expected.getMessage().contains("非法下载文件名"));
            }
        }
    }

    @Test
    public void buildContentDispositionShouldEncodeUtf8Filename() {
        String header = UploadFileDownloadUtil.buildContentDisposition("课程资料.txt");

        assertTrue(header.startsWith("attachment;"));
        assertTrue(header.contains("filename=\""));
        assertTrue(header.contains("filename*=UTF-8''"));
        assertTrue(header.contains("%E8%AF%BE%E7%A8%8B%E8%B5%84%E6%96%99.txt"));
    }
}
