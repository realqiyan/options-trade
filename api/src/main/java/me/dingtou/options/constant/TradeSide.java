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
    BUY(1, -1, 2),

    /**
     * 卖
     */
    SELL(2, 1, 1),

    /**
     * 卖空
     */
    SELL_SHORT(3, 1, 1),

    /**
     * 买回
     */
    BUY_BACK(4, -1, 2),
    ;

    private final int code;
    private final int sign;
    private final int reverseCode;

    TradeSide(int code, int sign, int reverseCode) {
        this.code = code;
        this.sign = sign;
        this.reverseCode = reverseCode;
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
