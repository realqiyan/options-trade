package me.dingtou.options.constant;

import lombok.Getter;

/**
 * 订单来源
 *
 * @author qiyan
 */
@Getter
public enum TradeFrom {
    /**
     * 系统创建
     */
    SYS_CREATE("sys_create"),
    /**
     * 系统平仓
     */
    SYS_CLOSE("sys_close"),
    /**
     * 同步订单
     */
    PULL_ORDER("pull_order"),
    /**
     * 同步成交
     */
    PULL_FILL("pull_fill"),
    ;

    private final String code;

    TradeFrom(String code) {
        this.code = code;
    }

    public static TradeFrom of(String code) {
        TradeFrom[] values = TradeFrom.values();
        for (TradeFrom val : values) {
            if (val.getCode().equals(code)) {
                return val;
            }
        }
        throw new IllegalArgumentException(code + " not found.");
    }

}
