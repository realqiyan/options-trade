package me.dingtou.options.model;

import lombok.Data;

import java.util.Optional;

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

    public Message() {
    }

    public Message(String id, String type, String message) {
        this.id = id;
        this.type = type;
        this.message = message;
    }

    @Override
    public String toString() {
        return "Message{" +
                "type='" + type + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
