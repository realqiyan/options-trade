package me.dingtou.options.model;


import lombok.Data;

import java.util.Date;

/**
 * 所有者沟通记录
 *
 * @author qiyan
 */
@Data
public class OwnerChatRecord {
    /**
     * 记录ID
     */
    private Long id;
    
    /**
     * 所有者
     */
    private String owner;
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 消息ID
     */
    private String messageId;
    
    /**
     * 消息类型（user: 用户消息, assistant: 助手消息）
     */
    private String role;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 推理内容
     */
    private String reasoningContent;
    
    /**
     * 标题
     */
    private String title;
    
    /**
     * 创建时间
     */
    private Date createTime;
    
    /**
     * 更新时间
     */
    private Date updateTime;
    
    public OwnerChatRecord() {
    }
    
    public OwnerChatRecord(String owner, String sessionId, String messageId, String title, String role, String content, String reasoningContent) {
        this.owner = owner;
        this.sessionId = sessionId;
        this.messageId = messageId;
        this.role = role;
        this.content = content;
        this.reasoningContent = reasoningContent;
        this.title = title;
        this.createTime = new Date();
        this.updateTime = new Date();
    }
} 