package me.dingtou.options.constant;

import lombok.Getter;

@Getter
public enum OptionsStrategy {

    CC_STRATEGY("cc_strategy", "Covered Call Strategy"),

    WHEEL_STRATEGY("wheel_strategy", "车轮策略(Wheel Strategy)"),

    DEFAULT("default", "卖期权策略");

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
        throw new IllegalArgumentException(code + " not found.");
    }

}
