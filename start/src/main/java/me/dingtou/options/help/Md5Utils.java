package me.dingtou.options.help;

import java.security.MessageDigest;

public class Md5Utils {

    /**
     * 字符串转md5
     *
     * @param input 输入字符串
     * @return 输入字符串计算的MD5
     */
    public static String getMD5Hash(String input) {
        try {
            // 获取MD5消息摘要实例
            MessageDigest md = MessageDigest.getInstance("MD5");

            // 计算输入字符串的MD5哈希值
            byte[] messageDigest = md.digest(input.getBytes("UTF-8"));

            // 将字节数组转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        System.out.println(getMD5Hash("123456"));
    }
}
