package me.dingtou.options.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 股票行情
 *
 * @author qiyan
 */
@Data
public class StockIndicator {

    /**
     * 期权标的行情
     */
    private SecurityQuote securityQuote;

    /**
     * 周价格幅度
     */
    private BigDecimal weekPriceRange;

    /**
     * 月价格幅度
     */
    private BigDecimal monthPriceRange;

    /**
     * 周K线
     */
    private Candlestick weekCandlestick;

    /**
     * 月K线
     */
    private Candlestick monthCandlestick;

    /**
     * 近几天RSI
     */
    private List<BigDecimal> rsi;

    /**
     * 近几天EMA5
     */
    private List<BigDecimal> ema5;

    /**
     * 近几天EMA20
     */
    private List<BigDecimal> ema20;

    /**
     * 近几天MACD
     */
    private List<BigDecimal> macd;

}
