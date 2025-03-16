package me.dingtou.options.constant;

import lombok.Getter;

/**
 * 任务状态
 *
 * @author qiyan
 */
@Getter
public enum TradeTaskStatus {
    /**
     * 待采纳
     */
    PENDING(1, "待采纳"),

    /**
     * 采纳中
     */
    EXECUTING(2, "采纳中"),

    /**
     * 已完成
     */
    COMPLETED(3, "已完成"),

    /**
     * 已取消
     */
    CANCELLED(4, "已取消"),

    /**
     * 执行失败
     */
    FAILED(5, "执行失败");

    private final Integer code;
    private final String desc;

    TradeTaskStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据code获取枚举
     *
     * @param code 代码
     * @return 枚举
     */
    public static TradeTaskStatus of(Integer code) {
        if (code == null) {
            return PENDING;
        }
        for (TradeTaskStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return PENDING;
    }
} 