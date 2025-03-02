package me.dingtou.options.constant;

import lombok.Getter;

@Getter
public enum IndicatorKey {

    /**
     * RSI
     */
    RSI("rsi", "RSI"),
    /**
     * MACD
     */
    MACD("macd", "MACD"),
    /**
     * EMA50
     */
    EMA50("ema50", "EMA50"),
    /**
     * BOLL_MIDDLE
     */
    BOLL_MIDDLE("boll_middle", "BOLL中轨"),
    /**
     * BOLL_UPPER
     */
    BOLL_UPPER("boll_upper", "BOLL上轨"),
    /**
     * BOLL_LOWER
     */
    BOLL_LOWER("boll_lower", "BOLL下轨"),
    ;


    /**
     * key
     */
    private final String key;
    /**
     * display name
     */
    private final String displayName;

    IndicatorKey(String key, String displayName) {
        this.key = key;
        this.displayName = displayName;
    }

    public static IndicatorKey of(String key) {
        IndicatorKey[] values = IndicatorKey.values();
        for (IndicatorKey val : values) {
            if (val.getKey().equals(key)) {
                return val;
            }
        }
        throw new IllegalArgumentException(key + " not found.");
    }


}
