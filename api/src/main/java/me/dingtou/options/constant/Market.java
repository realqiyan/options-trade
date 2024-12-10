package me.dingtou.options.constant;

/**
 * 市场
 *
 * @author qiyan
 */
public enum Market {

    /**
     * 美国
     */
    US(11),
    /**
     * 香港
     */
    HK(1),
    /**
     * 未知
     */
    UNKNOWN(-1);

    private final int code;

    Market(int code) {
        this.code = code;
    }

    public static Market of(int code) {
        Market[] values = Market.values();
        for (Market val : values) {
            if (val.getCode() == code) {
                return val;
            }
        }
        return UNKNOWN;
    }

    public int getCode() {
        return code;
    }

}
