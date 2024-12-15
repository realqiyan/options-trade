package me.dingtou.options.constant;

/**
 * 交易动作
 */
public enum OrderAction {
    /**
     * 取消
     */
    CANCEL("cancel"),

    /**
     * 未知
     */
    UNKNOWN("unknown");

    private final String code;

    OrderAction(String code) {
        this.code = code;
    }

    public static OrderAction of(String code) {
        OrderAction[] values = OrderAction.values();
        for (OrderAction val : values) {
            if (val.getCode().equals(code)) {
                return val;
            }
        }
        return UNKNOWN;
    }

    public String getCode() {
        return code;
    }
}
