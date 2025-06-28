package me.dingtou.options.model;

import lombok.Data;

/**
 * 消息
 *
 * @author qiyan
 */
@Data
public class Message {
    /**
     * 消息ID
     */
    private String id;
    /**
     * 会话ID
     */
    private String sessionId;
    /**
     * 角色
     */
    private String role;
    /**
     * 消息内容
     */
    private String content;
    /**
     * 思考内容
     */
    private String reasoningContent;

    public Message() {
    }

    public Message(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public Message(String id, String sessionId, String role, String content, String reasoningContent) {
        this.id = id;
        this.sessionId = sessionId;
        this.role = role;
        this.content = content;
        this.reasoningContent = reasoningContent;
    }

    @Override
    public String toString() {
        return "Message{" +
                "id='" + id + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", role='" + role + '\'' +
                ", content='" + content + '\'' +
                ", reasoningContent='" + reasoningContent + '\'' +
                '}';
    }
}
