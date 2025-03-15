package me.dingtou.options.constant;

import lombok.Getter;

/**
 * K线周期
 * <p>
 * DAY = 1000;
 * WEEK = 2000;
 * MONTH = 3000;
 * YEAR = 4000;
 *
 * @author yhb
 */
@Getter
public enum CandlestickPeriod {
    DAY(1000, "日"),
    WEEK(2000, "周"),
    MONTH(3000, "月"),
    YEAR(4000, "年"),
    ;

    /**
     * 编码
     */
    private final Integer code;
    /**
     * 名字
     */
    private final String name;

    CandlestickPeriod(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static CandlestickPeriod of(Integer code) {
        CandlestickPeriod[] values = CandlestickPeriod.values();
        for (CandlestickPeriod val : values) {
            if (val.getCode().equals(code)) {
                return val;
            }
        }
        throw new IllegalArgumentException(code + " not found.");
    }
}
