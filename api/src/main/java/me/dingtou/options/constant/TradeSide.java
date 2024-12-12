package me.dingtou.options.constant;

/**
 * 交易类型
 *
 * @author qiyan
 */
public enum TradeSide {
    /**
     * 买
     */
    BUY(1),
    /**
     * 卖
     */
    SELL(2);

    private final int code;

    TradeSide(int code) {
        this.code = code;
    }


    public int getCode() {
        return code;
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
