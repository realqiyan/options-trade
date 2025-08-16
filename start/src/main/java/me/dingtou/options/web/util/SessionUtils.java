package me.dingtou.options.web.util;

import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.web.model.LoginInfo;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author qiyan
 */
@Slf4j
@Component
public class SessionUtils {
    private static final Map<String, LoginInfo> SESSION_OWNER = new ConcurrentHashMap<>();
    private static final ThreadLocal<String> CURRENT_OWNER = new ThreadLocal<>();
    private static final Map<String, SseEmitter> SSE_EMITTER_MAP = new ConcurrentHashMap<>();

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

    /**
     * 创建sse连接
     *
     * @param owner     登陆用户
     * @param requestId 请求id
     * @return SseEmitter
     */
    public static SseEmitter connect(String owner, String requestId) {
        log.info("connect, owner:{} requestId:{}", owner, requestId);
        SseEmitter sseemitter = new SseEmitter(24 * 60 * 60 * 1000L);
        sseemitter.onCompletion(onCompletion(owner, requestId));
        sseemitter.onError(onError(owner, requestId));
        sseemitter.onTimeout(onTimeout(owner, requestId));
        SSE_EMITTER_MAP.put(buildUniqueKey(owner, requestId), sseemitter);
        return sseemitter;
    }

    /**
     * 获取sse连接
     *
     * @param owner     登陆用户
     * @param requestId 请求id
     * @return SseEmitter
     */
    public static SseEmitter getConnect(String owner, String requestId) {
        return SSE_EMITTER_MAP.get(buildUniqueKey(owner, requestId));
    }

    /**
     * 关闭sse连接
     *
     * @param owner     登陆用户
     * @param requestId 请求id
     */
    public static void close(String owner, String requestId) {
        log.info("close, owner:{} requestId:{}", owner, requestId);
        SSE_EMITTER_MAP.remove(buildUniqueKey(owner, requestId));
    }

    private static String buildUniqueKey(String owner, String requestId) {
        return owner + ":" + requestId;
    }

    private static Runnable onCompletion(String owner, String requestId) {
        return () -> {
            log.info("onCompletion, owner:{} requestId:{}", owner, requestId);
            close(owner, requestId);
        };
    }

    private static Runnable onTimeout(String owner, String requestId) {
        return () -> {
            log.info("onTimeout, owner:{} requestId:{}", owner, requestId);
            close(owner, requestId);
        };
    }

    private static Consumer<Throwable> onError(String owner, String requestId) {
        return throwable -> {
            log.info("onError, owner:{} requestId:{}", owner, requestId);
            close(owner, requestId);
        };
    }

    /**
     * ping客户端 定时清理已关闭的SseEmitter连接
     */
    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.MINUTES)
    public void ping() {
        SSE_EMITTER_MAP.entrySet().removeIf(entry -> {
            SseEmitter emitter = entry.getValue();
            if (emitter != null) {
                // 检查连接是否已经关闭
                try {
                    emitter.send(SseEmitter.event().comment("ping"));
                    return false; // 连接仍然有效
                } catch (Exception e) {
                    log.info("发现已关闭的连接: {}", entry.getKey());
                    return true; // 连接已关闭，需要移除
                }
            }
            return true; // emitter为null，需要移除
        });
    }

}
