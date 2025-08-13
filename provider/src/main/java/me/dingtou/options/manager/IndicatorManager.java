package me.dingtou.options.manager;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsUpperIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.LowPriceIndicator;
import org.ta4j.core.indicators.helpers.LowestValueIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.constant.CandlestickAdjustType;
import me.dingtou.options.constant.CandlestickPeriod;
import me.dingtou.options.constant.IndicatorKey;
import me.dingtou.options.gateway.CandlestickGateway;
import me.dingtou.options.gateway.SecurityQuoteGateway;
import me.dingtou.options.gateway.VixQueryGateway;
import me.dingtou.options.model.Candlestick;
import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.model.Security;
import me.dingtou.options.model.SecurityCandlestick;
import me.dingtou.options.model.SecurityQuote;
import me.dingtou.options.model.StockIndicator;
import me.dingtou.options.model.StockIndicatorItem;
import me.dingtou.options.model.SupportPriceIndicator;
import me.dingtou.options.model.VixIndicator;
import me.dingtou.options.util.ExceptionUtils;
import me.dingtou.options.util.NumberUtils;

@Slf4j
@Component
public class IndicatorManager {

    /**
     * 股票指标缓存
     */
    private static final Cache<String, StockIndicator> INDICATOR_CACHE = CacheBuilder.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    @Autowired
    private SecurityQuoteGateway securityQuoteGateway;

    @Autowired
    private CandlestickGateway candlestickGateway;

    @Autowired
    private VixQueryGateway vixQueryGateway;

    /**
     * 获取股票价格
     * 
     * @param ownerAccount 账户
     * @param security     股票
     * @return 价格
     */
    public BigDecimal queryStockPrice(OwnerAccount ownerAccount, Security security) {
        SecurityQuote securityQuote = securityQuoteGateway.quote(ownerAccount, security);
        return securityQuote.getLastDone();
    }

    /**
     * 获取VIX指数
     * 
     * @return VIX指数
     */
    public VixIndicator queryCurrentVix() {
        return vixQueryGateway.queryCurrentVix();
    }

    /**
     * 计算股票指标
     * 
     * @param ownerAccount 账户
     * @param security     股票
     * @param period       周期
     * @param count        数量
     * @return 股票指标
     */
    public StockIndicator calculateStockIndicator(OwnerAccount ownerAccount,
            Security security,
            CandlestickPeriod klinePeriod,
            int count) {
        try {
            return INDICATOR_CACHE.get(security.toString(), () -> {
                // 根据账户配置获取K线周期
                // 根据账户配置获取K线周期
                StockIndicator stockIndicator = new StockIndicator();
                // 期权标的价格
                SecurityQuote securityQuote = securityQuoteGateway.quote(ownerAccount, security);
                stockIndicator.setSecurityQuote(securityQuote);
                // 获取K线数据 50日均线需要多取50条数据计算
                int kCount = count + 50;
                SecurityCandlestick candlesticks = candlestickGateway.getCandlesticks(ownerAccount,
                        security,
                        klinePeriod,
                        kCount,
                        CandlestickAdjustType.FORWARD_ADJUST);
                if (null == candlesticks || CollectionUtils.isEmpty(candlesticks.getCandlesticks())) {
                    return stockIndicator;
                }

                // 技术指标
                BarSeries barSeries = convertToBarSeries(candlesticks);
                ClosePriceIndicator closePrice = new ClosePriceIndicator(barSeries);

                // RSI
                stockIndicator.addIndicator(IndicatorKey.RSI.getKey(), getValue(new RSIIndicator(closePrice, 14), 14));

                // MACD相关指标
                MACDIndicator macdIndicator = new MACDIndicator(closePrice, 12, 26);
                // MACD柱状图
                stockIndicator.addIndicator(IndicatorKey.MACD.getKey(), getValue(macdIndicator, 26));
                // DIF快线 - 短期EMA与长期EMA的差值
                stockIndicator.addIndicator(IndicatorKey.MACD_DIF.getKey(), getValue(macdIndicator, 26));
                // DEA慢线 - DIF的9日EMA
                EMAIndicator signalLine = macdIndicator.getSignalLine(9);
                stockIndicator.addIndicator(IndicatorKey.MACD_DEA.getKey(), getValue(signalLine, 26));

                // EMA
                stockIndicator.addIndicator(IndicatorKey.EMA5.getKey(), getValue(new EMAIndicator(closePrice, 5), 0));
                stockIndicator.addIndicator(IndicatorKey.EMA20.getKey(), getValue(new EMAIndicator(closePrice, 20), 0));
                stockIndicator.addIndicator(IndicatorKey.EMA50.getKey(), getValue(new EMAIndicator(closePrice, 50), 0));

                // BOLL
                SMAIndicator sma20 = new SMAIndicator(closePrice, 20);
                BollingerBandsMiddleIndicator bollMiddle = new BollingerBandsMiddleIndicator(sma20);
                Indicator<Num> deviation = new StandardDeviationIndicator(closePrice, 20);
                Num k = DecimalNum.valueOf(2);
                // 上轨通常是中轨加上2倍标准差
                BollingerBandsUpperIndicator bollUpper = new BollingerBandsUpperIndicator(bollMiddle, deviation, k);
                // 下轨通常是中轨减去2倍标准差
                BollingerBandsLowerIndicator bollLower = new BollingerBandsLowerIndicator(bollMiddle, deviation, k);
                stockIndicator.addIndicator(IndicatorKey.BOLL_MIDDLE.getKey(), getValue(bollMiddle, 20));
                stockIndicator.addIndicator(IndicatorKey.BOLL_UPPER.getKey(), getValue(bollUpper, 20));
                stockIndicator.addIndicator(IndicatorKey.BOLL_LOWER.getKey(), getValue(bollLower, 20));

                SupportPriceIndicator supportPriceIndicator = new SupportPriceIndicator();
                supportPriceIndicator.setLowestSupportPrice(calculateLowSupport(barSeries));
                stockIndicator.setSupportPriceIndicator(supportPriceIndicator);

                for (String key : stockIndicator.getIndicatorMap().keySet()) {
                    List<StockIndicatorItem> dataList = stockIndicator.getIndicatorMap().get(key);
                    if (null != dataList && dataList.size() > count) {
                        stockIndicator.getIndicatorMap().put(key, dataList.subList(0, count));
                    }
                }

                int kSize = candlesticks.getCandlesticks().size();
                List<Candlestick> subCandlesticks = candlesticks.getCandlesticks().subList(kSize - count, kSize);
                stockIndicator.setCandlesticks(subCandlesticks);
                stockIndicator.setPeriod(klinePeriod);

                return stockIndicator;
            });
        } catch (Exception e) {
            log.error("计算股票指标失败", e);
            ExceptionUtils.throwRuntimeException(e);
            return null;
        }
    }

    /**
     * 获取K线数据
     * 
     * @param ownerAccount 账户
     * @param security     股票
     * @param period       周期
     * @param count        数量
     * @return K线数据
     */
    public SecurityCandlestick getCandlesticks(OwnerAccount ownerAccount,
            Security security,
            CandlestickPeriod period,
            Integer count) {
        return candlestickGateway.getCandlesticks(ownerAccount, security, period, count,
                CandlestickAdjustType.FORWARD_ADJUST);
    }

    /**
     * 计算最低价支撑位
     * 
     * @param barSeries K线数据
     * @return 最低价支撑位
     */
    private BigDecimal calculateLowSupport(BarSeries barSeries) {
        // 创建低价指标
        LowPriceIndicator lowPrice = new LowPriceIndicator(barSeries);
        LowestValueIndicator lowest = new LowestValueIndicator(lowPrice, barSeries.getBarCount());
        // 获取最新支撑位
        Num supportLevel = lowest.getValue(barSeries.getEndIndex());
        return NumberUtils.scale(supportLevel.bigDecimalValue());
    }

    /**
     * 获取指标值列表
     * 
     * @param indicator 指标
     * @param skip      偏移量
     * @return 指标值列表
     */
    private List<StockIndicatorItem> getValue(CachedIndicator<Num> indicator, int skip) {
        int endIndex = indicator.getBarSeries().getEndIndex();
        List<StockIndicatorItem> result = new ArrayList<>();
        DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd").toFormatter();
        // 排除掉 unstableBars 和 最早3天的数据
        for (int i = endIndex; i >= indicator.getUnstableBars() + skip; i--) {
            Num value = indicator.getValue(i);
            Bar bar = indicator.getBarSeries().getBar(i);
            // 保留3位小数后直接截断
            BigDecimal val = NumberUtils.scale(value.bigDecimalValue());
            result.add(new StockIndicatorItem(bar.getEndTime().format(dateTimeFormatter), val));
        }
        return result;
    }

    /**
     * 将SecurityCandlestick转换为BarSeries
     * 
     * @param candlesticks 蜡烛图数据
     * @return BarSeries
     */
    private BarSeries convertToBarSeries(SecurityCandlestick candlesticks) {
        BarSeries series = new BaseBarSeriesBuilder().withName(candlesticks.getSecurity().toString()).build();
        for (Candlestick candlestick : candlesticks.getCandlesticks()) {
            // candlestick.getTimestamp()转ZonedDateTime
            Instant instant = Instant.ofEpochSecond(candlestick.getTimestamp());
            series.addBar(instant.atZone(ZoneId.systemDefault()), candlestick.getOpen(), candlestick.getHigh(),
                    candlestick.getLow(), candlestick.getClose(), candlestick.getVolume(), candlestick.getTurnover());
        }
        return series;
    }

}
