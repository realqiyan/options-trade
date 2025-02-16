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

    public LoginInfo(String owner, String otpPassword) {
        this.owner = owner;
        this.otpPassword = otpPassword;
        this.loginTime = new Date();
    }
}
