package me.dingtou.options.service;

import me.dingtou.options.model.OwnerChatRecord;
import java.util.List;
import java.util.Map;

/**
 * AI服务
 * 
 * @author qiyan
 */
public interface AssistantService {

    /**
     * 获取AI设置
     * 
     * @param owner 所有者
     * @return 设置键值对（mcpSettings, temperature）
     */
    Map<String, Object> getSettings(String owner);

    /**
     * 获取用户所有会话列表（GROUP BY后的沟通记录）
     *
     * @param owner 所有者
     * @param limit 限制数量
     * @return 会话列表
     */
    List<OwnerChatRecord> summaryChatRecord(String owner, int limit);

    /**
     * 根据会话ID获取沟通记录(user & assistant)
     *
     * @param owner     所有者
     * @param sessionId 会话ID
     * @return 沟通记录列表
     */
    List<OwnerChatRecord> listRecordsBySessionId(String owner, String sessionId);

    /**
     * 根据消息ID获取沟通记录
     * 
     * @param owner     所有者
     * @param messageId 消息ID
     * @return 沟通记录
     */
    OwnerChatRecord getRecordByMessageId(String owner, String messageId);

    /**
     * 添加沟通记录
     *
     * @param owner     所有者
     * @param sessionId 会话ID
     * @param record    消息记录
     * @return 添加结果
     */
    Boolean addChatRecord(String owner, String sessionId, OwnerChatRecord record);

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
     * @param owner       所有者
     * @param mcpSettings MCP服务器配置
     * @param temperature 温度参数
     * @return 是否成功
     */
    boolean updateSettings(String owner, String mcpSettings, Double temperature);
}
