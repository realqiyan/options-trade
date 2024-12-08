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
    HK(1);

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
        throw new IllegalArgumentException(code + " not found.");
    }

    public int getCode() {
        return code;
    }

}
