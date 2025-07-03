package me.dingtou.options.model;

import org.apache.commons.text.StringEscapeUtils;

import lombok.Data;
import me.dingtou.options.util.EscapeUtils;

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

    public Message(String sessionId, String role, String content) {
        this.sessionId = sessionId;
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

    public void escapeHtml() {
        this.content = EscapeUtils.escapeHtml(this.content);
        this.reasoningContent = EscapeUtils.escapeHtml(this.reasoningContent);
    }

    public void escapeJson() {
        this.content = StringEscapeUtils.escapeJson(this.content);
        this.reasoningContent = StringEscapeUtils.escapeJson(this.reasoningContent);
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
