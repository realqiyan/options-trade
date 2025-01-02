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
     * 期权总收益
     */
    private BigDecimal allOptionsIncome;

    /**
     * 未实现期权收益
     */
    private BigDecimal unrealizedOptionsIncome;

    /**
     * 股票总收益
     */
    private BigDecimal securityIncome;

    /**
     * 持有股票数
     */
    private Integer holdSecurityNum;

    /**
     * 订单列表
     */
    private List<OwnerOrder> strategyOrders;
}
