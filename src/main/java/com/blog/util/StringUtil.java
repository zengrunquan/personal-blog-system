package com.blog.util;

/**
 * 字符串工具类
 * 提供字符串处理的常用方法
 *
 * @author blog-system
 */
public class StringUtil {

    /**
     * 判断字符串是否为空或空白
     *
     * @param str 字符串
     * @return 是否为空
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * 判断字符串是否不为空
     *
     * @param str 字符串
     * @return 是否不为空
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * XSS防护：转义HTML特殊字符
     * 防止XSS攻击
     *
     * @param input 输入字符串
     * @return 转义后的字符串
     */
    public static String escapeHtml(String input) {
        if (input == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '&':
                    sb.append("&amp;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                case '\'':
                    sb.append("&#39;");
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        return sb.toString();
    }

    /**
     * 截取字符串（支持中文）
     *
     * @param str    原字符串
     * @param maxLength 最大长度
     * @return 截取后的字符串
     */
    public static String truncate(String str, int maxLength) {
        if (str == null) {
            return "";
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "...";
    }

    /**
     * 生成CSRF Token
     *
     * @return 随机Token字符串
     */
    public static String generateToken() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }
}
