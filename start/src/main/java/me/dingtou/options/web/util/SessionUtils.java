package me.dingtou.options.web.util;

import lombok.extern.slf4j.Slf4j;

/**
 * @author qiyan
 */
@Slf4j
public class SessionUtils {


    public static final String DEFAULT_USER = "qiyan";

    /**
     * 获取当前登陆用户
     *
     * @return 获取当前登陆用户
     */
    public static String getCurrentOwner() {
        // 扩展登录模块
        return DEFAULT_USER;
    }


}
