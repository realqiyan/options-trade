package me.dingtou.options.graph.node;

import java.util.Optional;
import java.util.function.Function;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.NodeActionWithConfig;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.model.Message;

/**
 * 基础节点
 */
@Slf4j
public abstract class BaseNode implements NodeActionWithConfig {

    /**
     * callback
     * 
     * @param state   状态
     * @param config  配置
     * @param message 消息
     */
    protected void callback(OverAllState state, RunnableConfig config, Message message) {
        log.info("node:{} message:{}", name(), message);
        Optional<Object> metadata = config.metadata("__callback__");
        if (metadata.isPresent()) {
            @SuppressWarnings("unchecked")
            Function<Message, Void> callback = (Function<Message, Void>) metadata.get();
            callback.apply(message);
        } else {
            log.info("__callback__ 未配置, 忽略消息: {}", message);
        }

    }

    /**
     * 获取节点名称
     * 
     * @return 节点名称
     */
    protected abstract String name();

}
