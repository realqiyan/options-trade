package me.dingtou.options.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * 支撑位
     */
    private SupportPriceIndicator supportPriceIndicator;

    /**
     * 趋势指标
     */
    private final Map<String, List<StockIndicatorItem>> indicatorMap = new HashMap<>();

    /**
     * 加入趋势指标
     *
     * @param key  指标key
     * @param line 指标线
     */
    public void addIndicator(String key, List<StockIndicatorItem> line) {
        this.indicatorMap.put(key, line);
    }

    /**
     * 移除趋势指标
     *
     * @param key 指标key
     */
    public void removeIndicator(String key) {
        this.indicatorMap.remove(key);
    }


}
