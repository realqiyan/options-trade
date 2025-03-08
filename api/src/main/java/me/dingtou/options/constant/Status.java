package me.dingtou.options.constant;

import lombok.Getter;

/**
 * 策略状态
 *
 * @author qiyan
 */
@Getter
public enum Status {


    VALID(1), //有效
    INVALID(0), //无效
    ;

    /**
     * 状态码
     */
    private final Integer code;

    Status(int code) {
        this.code = code;
    }

    public static Status of(Integer code) {
        Status[] values = Status.values();
        for (Status val : values) {
            if (val.getCode().equals(code)) {
                return val;
            }
        }
        throw new IllegalArgumentException(code + " not found.");
    }

}
