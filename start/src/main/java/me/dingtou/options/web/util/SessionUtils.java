package me.dingtou.options.web.util;

import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.web.model.LoginInfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author qiyan
 */
@Slf4j
public class SessionUtils {
    private static final Map<String, LoginInfo> SESSION_OWNER = new ConcurrentHashMap<>();
    private static final ThreadLocal<String> CURRENT_OWNER = new ThreadLocal<>();

    public static final String DEFAULT_USER = "qiyan";

    /**
     * 获取当前登陆用户
     *
     * @return 获取当前登陆用户
     */
    public static String getCurrentOwner() {
        String currentOwner = CURRENT_OWNER.get();
        return null == currentOwner ? DEFAULT_USER : currentOwner;
    }

    /**
     * 设置当前登陆用户
     *
     * @param owner 当前登陆用户
     */
    public static void setCurrentOwner(String owner) {
        CURRENT_OWNER.set(owner);
    }

    /**
     * 清除当前登陆用户
     */
    public static void clearCurrentOwner() {
        CURRENT_OWNER.remove();
    }


    /**
     * 获取登陆用户
     *
     * @param owner 登陆用户
     * @return 登陆信息
     */
    public static LoginInfo get(String owner) {
        return SESSION_OWNER.get(owner);
    }


    /**
     * 登陆
     *
     * @param loginInfo 登陆信息
     */
    public static void login(LoginInfo loginInfo) {
        SESSION_OWNER.put(loginInfo.getOwner(), loginInfo);
    }


}
