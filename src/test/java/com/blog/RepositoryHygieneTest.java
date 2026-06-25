package com.blog;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class RepositoryHygieneTest {

    private static final Path GITIGNORE_PATH = Paths.get(".gitignore");
    private static final Path README_PATH = Paths.get("README.md");

    @Test
    public void gitignoreShouldExcludeBuildIdeClassAndRuntimeUploadFiles() throws Exception {
        String gitignore = Files.readString(GITIGNORE_PATH);

        assertTrue("必须忽略 Maven 编译产物目录 target/", gitignore.contains("target/"));
        assertTrue("必须忽略 Java class 编译产物", gitignore.contains("*.class"));
        assertTrue("必须忽略 IDEA 工程配置目录", gitignore.contains(".idea/"));
        assertTrue("必须忽略 IDEA 模块文件", gitignore.contains("*.iml"));
        assertTrue("必须忽略运行时上传文件目录", gitignore.contains("src/main/webapp/uploads/"));
    }

    @Test
    public void readmeShouldDocumentRepositoryHygieneRules() throws Exception {
        String readme = Files.readString(README_PATH);

        assertTrue("README 必须说明 IDE 配置目录不提交", readme.contains(".idea/"));
        assertTrue("README 必须说明 Maven 构建产物不提交", readme.contains("target/"));
        assertTrue("README 必须说明 class 文件不提交", readme.contains("*.class"));
        assertTrue("README 必须说明运行时上传文件不提交", readme.contains("src/main/webapp/uploads/"));
    }
}
