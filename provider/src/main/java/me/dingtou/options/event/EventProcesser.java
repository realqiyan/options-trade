package me.dingtou.options.event;

import me.dingtou.options.constant.PushDataType;

/**
 * 事件处理器
 */
public interface EventProcesser {

    /**
     * 支持的事件类型
     * 
     * @return
     */
    PushDataType supportType();

    /**
     * 处理事件
     * 
     * @param event
     */
    void process(AppEvent event);
}
