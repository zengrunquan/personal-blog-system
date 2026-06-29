package com.blog;

import com.blog.util.PasswordUtil;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PasswordStorageDocumentationTest {

    private static final Path INIT_DATABASE_PATH = Paths.get("init-database.sql");
    private static final Path README_PATH = Paths.get("README.md");
    private static final Pattern TEST_USER_PATTERN = Pattern.compile(
            "\\('(admin|zhangsan|lisi)',\\s*'([^']+)'"
    );

    @Test
    public void initDatabaseShouldStoreVerifiableBcryptHashes() throws Exception {
        String sql = Files.readString(INIT_DATABASE_PATH);
        Matcher matcher = TEST_USER_PATTERN.matcher(sql);
        Map<String, String> hashes = new HashMap<>();
        while (matcher.find()) {
            hashes.put(matcher.group(1), matcher.group(2));
        }

        assertEquals("初始化 SQL 必须包含三个测试账号", 3, hashes.size());
        assertBcryptPassword(hashes.get("admin"), "admin123");
        assertBcryptPassword(hashes.get("zhangsan"), "user123");
        assertBcryptPassword(hashes.get("lisi"), "user123");
        assertFalse("初始化 SQL 的密码字段注释不能继续声明为 MD5",
                sql.contains("密码（MD5加密）"));
    }

    @Test
    public void readmeShouldDescribeBcryptAndLegacyUpgrade() throws Exception {
        String readme = Files.readString(README_PATH);

        assertTrue("README 必须说明 BCrypt 密码哈希", readme.contains("BCrypt"));
        assertTrue("README 必须说明旧 MD5 登录后自动升级",
                readme.contains("旧 MD5") && readme.contains("自动升级"));
        assertFalse("README 不应继续把 MD5 描述为当前密码存储方案",
                readme.contains("- 密码MD5加密"));
    }

    private void assertBcryptPassword(String passwordHash, String plainPassword) {
        assertTrue("测试账号必须使用成本因子 12 的 BCrypt 哈希",
                passwordHash != null && passwordHash.startsWith("$2a$12$"));
        assertTrue("README 中的测试密码必须能通过初始化哈希校验",
                PasswordUtil.verify(plainPassword, passwordHash));
        assertFalse(PasswordUtil.needsRehash(passwordHash));
    }
}
