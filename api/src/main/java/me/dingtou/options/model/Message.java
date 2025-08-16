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
    private String messageId;
    /**
     * 角色（user: 用户消息, assistant: 助手消息）
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
        this.messageId = role + System.currentTimeMillis();
        this.role = role;
        this.content = content;
    }

    public Message(String messageId,
            String role,
            String content) {
        this.messageId = messageId;
        this.role = role;
        this.content = content;
    }

    @Override
    public String toString() {
        return "Message{" +
                "messageId='" + messageId + '\'' +
                ", role='" + role + '\'' +
                ", content='" + content + '\'' +
                ", reasoningContent='" + reasoningContent + '\'' +
                '}';
    }

}
