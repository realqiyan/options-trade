package me.dingtou.options.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 所有者沟通记录
 *
 * @author qiyan
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OwnerChatRecord extends Message {
    /**
     * 记录ID
     */
    private Long id;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 所有者
     */
    private String owner;

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

    public OwnerChatRecord(String owner,
            String sessionId,
            String title,
            String role,
            String content,
            String reasoningContent) {
        super(role, content);
        Date date = new Date();
        this.setOwner(owner);
        this.setSessionId(sessionId);
        this.setTitle(title);
        this.setCreateTime(date);
        this.setUpdateTime(date);
        this.setReasoningContent(reasoningContent);
    }
}