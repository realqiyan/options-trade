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
     * 消息类型
     */
    private String type;
    /**
     * 消息内容
     */
    private String message;
    /**
     * 思考内容
     */
    private String reasoningMessage;

    public Message() {
    }

    public Message(String id, String type, String message) {
        this.id = id;
        this.type = type;
        this.message = message;
    }
    
    public Message(String id, String type, String message, String reasoningMessage) {
        this.id = id;
        this.type = type;
        this.message = message;
        this.reasoningMessage = reasoningMessage;
    }

    @Override
    public String toString() {
        return "Message{" +
                "type='" + type + '\'' +
                ", message='" + message + '\'' +
                ", reasoningMessage='" + reasoningMessage + '\'' +
                '}';
    }
}
