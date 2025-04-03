package me.dingtou.options.constant;

import java.time.ZoneId;

/**
 * 市场
 *
 * @author qiyan
 */
public enum Market {

    /**
     * 美国
     */
    US(11, ZoneId.of("America/New_York")),
    /**
     * 香港
     */
    HK(1, ZoneId.of("Asia/Hong_Kong")),
    ;

    private final int code;

    private final ZoneId zoneId;

    Market(int code, ZoneId zoneId) {
        this.code = code;
        this.zoneId = zoneId;
    }

    public static Market of(int code) {
        Market[] values = Market.values();
        for (Market val : values) {
            if (val.getCode() == code) {
                return val;
            }
        }
        throw new IllegalArgumentException("Invalid market code: " + code);
    }

    public int getCode() {
        return code;
    }

    public ZoneId getZoneId() {
        return zoneId;
    }

}
