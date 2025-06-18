package me.dingtou.options.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.TreeMap;

/**
 * 账户汇总
 *
 * @author qiyan
 */
@Data
public class OwnerSummary {
    /**
     * 总收入
     */
    private BigDecimal allIncome;

    /**
     * 期权总收益
     */
    private BigDecimal allOptionsIncome;

    /**
     * 股票盈亏
     */
    private BigDecimal allHoldStockProfit;

    /**
     * 总手续费
     */
    private BigDecimal totalFee;

    /**
     * 未实现期权收益
     */
    private BigDecimal unrealizedOptionsIncome;

    /**
     * 所有未到期期权合约数
     */
    private BigDecimal allOpenOptionsQuantity;

    /**
     * 策略汇总
     */
    private List<StrategySummary> strategySummaries;

    /**
     * 未实现期权
     */
    private List<OwnerOrder> unrealizedOrders;

    /**
     * 月度收益
     */
    private TreeMap<String, BigDecimal> monthlyIncome;

    /**
     * 账户总规模
     */
    private BigDecimal accountSize;

    /**
     * 保证金比例
     */
    private BigDecimal marginRatio;

    /**
     * 头寸比例
     */
    private BigDecimal positionRatio;

    /**
     * PUT订单保证金占用
     */
    private BigDecimal putMarginOccupied;

    /**
     * 持有股票总成本
     */
    private BigDecimal totalStockCost;

    /**
     * 可用资金
     */
    private BigDecimal availableFunds;

    /**
     * 当前投资占用
     */
    private BigDecimal totalInvestment;

    /**
     * 持仓
     */
    private List<OwnerPosition> positions;
}
