package me.dingtou.options.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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
     * 策略总体Options Delta
     */
    private BigDecimal optionsDelta;

    /**
     * 策略总体Options Gamma
     */
    private BigDecimal optionsGamma;

    /**
     * 策略总体Options Theta
     */
    private BigDecimal optionsTheta;

    /**
     * 策略未平仓的持有期权合约数
     */
    private BigDecimal openOptionsQuantity;

    /**
     * 平均Delta
     */
    private BigDecimal avgDelta;

    /**
     * 总手续费
     */
    private BigDecimal totalFee;

    /**
     * 期权总收益(已经扣除手续费)
     * 指派后卖出股票和新的期权后，allOptionsIncome会有偏差。
     */
    @Deprecated
    private BigDecimal allOptionsIncome;

    /**
     * 策略所有交易总收益(已经扣除手续+初始化投资)
     */
    private BigDecimal allTradeIncome;

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

    /**
     * 订单分组
     */
    private Map<String, OwnerOrderGroup> orderGroups;

}
