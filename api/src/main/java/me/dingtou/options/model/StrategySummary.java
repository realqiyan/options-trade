package me.dingtou.options.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 策略汇总
 *
 * @author qiyan
 */
@Data
public class StrategySummary {

    /**
     * 策略信息
     */
    private OwnerStrategy strategy;

    /**
     * 总手续费
     */
    private BigDecimal totalFee;

    /**
     * 总收益
     */
    private BigDecimal totalIncome;

    /**
     * 订单列表
     */
    private List<OwnerOrder> strategyOrders;
}
