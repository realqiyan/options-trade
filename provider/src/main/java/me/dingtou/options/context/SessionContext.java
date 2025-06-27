package me.dingtou.options.context;

/**
 * 会话上下文 特殊情况获取当前登录用户
 *
 * @author qiyan
 */
public class SessionContext {

    /**
     * 记录当前用户
     */
    private static final ThreadLocal<String> OWNER = new ThreadLocal<>();

    /**
     * 设置当前用户
     * 
     * @param owner 当前用户
     */
    public static void setOwner(String owner) {
        OWNER.set(owner);
    }

    /**
     * 清理当前用户
     */
    public static void clearOwner() {
        OWNER.remove();
    }

    /**
     * 获取当前用户
     * 
     * @return 当前用户
     */
    public static String getOwner() {
        String owner = OWNER.get();
        if (null == owner) {
            throw new RuntimeException("未认证用户");
        }
        return owner;
    }
}
