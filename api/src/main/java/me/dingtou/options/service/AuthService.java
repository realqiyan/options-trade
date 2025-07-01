package me.dingtou.options.service;

import java.util.Date;

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

    /**
     * 获取账号JWT
     *
     * @param owner      用户
     * @param expireDate 失效时间
     * @return jwt
     */
    String jwt(String owner, Date expireDate);

    /**
     * 获取账号的动态加密编码 （一天内有效）
     *
     * @param owner 用户
     * @return 账号的动态加密编码
     */
    String encodeOwner(String owner);

    /**
     * 解密账号的动态加密编码
     *
     * @param ownerCode 账号的动态加密编码
     * @return 用户
     */
    String decodeOwner(String ownerCode);
}
