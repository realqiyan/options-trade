package me.dingtou.options.service;

/**
 * 认证服务
 *
 * @author qiyan
 */
public interface AuthService {

    /**
     * 认证服务
     *
     * @param owner       用户
     * @param otpPassword otp 密码
     * @return true or false
     */
    Boolean auth(String owner, String otpPassword);


    /**
     * 获取账号密钥Sha256
     *
     * @param owner 用户
     * @return 密钥Sha256
     */
    String secretKeySha256(String owner);
}
