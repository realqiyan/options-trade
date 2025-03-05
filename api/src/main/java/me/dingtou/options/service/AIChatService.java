package me.dingtou.options.service;

import me.dingtou.options.model.Message;

import java.util.function.Function;

/**
 * AI服务
 * 
 * @author qiyan
 */
public interface AIChatService {

    /**
     * 聊天
     * 
     * @param message  消息
     * @param callback 回调
     */
    void chat(String message, Function<Message, Void> callback);

}
