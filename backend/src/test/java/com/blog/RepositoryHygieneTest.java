package com.blog;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RepositoryHygieneTest {

    private static final Path GITIGNORE_PATH = Paths.get("..", ".gitignore");
    private static final Path README_PATH = Paths.get("..", "README.md");

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
        assertTrue("README 必须说明项目 docs 上传目录不提交", readme.contains("docs/uploads/"));
    }

    @Test
    public void localSensitiveFilesShouldBeIgnoredButTemplateShouldRemainVersionable() throws Exception {
        assertTrue("真实数据库配置必须被 Git 忽略", isIgnored("backend/src/main/resources/db.properties"));
        assertTrue("旧目录中的真实数据库配置也必须被 Git 忽略", isIgnored("src/main/resources/db.properties"));
        assertTrue("数据库备份必须被 Git 忽略", isIgnored("database/backups/example.sql"));
        assertTrue("本机视觉验收记录必须被 Git 忽略", isIgnored("design-qa.md"));
        assertTrue("前端本地环境变量必须被 Git 忽略", isIgnored("frontend/.env.local"));
        assertTrue("私有附件目录必须被 Git 忽略",
                isIgnored("backend/src/main/webapp/WEB-INF/private-uploads/example.html"));
        assertTrue("项目 docs 上传目录必须被 Git 忽略",
                isIgnored("docs/uploads/example.jpg"));
        assertFalse("脱敏后的数据库配置示例必须允许提交", isIgnored("backend/src/main/resources/db.properties.example"));
        assertFalse("环境变量示例必须允许提交", isIgnored("frontend/.env.example"));
    }

    private boolean isIgnored(String repositoryPath) throws Exception {
        Path repositoryRoot = Files.exists(Paths.get(".git")) ? Paths.get(".") : Paths.get("..");
        Process process = new ProcessBuilder(
                "git", "-C", repositoryRoot.toAbsolutePath().normalize().toString(),
                "check-ignore", "--no-index", "--quiet", "--", repositoryPath
        ).redirectErrorStream(true).start();
        int exitCode = process.waitFor();
        if (exitCode != 0 && exitCode != 1) {
            throw new IllegalStateException("无法检查 Git 忽略规则，exitCode=" + exitCode);
        }
        return exitCode == 0;
    }
}
