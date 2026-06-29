package com.blog.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mindrot.jbcrypt.BCrypt;

/**
 * 密码哈希与校验工具。
 */
public final class PasswordUtil {

    private static final Logger LOGGER = LogManager.getLogger(PasswordUtil.class);
    private static final int BCRYPT_COST = 12;
    private static final String BCRYPT_PREFIX = "$2a$";

    private PasswordUtil() {
    }

    /**
     * 使用随机盐生成BCrypt密码哈希。
     *
     * @param password 明文密码
     * @return BCrypt哈希
     */
    public static String hash(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("密码不能为空，无法生成安全哈希");
        }

        try {
            return BCrypt.hashpw(password, BCrypt.gensalt(BCRYPT_COST));
        } catch (IllegalArgumentException e) {
            LOGGER.error(
                    "[PasswordUtil#hash] BCrypt密码哈希生成失败，passwordLength={}",
                    password.length(),
                    e
            );
            throw new IllegalStateException("BCrypt密码哈希生成失败", e);
        }
    }

    /**
     * 校验明文密码，兼容登录时迁移的旧版MD5哈希。
     *
     * @param password 明文密码
     * @param passwordHash 已存储的密码哈希
     * @return 是否匹配
     */
    public static boolean verify(String password, String passwordHash) {
        if (password == null || password.isEmpty() || passwordHash == null || passwordHash.isEmpty()) {
            return false;
        }

        if (MD5Util.isLegacyHash(passwordHash)) {
            return MD5Util.verify(password, passwordHash);
        }

        if (!passwordHash.startsWith(BCRYPT_PREFIX)) {
            LOGGER.error(
                    "[PasswordUtil#verify] 无法识别密码哈希格式，hashLength={}",
                    passwordHash.length()
            );
            return false;
        }

        try {
            return BCrypt.checkpw(password, passwordHash);
        } catch (IllegalArgumentException e) {
            LOGGER.error(
                    "[PasswordUtil#verify] BCrypt密码哈希格式损坏，hashLength={}",
                    passwordHash.length(),
                    e
            );
            return false;
        }
    }

    /**
     * 判断登录成功后是否需要把旧密码哈希升级为BCrypt。
     *
     * @param passwordHash 已存储的密码哈希
     * @return 旧版MD5哈希返回true
     */
    public static boolean needsRehash(String passwordHash) {
        return MD5Util.isLegacyHash(passwordHash);
    }
}
