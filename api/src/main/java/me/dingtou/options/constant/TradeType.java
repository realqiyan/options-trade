package me.dingtou.options.constant;

/**
 * 交易类型
 *
 * @author qiyan
 */
public enum TradeType {
    /**
     * 买
     */
    BUY("buy"),
    /**
     * 卖
     */
    SELL("sell");

    private final String code;

    TradeType(String code) {
        this.code = code;
    }

    public static TradeType of(String code) {
        TradeType[] values = TradeType.values();
        for (TradeType val : values) {
            if (val.getCode().equals(code)) {
                return val;
            }
        }
        throw new IllegalArgumentException(code + " not found.");
    }

    public String getCode() {
        return code;
    }

    @Override
    public String toString() {
        return code;
    }
}
