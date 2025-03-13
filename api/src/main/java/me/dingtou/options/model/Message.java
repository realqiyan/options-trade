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

    public Message(String id, String role, String content, String reasoningContent) {
        this.id = id;
        this.role = role;
        this.content = content;
        this.reasoningContent = reasoningContent;
    }

    @Override
    public String toString() {
        return "Message{" +
                "id='" + id + '\'' +
                ", role='" + role + '\'' +
                ", content='" + content + '\'' +
                ", reasoningContent='" + reasoningContent + '\'' +
                '}';
    }
}
