package me.dingtou.options.constant;

import lombok.Getter;

/**
 * 策略状态
 *
 * @author qiyan
 */
@Getter
public enum StrategyStatus {


    VALID(1), //有效
    INVALID(0), //无效
    ;

    /**
     * 状态码
     */
    private final Integer code;

    StrategyStatus(int code) {
        this.code = code;
    }

    public static StrategyStatus of(Integer code) {
        StrategyStatus[] values = StrategyStatus.values();
        for (StrategyStatus val : values) {
            if (val.getCode().equals(code)) {
                return val;
            }
        }
        throw new IllegalArgumentException(code + " not found.");
    }

}
