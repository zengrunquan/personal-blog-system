package com.blog.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

/**
 * 旧版MD5密码兼容工具。
 * 仅用于验证历史账号，以便登录成功后迁移到BCrypt，不允许用于新密码存储。
 */
final class MD5Util {

    private static final Logger LOGGER = LogManager.getLogger(MD5Util.class);
    private static final Pattern LEGACY_MD5_PATTERN = Pattern.compile("^[a-fA-F0-9]{32}$");

    private MD5Util() {
    }

    static boolean isLegacyHash(String passwordHash) {
        return passwordHash != null && LEGACY_MD5_PATTERN.matcher(passwordHash).matches();
    }

    static boolean verify(String inputPassword, String passwordHash) {
        if (inputPassword == null || !isLegacyHash(passwordHash)) {
            return false;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] actualHash = md.digest(inputPassword.getBytes(StandardCharsets.UTF_8));
            byte[] expectedHash = hexToBytes(passwordHash);
            return MessageDigest.isEqual(actualHash, expectedHash);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("[MD5Util#verify] 无法验证旧版MD5密码，JVM缺少MD5算法", e);
            throw new IllegalStateException("旧版MD5密码验证失败", e);
        }
    }

    private static byte[] hexToBytes(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            bytes[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
        }
        return bytes;
    }
}
