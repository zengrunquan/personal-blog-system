package com.blog.controller;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class ArticleDownloadEndpointTest {

    private static final Path ARTICLE_SERVLET =
            Paths.get("src/main/java/com/blog/controller/ArticleServlet.java");

    @Test
    public void articleServletShouldExposeDownloadGetEndpoint() throws Exception {
        String source = Files.readString(ARTICLE_SERVLET);
        int doGetIndex = source.indexOf("protected void doGet");
        int switchIndex = source.indexOf("switch (pathInfo)", doGetIndex);
        int downloadCaseIndex = source.indexOf("case \"/download\"", switchIndex);
        int downloadCallIndex = source.indexOf("downloadFile(request, response)", downloadCaseIndex);

        assertTrue("ArticleServlet#doGet 必须暴露 /article/download 下载入口",
                doGetIndex >= 0 && downloadCaseIndex > switchIndex);
        assertTrue("下载入口必须分发到 downloadFile 方法",
                downloadCallIndex > downloadCaseIndex);
    }
}
