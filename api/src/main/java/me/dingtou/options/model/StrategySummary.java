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
     * 期权总收益(未扣除手续费)
     * 注意：put期权提前指派后，卖出被指派股票和被指派期权后，allOptionsIncome会失真，一部分收益是股票差价。
     * 例如：
     * BABA250627P130000 在股价117.05时被指派。
     * 接下来卖出100股BABA（117.05） + BABA250718P130000（13.45）
     * 实际收益(13.45+117.05-130)*100 = 50，但是系统会记录收益1345，因为卖了一张13.45的期权。
     */
    private BigDecimal allOptionsIncome;

    /**
     * 策略所有交易总收益（已经扣除手续和初始化投资）
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
