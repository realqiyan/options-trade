package me.dingtou.options.model;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 策略数据
 *
 * @author qiyan
 */
@Data
public class OptionsStrategyData {
    /**
     * 卖出年化收益率
     */
    private BigDecimal sellAnnualYield;
    /**
     * 价格涨跌幅
     */
    private BigDecimal range;
    /**
     * 是否推荐
     */
    private Boolean recommend;
    /**
     * 推荐级别 0 不推荐 1 推荐 2 强烈推荐
     */
    private int recommendLevel;
}
