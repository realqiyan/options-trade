package me.dingtou.options.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;

import lombok.extern.slf4j.Slf4j;

/**
 * 基础节点
 */
@Slf4j
public abstract class BaseNode implements NodeAction {

    /**
     * 记录消息日志
     * 
     * @param state   状态
     * @param message 消息
     */
    protected void log(OverAllState state, String message) {
        log.info("{}: {}", name(), message);
    }

    /**
     * 获取节点名称
     * 
     * @return 节点名称
     */
    protected abstract String name();

}
