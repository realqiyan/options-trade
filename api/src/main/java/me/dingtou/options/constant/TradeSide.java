package me.dingtou.options.constant;

import lombok.Getter;

/**
 * 交易类型
 *
 * @author qiyan
 */
@Getter
public enum TradeSide {
    /**
     * 买
     */
    BUY(1, -1, 2, "买入"),

    /**
     * 卖
     */
    SELL(2, 1, 1, "卖出"),

    /**
     * 卖空
     */
    SELL_SHORT(3, 1, 1, "卖空"),

    /**
     * 买回
     */
    BUY_BACK(4, -1, 2, "买回"),
    ;

    /**
     * 交易类型
     */
    private final int code;
    /**
     * 开销符号 -1花钱 1赚钱
     */
    private final int sign;
    /**
     * 反向交易类型
     */
    private final int reverseCode;

    /**
     * 交易类型名称
     */
    private final String name;

    TradeSide(int code, int sign, int reverseCode, String name) {
        this.code = code;
        this.sign = sign;
        this.reverseCode = reverseCode;
        this.name = name;
    }

    public static TradeSide of(int code) {
        TradeSide[] values = TradeSide.values();
        for (TradeSide val : values) {
            if (val.getCode() == code) {
                return val;
            }
        }
        throw new IllegalArgumentException(code + " not found.");
    }

}
