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
    DAY(1000),
    WEEK(2000),
    MONTH(3000),
    YEAR(4000),
    ;

    /**
     * 状态码
     */
    private final Integer code;

    CandlestickPeriod(int code) {
        this.code = code;
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
