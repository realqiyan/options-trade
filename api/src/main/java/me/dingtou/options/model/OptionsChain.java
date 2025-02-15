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
     * 周价格幅度
     */
    private BigDecimal weekPriceRange;

    /**
     * 周收盘行情
     */
    private Candlestick weekCandlestick;

    /**
     * 月价格幅度
     */
    private BigDecimal monthPriceRange;

    /**
     * 月收盘行情
     */
    private Candlestick monthCandlestick;

    /**
     * .VIX行情
     */
    private SecurityQuote vixQuote;

    /**
     * 期权标的行情
     */
    private SecurityQuote securityQuote;

    /**
     * 期权列表
     */
    private List<OptionsTuple> optionList;

    /**
     * AI提示
     */
    private String aiPrompt;
}
