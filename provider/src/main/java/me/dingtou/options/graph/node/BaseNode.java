package me.dingtou.options.graph.node;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.NodeActionWithConfig;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.graph.common.ContextKeys;
import me.dingtou.options.model.Message;

/**
 * 基础节点
 */
@Slf4j
public abstract class BaseNode implements NodeActionWithConfig {

    @Override
    @SuppressWarnings("unchecked")
    final public Map<String, Object> apply(OverAllState state, RunnableConfig config) throws Exception {
        Optional<Object> callbackMetadata = config.metadata("__callback__");
        Optional<Object> failCallbackMetadata = config.metadata("__fail_callback__");

        state.updateState(Map.of(ContextKeys.NODE_START_TIME, System.currentTimeMillis()));

        Function<Message, Void> callback;
        if (callbackMetadata.isPresent()) {
            callback = (Function<Message, Void>) callbackMetadata.get();
        } else {
            callback = new Function<Message, Void>() {
                @Override
                public Void apply(Message message) {
                    return null;
                }
            };
        }

        Function<Message, Void> failCallback;
        if (failCallbackMetadata.isPresent()) {
            failCallback = (Function<Message, Void>) failCallbackMetadata.get();
        } else {
            failCallback = new Function<Message, Void>() {
                @Override
                public Void apply(Message message) {
                    return null;
                }
            };
        }

        Map<String, Object> result = apply(state, config, callback, failCallback);
        return result;
    }

    /**
     * 执行节点
     * 
     * @param state        状态
     * @param config       配置
     * @param callback     成功回调
     * @param failCallback 失败回调
     * @return 节点输出
     * @throws Exception 执行异常
     */
    public abstract Map<String, Object> apply(OverAllState state,
            RunnableConfig config,
            Function<Message, Void> callback,
            Function<Message, Void> failCallback) throws Exception;

    /**
     * 获取节点名称
     * 
     * @return 节点名称
     */
    protected abstract String name();

}
