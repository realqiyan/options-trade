package me.dingtou.options.constant;

import lombok.Getter;

/**
 * 策略阶段
 *
 * @author qiyan
 */
@Getter
public enum StrategyStage {

    RUNNING("running"), //进行中
    SUSPEND("suspend"), //暂停
    STOPPED("stopped"), //停止
    ;

    /**
     * 阶段编码
     */
    private final String code;

    StrategyStage(String code) {
        this.code = code;
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
