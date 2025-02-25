package me.dingtou.options.web.model;

import lombok.Data;

import java.util.Date;

/**
 * 登陆信息
 *
 * @author qiyan
 */
@Data
public class LoginInfo {

    public static final String JWT = "jwt";
    public static final String OTP = "otp";

    /**
     * 登录类型
     */
    private final String type;

    /**
     * 登录用户
     */
    private final String owner;

    /**
     * 登录otp密码
     */
    private final String otpPassword;

    /**
     * 登录时间
     */
    private final Date loginTime;

    public LoginInfo(String owner) {
        this.owner = owner;
        this.type = LoginInfo.JWT;
        this.otpPassword = null;
        this.loginTime = new Date();
    }

    public LoginInfo(String owner, String otpPassword) {
        this.owner = owner;
        this.type = LoginInfo.OTP;
        this.otpPassword = otpPassword;
        this.loginTime = new Date();
    }
}
