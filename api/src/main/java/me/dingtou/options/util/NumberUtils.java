package me.dingtou.options.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 数字处理工具类
 *
 * @author qiyan
 */
public class NumberUtils {

    /**
     * 默认精度
     */
    private static final int DEFAULT_SCALE = 4;

    /**
     * 设置精度 默认当精度大于4时设置精度为4并且截断后面的数字
     *
     * @param num 数字
     * @return 处理精度后的数字
     */
    public static BigDecimal scale(BigDecimal num) {
        return scale(num, DEFAULT_SCALE, RoundingMode.DOWN);
    }

    /**
     * 设置精度
     *
     * @param num         数字
     * @param targetScale 目标精度
     * @return 处理精度后的数字
     */
    public static BigDecimal scale(BigDecimal num, int targetScale, RoundingMode roundingMode) {
        if (null == num) {
            return BigDecimal.ZERO;
        }
        if (null == roundingMode) {
            return num;
        }
        int scale = Math.min(num.scale(), targetScale);
        return num.setScale(scale, roundingMode).stripTrailingZeros();
    }

}
