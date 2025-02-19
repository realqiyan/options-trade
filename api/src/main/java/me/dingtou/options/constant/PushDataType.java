package me.dingtou.options.constant;

import lombok.Getter;

/**
 * 数据推送类型
 */
@Getter
public enum PushDataType {

    /**
     * 纽约时间
     */
    NYC_TIME("nyc_time", String.class),
    ;

    /**
     * 数据编码
     */
    private final String code;
    /**
     * 数据类型
     */
    private final Class<?> dataClass;

    PushDataType(String code, Class<?> dataClass) {
        this.code = code;
        this.dataClass = dataClass;
    }

    public static StrategyStage of(String code) {
        StrategyStage[] values = StrategyStage.values();
        for (StrategyStage val : values) {
            if (val.getCode().equals(code)) {
                return val;
            }
        }
        throw new IllegalArgumentException(code + " not found.");
    }
}
