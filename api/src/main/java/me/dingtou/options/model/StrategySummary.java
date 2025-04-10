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
     * 总收入（期权收入+股票盈亏）
     */
    private BigDecimal allIncome;

    /**
     * 期权总收益
     */
    private BigDecimal allOptionsIncome;

    /**
     * 未实现期权收益
     */
    private BigDecimal unrealizedOptionsIncome;

    /**
     * 持有股票数
     */
    private Integer holdStockNum;

    /**
     * 持股盈亏
     */
    private BigDecimal holdStockProfit;

    /**
     * 股票总成本
     */
    private BigDecimal totalStockCost;

    /**
     * 股票平均成本
     */
    private BigDecimal averageStockCost;

    /**
     * 当前股价
     */
    private BigDecimal currentStockPrice;

    /**
     * PUT订单保证金占用
     */
    private BigDecimal putMarginOccupied;

    /**
     * 订单列表
     */
    private List<OwnerOrder> strategyOrders;
}
