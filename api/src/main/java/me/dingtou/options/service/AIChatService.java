package me.dingtou.options.service;

import me.dingtou.options.model.OwnerChatRecord;
import me.dingtou.options.model.Message;

import java.util.List;
import java.util.function.Function;

/**
 * AI服务
 * 
 * @author qiyan
 */
public interface AIChatService {

    /**
     * 聊天（带标题）
     * 
     * @param owner     所有者
     * @param sessionId 会话ID
     * @param title     标题（股票+策略）
     * @param messages  消息列表
     * @param callback  回调
     */
    void chat(String owner, String sessionId, String title, List<Message> messages, Function<Message, Void> callback);

    /**
     * 获取用户所有会话列表（GROUP BY后的沟通记录）
     *
     * @param owner 所有者
     * @param limit 限制数量
     * @return 会话列表
     */ 
    List<OwnerChatRecord> summaryChatRecord(String owner, int limit);

    /**
     * 根据会话ID获取沟通记录
     *
     * @param owner     所有者
     * @param sessionId 会话ID
     * @return 沟通记录列表
     */
    List<OwnerChatRecord> listRecordsBySessionId(String owner, String sessionId);

    /**
     * 删除会话记录
     *
     * @param owner     所有者
     * @param sessionId 会话ID
     * @return 是否成功
     */
    boolean deleteBySessionId(String owner, String sessionId);

    /**
     * 更新会话标题
     *
     * @param owner     所有者
     * @param sessionId 会话ID
     * @param title     标题
     * @return 是否成功
     */
    boolean updateSessionTitle(String owner, String sessionId, String title);

    /**
     * 更新AI设置
     *
     * @param owner        所有者
     * @param systemPrompt 系统提示词
     * @param temperature  温度参数
     * @return 是否成功
     */
    boolean updateSettings(String owner, String systemPrompt, Double temperature);
}
