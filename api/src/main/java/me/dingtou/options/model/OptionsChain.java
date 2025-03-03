package me.dingtou.options.model;

import lombok.Data;

import java.math.BigDecimal;
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
     * 期权到期时间戳
     */
    private Long strikeTimestamp;

    /**
     * VIX指标
     */
    private VixIndicator vixIndicator;

    /**
     * 股票指标
     */
    private StockIndicator stockIndicator;

    /**
     * 期权列表
     */
    private List<OptionsTuple> optionList;

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

    /**
     * 最低价支撑位
     */
    private String lowSupport;

    /**
     * 移动平均支撑位
     */
    private String maSupport;

    /**
     * 布林带下轨支撑位
     */
    private String bollSupport;
}
