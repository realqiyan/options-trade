package me.dingtou.options.constant;

/**
 * 交易状态
 *
 *  @author qiyan
 */
public enum TradeStatus {
    /**
     * 交易处理中
     */
    PROCESSING("processing"),
    /**
     * 交易处理完成
     */
    DONE("done");

    private final String code;

    TradeStatus(String code) {
        this.code = code;
    }

    public static TradeStatus of(String code) {
        TradeStatus[] values = TradeStatus.values();
        for (TradeStatus val : values) {
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
