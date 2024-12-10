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
}
