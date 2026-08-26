package com.blog.util;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PasswordUtilTest {

    @Test
    public void hashShouldUseRandomSaltAndVerifyPassword() {
        String firstHash = PasswordUtil.hash("StrongPass123!");
        String secondHash = PasswordUtil.hash("StrongPass123!");

        assertNotEquals("相同密码必须生成不同哈希，避免复用固定盐", firstHash, secondHash);
        assertTrue("BCrypt 哈希必须使用成本因子 12", firstHash.startsWith("$2a$12$"));
        assertTrue(PasswordUtil.verify("StrongPass123!", firstHash));
        assertTrue(PasswordUtil.verify("StrongPass123!", secondHash));
        assertFalse(PasswordUtil.needsRehash(firstHash));
    }

    @Test
    public void verifyShouldRejectWrongEmptyAndMalformedValues() {
        String passwordHash = PasswordUtil.hash("StrongPass123!");

        assertFalse(PasswordUtil.verify("WrongPass123!", passwordHash));
        assertFalse(PasswordUtil.verify(null, passwordHash));
        assertFalse(PasswordUtil.verify("", passwordHash));
        assertFalse(PasswordUtil.verify("StrongPass123!", null));
        assertFalse(PasswordUtil.verify("StrongPass123!", "invalid-password-hash"));
    }

    @Test
    public void legacyMd5ShouldVerifyWithUtf8AndRequireRehash() {
        String legacyHash = "0192023a7bbd73250516f069df18b500";

        assertTrue(PasswordUtil.verify("admin123", legacyHash));
        assertFalse(PasswordUtil.verify("wrong-password", legacyHash));
        assertTrue(PasswordUtil.needsRehash(legacyHash));
    }

    @Test
    public void hashShouldRejectEmptyPasswordWithMeaningfulMessage() {
        try {
            PasswordUtil.hash("");
            fail("空密码不允许生成哈希");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("密码不能为空"));
        }
    }
}
