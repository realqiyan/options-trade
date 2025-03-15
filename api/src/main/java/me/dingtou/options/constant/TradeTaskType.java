package me.dingtou.options.constant;

import lombok.Getter;

/**
 * 任务类型
 *
 * @author qiyan
 */
@Getter
public enum TradeTaskType {
    /**
     * 买入期权
     */
    BUY_OPTION(1, "买入期权"),

    /**
     * 卖出期权
     */
    SELL_OPTION(2, "卖出期权"),

    /**
     * 平仓期权
     */
    CLOSE_OPTION(5, "平仓期权"),

    /**
     * 滚动期权
     */
    ROLL_OPTION(7, "滚动期权"),

    /**
     * 其他任务
     */
    OTHER(99, "其他任务");

    private final int code;
    private final String desc;

    TradeTaskType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据code获取枚举
     *
     * @param code 代码
     * @return 枚举
     */
    public static TradeTaskType of(int code) {
        for (TradeTaskType type : values()) {
            if (type.getCode() == code) {
                return type;
            }
        }
        return OTHER;
    }
} 