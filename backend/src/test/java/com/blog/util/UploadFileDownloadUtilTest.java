package com.blog.util;

import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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

    @Test
    public void headerShouldBeAsciiAndPreserveUtf8Name() {
        String header = UploadFileDownloadUtil
                .buildContentDisposition("课程资料.txt");

        assertTrue(header.chars().allMatch(c -> c >= 32 && c <= 126));
        assertTrue(header.contains(
                "filename*=UTF-8''%E8%AF%BE%E7%A8%8B%E8%B5%84%E6%96%99.txt"
        ));
    }

    @Test
    public void headerShouldPreserveLiteralPlusAndPercent() {
        String header = UploadFileDownloadUtil
                .buildContentDisposition("A+B 100%.txt");

        assertTrue(header.contains("filename*=UTF-8''A%2BB%20100%25.txt"));
    }

    @Test
    public void nameShouldRemoveClientPathAndControlCharacters() {
        assertEquals("课程资料.pdf",
                UploadFileDownloadUtil.sanitizeDownloadName(
                        "C:\\fakepath\\课程资料.pdf", "fallback.pdf"));

        assertEquals("a__b.txt",
                UploadFileDownloadUtil.sanitizeDownloadName(
                        "a\r\nb.txt", "fallback.txt"));

        assertEquals("fallback.pdf",
                UploadFileDownloadUtil.sanitizeDownloadName(
                        "   ", "fallback.pdf"));
    }

    @Test
    public void nameShouldKeepSafeCharactersAndUseFallbackForEmptyBasename() {
        assertEquals("archive.tar.gz",
                UploadFileDownloadUtil.sanitizeDownloadName(
                        "archive.tar.gz", "fallback"));
        assertEquals("Emoji 📎.txt",
                UploadFileDownloadUtil.sanitizeDownloadName(
                        "Emoji 📎.txt", "fallback"));
        assertEquals("A+B 100%.txt",
                UploadFileDownloadUtil.sanitizeDownloadName(
                        "A+B 100%.txt", "fallback"));
        assertEquals("a_b_c_d_e_f__.txt",
                UploadFileDownloadUtil.sanitizeDownloadName(
                        "a\"b<c>d:e|f?*.txt", "fallback"));
        assertEquals("fallback",
                UploadFileDownloadUtil.sanitizeDownloadName(
                        "C:\\fakepath\\", "fallback"));
        assertEquals("fallback",
                UploadFileDownloadUtil.sanitizeDownloadName(null, "fallback"));
        assertEquals("fallback",
                UploadFileDownloadUtil.sanitizeDownloadName("..", "fallback"));
    }

    @Test
    public void headerShouldNotContainRawUnsafeFilenameCharacters() {
        String header = UploadFileDownloadUtil.buildContentDisposition(
                "C:\\fakepath\\课程\"资料?.txt");

        assertFalse(header.contains("C:"));
        assertFalse(header.contains("fakepath"));
        assertTrue(header.contains("%E8%AF%BE%E7%A8%8B_%E8%B5%84%E6%96%99_.txt"));
    }
}
