package me.dingtou.options.model;

import lombok.Data;

import java.util.List;

/**
 * 期权链
 *
 * @author qiyan
 */
@Data
public class OptionsChain {

    /**
     * 期权到期日
     */
    private String strikeTime;

    /**
     * 期权列表
     */
    private List<Options> optionsList;

    /**
     * VIX指标
     */
    private VixIndicator vixIndicator;

    /**
     * 股票指标
     */
    private StockIndicator stockIndicator;

    /**
     * 交易级别
     * 0: 不推荐
     * 1: 正常
     */
    private Integer tradeLevel;

    /**
     * AI提示词
     */
    private String prompt;
}
