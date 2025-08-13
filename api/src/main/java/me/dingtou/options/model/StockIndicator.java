package me.dingtou.options.model;

import lombok.Data;
import me.dingtou.options.constant.CandlestickPeriod;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
     * 近期K线类型
     */
    private CandlestickPeriod period;

    /**
     * 近期K线
     */
    private List<Candlestick> candlesticks;

    /**
     * 支撑位
     */
    private SupportPriceIndicator supportPriceIndicator;

    /**
     * 趋势指标
     */
    private final Map<String, List<StockIndicatorItem>> indicatorMap = new TreeMap<>();

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
