package me.dingtou.options.model;


import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author qiyan
 */
@Data
public class VixIndicator {
    /**
     * 恐慌指数
     */
    private StockIndicatorItem currentVix;

    /**
     * 标普500指数
     */
    private StockIndicatorItem currentSp500;

    /**
     * VIX日变化
     */
    private BigDecimal vixDailyChange;

    /**
     * VIX日回报率(%)
     */
    private BigDecimal vixDailyReturn;

    /**
     * VIX月至今回报率(%)
     */
    private BigDecimal vixMonthToDateReturn;

    /**
     * VIX年至今回报率(%)
     */
    private BigDecimal vixYearToDateReturn;

    /**
     * VIX一年波动率(%)
     */
    private BigDecimal vixOneYearVolatility;

    /**
     * 标普500日变化
     */
    private BigDecimal sp500DailyChange;

    /**
     * 标普500日回报率(%)
     */
    private BigDecimal sp500DailyReturn;

    /**
     * VIX与标普500相关系数
     */
    private BigDecimal correlationWithSp500;

    /**
     * 近期VIX历史数据
     */
    private List<StockIndicatorItem> vixHistory;

    /**
     * 近期标普500历史数据
     */
    private List<StockIndicatorItem> sp500History;
}
