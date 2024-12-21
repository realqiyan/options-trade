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
    BUY(1),

    /**
     * 卖
     */
    SELL(2),

    /**
     * 卖空
     */
    SELL_SHORT(3),

    /**
     * 买回
     */
    BUY_BACK(4),
    ;

    private final int code;

    TradeSide(int code) {
        this.code = code;
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
