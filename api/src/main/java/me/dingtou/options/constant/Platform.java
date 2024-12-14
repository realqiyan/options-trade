package me.dingtou.options.constant;

/**
 * 平台
 *
 * @author qiyan
 */
public enum Platform {

    /**
     * 富途
     */
    FUTU("futu"),
    /**
     * 长桥
     */
    LONGPORT("longport"),
    /**
     * 未知
     */
    UNKNOWN("unknown");

    private final String code;

    Platform(String code) {
        this.code = code;
    }

    public static Platform of(String code) {
        Platform[] values = Platform.values();
        for (Platform val : values) {
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
