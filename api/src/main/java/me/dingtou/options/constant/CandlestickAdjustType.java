package me.dingtou.options.constant;

import lombok.Getter;

/**
 * K线复权类型
 * <p>
 * NO_ADJUST = 0;除权
 * FORWARD_ADJUST = 1;前复权
 *
 * @author yhb
 */
@Getter
public enum CandlestickAdjustType {
    NO_ADJUST(0),
    FORWARD_ADJUST(1),
    ;

    /**
     * 状态码
     */
    private final Integer code;

    CandlestickAdjustType(int code) {
        this.code = code;
    }

    public static CandlestickAdjustType of(Integer code) {
        CandlestickAdjustType[] values = CandlestickAdjustType.values();
        for (CandlestickAdjustType val : values) {
            if (val.getCode().equals(code)) {
                return val;
            }
        }
        throw new IllegalArgumentException(code + " not found.");
    }
}
