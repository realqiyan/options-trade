package me.dingtou.options.constant;

/**
 * 内置期权策略
 */
public enum OptionsStrategy {

    /**
     * 备兑看涨策略(Covered Call Strategy)
     */
    CC_STRATEGY("cc_strategy", "备兑看涨策略(Covered Call Strategy)"),

    /**
     * 车轮策略(Wheel Strategy)
     */
    WHEEL_STRATEGY("wheel_strategy", "车轮策略(Wheel Strategy)"),

    /**
     * 默认卖期权策略(Default Strategy)
     */
    DEFAULT("default", "默认卖期权策略(Default Strategy)");

    private final String code;
    private final String description;

    OptionsStrategy(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static OptionsStrategy of(String code) {
        OptionsStrategy[] values = OptionsStrategy.values();
        for (OptionsStrategy val : values) {
            if (val.getCode().equals(code)) {
                return val;
            }
        }
        return DEFAULT;
    }

}
