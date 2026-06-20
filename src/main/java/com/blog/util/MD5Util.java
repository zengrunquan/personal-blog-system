package com.blog.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5加密工具类
 * 用于密码加密存储
 *
 * @author blog-system
 */
public class MD5Util {

    /**
     * 对字符串进行MD5加密
     *
     * @param source 原始字符串
     * @return 加密后的32位十六进制字符串
     */
    public static String md5(String source) {
        if (source == null || source.isEmpty()) {
            return "";
        }

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(source.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                // 将每个字节转换为两位十六进制数
                String hex = Integer.toHexString(b & 0xFF);
                if (hex.length() == 1) {
                    sb.append("0");
                }
                sb.append(hex);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5加密失败", e);
        }
    }

    /**
     * 验证密码是否正确
     *
     * @param inputPassword    用户输入的密码
     * @param encryptedPassword 数据库中加密后的密码
     * @return 密码是否匹配
     */
    public static boolean verify(String inputPassword, String encryptedPassword) {
        if (inputPassword == null || encryptedPassword == null) {
            return false;
        }
        return md5(inputPassword).equals(encryptedPassword);
    }

    /**
     * 测试方法
     */
    public static void main(String[] args) {
        System.out.println("admin123 加密后: " + md5("admin123"));
        System.out.println("user123 加密后: " + md5("user123"));
    }
}
