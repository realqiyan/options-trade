package me.dingtou.options.constant;

/**
 * 状态
 *
 * @author qiyan
 */
public enum Status {
    /**
     * 正常状态
     */
    NORMAL(1),
    /**
     * 处理中
     */
    PROCESSING(0),
    /**
     * 删除
     */
    DELETE(-1);

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

    public Integer getCode() {
        return code;
    }
}
