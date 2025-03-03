package me.dingtou.options.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 账户汇总
 *
 * @author qiyan
 */
@Data
public class OwnerSummary {
    /**
     * 期权总收益
     */
    private BigDecimal allOptionsIncome;

    /**
     * 总手续费
     */
    private BigDecimal totalFee;

    /**
     * 未实现期权收益
     */
    private BigDecimal unrealizedOptionsIncome;

    /**
     * 策略汇总
     */
    private List<StrategySummary> strategySummaries;

    /**
     * 未实现期权
     */
    private List<OwnerOrder> unrealizedOrders;
}
