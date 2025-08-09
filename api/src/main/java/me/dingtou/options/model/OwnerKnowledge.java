package me.dingtou.options.model;

import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 知识库模型
 *
 * @author ai
 */
@Data
public class OwnerKnowledge implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 所有者
     */
    private String owner;

    /**
     * 标题
     */
    private String title;

    /**
     * 类型
     * 1: 期权策略知识
     * 2: 规则知识
     */
    private Integer type;

    /**
     * 描述
     */
    private String description;

    /**
     * 内容（支持Markdown）
     */
    private String content;

    /**
     * 是否启用
     * 1: 启用
     * 0: 禁用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    // 枚举类型定义
    public enum KnowledgeType {
        OPTIONS_STRATEGY(1, "期权策略知识"),
        RULES(2, "规则知识");

        private final Integer code;
        private final String description;

        KnowledgeType(Integer code, String description) {
            this.code = code;
            this.description = description;
        }

        public Integer getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }

    // 状态枚举
    public enum KnowledgeStatus {
        DISABLED(0, "禁用"),
        ENABLED(1, "启用");

        private final Integer code;
        private final String description;

        KnowledgeStatus(Integer code, String description) {
            this.code = code;
            this.description = description;
        }

        public Integer getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }
}