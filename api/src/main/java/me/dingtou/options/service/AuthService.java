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
     * @param owner       用户信息
     * @param otpPassword otp 密码
     * @return true or false
     */
    Boolean auth(String owner, String otpPassword);
}
