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
     * 策略总体Delta
     */
    private BigDecimal strategyDelta;

    /**
     * 策略总体Gamma
     */
    private BigDecimal strategyGamma;

    /**
     * 策略总体Theta
     */
    private BigDecimal strategyTheta;

    /**
     * 策略方向(-100->0->100) 100=100%看多，-100=100%看空
     */
    private BigDecimal strategyDirection;

    /**
     * 总手续费
     */
    private BigDecimal totalFee;

    /**
     * 期权总收益(已经扣除手续费)
     */
    private BigDecimal allOptionsIncome;

    /**
     * 总收入（期权总收益+股票盈亏）
     */
    private BigDecimal allIncome;

    /**
     * 未实现期权收益
     */
    private BigDecimal unrealizedOptionsIncome;

    /**
     * 持有股票数
     */
    private Integer holdStockNum;

    /**
     * 持有股票成本价
     */
    private BigDecimal holdStockCost;

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
