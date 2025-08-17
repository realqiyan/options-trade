package me.dingtou.options.constant;

import lombok.Getter;

@Getter
public enum OptionsStrategy {

    CC_STRATEGY("cc_strategy", "备兑看涨策略(Covered Call Strategy)"),

    WHEEL_STRATEGY("wheel_strategy", "车轮策略(Wheel Strategy)"),

    DEFAULT("default", "默认卖期权策略(Default Strategy)");

    private String code;
    private String name;

    OptionsStrategy(String code, String name) {
        this.code = code;
        this.name = name;
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
