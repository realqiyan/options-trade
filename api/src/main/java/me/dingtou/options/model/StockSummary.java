package me.dingtou.options.model;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 按股票汇总的收益明细
 */
@Data
public class StockSummary implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 股票代码
     */
    private String stockCode;

    /**
     * 总期权收入
     */
    private BigDecimal totalOptionsIncome;

    /**
     * 股票盈亏
     */
    private BigDecimal stockProfit;

    /**
     * 总收入
     */
    private BigDecimal totalIncome;

    /**
     * 手续费
     */
    private BigDecimal totalFee;

    /**
     * 持仓股票数量
     */
    private Integer holdStockNum;

    /**
     * 策略数量
     */
    private Integer strategyCount;
}