package me.dingtou.options.constant;

import lombok.Getter;

/**
 * 期权过滤类型
 *
 * @author qiyan
 */
@Getter
public enum OptionsFilterType {
    /**
     * 获取所有期权
     */
    ALL("ALL", "所有期权"),

    /**
     * 仅获取PUT期权
     */
    PUT("PUT", "看跌期权"),

    /**
     * 仅获取CALL期权
     */
    CALL("CALL", "看涨期权"),

    /**
     * 仅获取OTM ALL期权
     */
    OTM_ALL("OTM_ALL", "OTM ALL期权");

    /**
     * 过滤类型代码
     */
    private final String code;

    /**
     * 过滤类型名称
     */
    private final String name;

    OptionsFilterType(String code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 根据代码获取枚举值
     *
     * @param code 代码
     * @return 枚举值
     */
    public static OptionsFilterType of(String code) {
        OptionsFilterType[] values = OptionsFilterType.values();
        for (OptionsFilterType val : values) {
            if (val.getCode().equals(code)) {
                return val;
            }
        }
        return OTM_ALL;
    }

}