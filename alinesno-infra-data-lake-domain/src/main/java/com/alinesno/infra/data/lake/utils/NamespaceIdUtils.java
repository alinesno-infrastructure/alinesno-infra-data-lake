package com.alinesno.infra.data.lake.utils;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * 生成命名空间ID
 */
public class NamespaceIdUtils {

    // Base62 字符集：数字 + 大写字母 + 小写字母
    private static final char[] BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();
    // 固定长度 8
    private static final int TARGET_LENGTH = 8;

    /**
     * 获取命名空间ID，由数字和大小写字母组成，固定长度为 8
     * @return 由 0-9 A-Z a-z 组成的唯一 ID（尽力唯一，实际唯一性依赖于随机性）
     */
    public static String getId() {
        // 使用 48 位随机数（6 字节），转换为 Base62，长度补齐到 8。
        byte[] randomBytes = new byte[6]; // 6 字节 = 48 位
        RANDOM.nextBytes(randomBytes);

        BigInteger bi = new BigInteger(1, randomBytes);
        String base62 = toBase62(bi);

        // 补齐到固定长度（前面填充 '0' 字符）
        if (base62.length() < TARGET_LENGTH) {
            StringBuilder sb = new StringBuilder(TARGET_LENGTH);
            for (int i = base62.length(); i < TARGET_LENGTH; i++) {
                sb.append('0');
            }
            sb.append(base62);
            return sb.toString();
        } else if (base62.length() > TARGET_LENGTH) {
            // 极少情况（理论上 48 位转 Base62 不会超过 8，但为保险起见截断高位）
            return base62.substring(base62.length() - TARGET_LENGTH);
        } else {
            return base62;
        }
    }

    private static String toBase62(BigInteger value) {
        if (value == null || value.equals(BigInteger.ZERO)) {
            return "0";
        }

        StringBuilder sb = new StringBuilder();
        BigInteger base = BigInteger.valueOf(62);
        while (value.compareTo(BigInteger.ZERO) > 0) {
            BigInteger[] divRem = value.divideAndRemainder(base);
            value = divRem[0];
            int digit = divRem[1].intValue();
            sb.append(BASE62[digit]);
        }
        return sb.reverse().toString();
    }

    // 简单测试
    public static void main(String[] args) {
        for (int i = 0; i < 10; i++) {
            System.out.println(getId());
        }
    }
}