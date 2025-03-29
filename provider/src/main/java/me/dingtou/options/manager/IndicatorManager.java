package me.dingtou.options.manager;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Comparator;
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
import me.dingtou.options.model.Candlestick;
import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.model.Security;
import me.dingtou.options.model.SecurityCandlestick;
import me.dingtou.options.model.SecurityQuote;
import me.dingtou.options.model.StockIndicator;
import me.dingtou.options.model.StockIndicatorItem;
import me.dingtou.options.model.SupportPriceIndicator;
import me.dingtou.options.util.AccountExtUtils;
import me.dingtou.options.util.NumberUtils;

@Slf4j
@Component
public class IndicatorManager {

    /**
     * 股票指标缓存
     */
    private static final Cache<String, StockIndicator> INDICATOR_CACHE = CacheBuilder.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build();

    @Autowired
    private SecurityQuoteGateway securityQuoteGateway;

    @Autowired
    private CandlestickGateway candlestickGateway;

    /**
     * 计算股票指标
     * 
     * @param ownerAccount 账户
     * @param security     股票
     * @return 股票指标
     */
    public StockIndicator calculateStockIndicator(OwnerAccount ownerAccount, Security security) {
        try {
            return INDICATOR_CACHE.get(security.toString(), () -> {
                // 根据账户配置获取K线周期
                // 根据账户配置获取K线周期
                CandlestickPeriod klinePeriod = AccountExtUtils.getKlinePeriod(ownerAccount);
                StockIndicator stockIndicator = new StockIndicator();
                // 期权标的价格
                SecurityQuote securityQuote = securityQuoteGateway.quote(ownerAccount, security);
                stockIndicator.setSecurityQuote(securityQuote);
                // 获取K线数据
                SecurityCandlestick candlesticks = candlestickGateway.getCandlesticks(ownerAccount,
                        security,
                        klinePeriod,
                        70,
                        CandlestickAdjustType.FORWARD_ADJUST);
                if (null == candlesticks || CollectionUtils.isEmpty(candlesticks.getCandlesticks())) {
                    return stockIndicator;
                }
                stockIndicator.setCandlesticks(candlesticks.getCandlesticks());
                stockIndicator.setPeriod(klinePeriod);
                int weekSize = CandlestickPeriod.WEEK.equals(klinePeriod) ? 2 : 5;
                int monthSize = CandlestickPeriod.WEEK.equals(klinePeriod) ? 5 : 20;
                SecurityCandlestick weekCandlesticks = summarySecurityCandlestick(candlesticks, weekSize);
                SecurityCandlestick monthCandlesticks = summarySecurityCandlestick(candlesticks, monthSize);

                if (null != weekCandlesticks && !CollectionUtils.isEmpty(weekCandlesticks.getCandlesticks())) {
                    Candlestick candlestick = weekCandlesticks.getCandlesticks().get(0);
                    // 获取K线波动幅度
                    BigDecimal priceRange = candlestick.getHigh().subtract(candlestick.getLow());
                    stockIndicator.setWeekPriceRange(priceRange);
                    stockIndicator.setWeekCandlestick(candlestick);
                }
                if (null != monthCandlesticks && !CollectionUtils.isEmpty(monthCandlesticks.getCandlesticks())) {
                    Candlestick candlestick = monthCandlesticks.getCandlesticks().get(0);
                    // 获取K线波动幅度
                    BigDecimal priceRange = candlestick.getHigh().subtract(candlestick.getLow());
                    stockIndicator.setMonthPriceRange(priceRange);
                    stockIndicator.setMonthCandlestick(candlestick);
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
                stockIndicator.addIndicator(IndicatorKey.EMA5.getKey(), getValue(new EMAIndicator(closePrice, 5), 20));
                stockIndicator.addIndicator(IndicatorKey.EMA20.getKey(), getValue(new EMAIndicator(closePrice, 20), 5));
                stockIndicator.addIndicator(IndicatorKey.EMA50.getKey(), getValue(new EMAIndicator(closePrice, 50), 0));

                // BOLL
                BollingerBandsMiddleIndicator bollMiddle = new BollingerBandsMiddleIndicator(
                        new SMAIndicator(closePrice, 20));
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

                return stockIndicator;
            });
        } catch (Exception e) {
            log.error("计算股票指标失败", e);
            return null;
        }
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
     * @param offset    偏移量
     * @return 指标值列表
     */
    private List<StockIndicatorItem> getValue(CachedIndicator<Num> indicator, int offset) {
        int endIndex = indicator.getBarSeries().getEndIndex();
        List<StockIndicatorItem> result = new ArrayList<>();
        DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd").toFormatter();
        // 排除掉 unstableBars 和 最早3天的数据
        for (int i = endIndex; i >= indicator.getUnstableBars() + offset; i--) {
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

    /**
     * 汇总蜡烛图数据
     * 
     * @param dayCandlesticks 蜡烛图数据
     * @param size            蜡烛图数量
     * @return 汇总后的蜡烛图数据
     */
    private SecurityCandlestick summarySecurityCandlestick(SecurityCandlestick dayCandlesticks, int size) {
        List<Candlestick> candlesticks = dayCandlesticks.getCandlesticks();
        if (candlesticks.size() < size) {
            return null;
        }
        // timestamp日期由远到近排序
        candlesticks.sort(Comparator.comparing(Candlestick::getTimestamp));
        int endIndex = candlesticks.size();
        List<Candlestick> subList = candlesticks.subList(endIndex - size, endIndex);
        BigDecimal close = BigDecimal.ZERO;
        BigDecimal open = BigDecimal.ZERO;
        BigDecimal low = BigDecimal.ZERO;
        BigDecimal high = BigDecimal.ZERO;
        Long volume = 0L;
        BigDecimal turnover = BigDecimal.ZERO;
        Long timestamp = 0L;
        for (int i = 0; i < subList.size(); i++) {
            Candlestick candlestick = subList.get(i);
            if (i == 0) {
                open = candlestick.getOpen();
                low = candlestick.getLow();
                high = candlestick.getHigh();
            }

            if (low.compareTo(candlestick.getLow()) > 0) {
                low = candlestick.getLow();
            }

            if (high.compareTo(candlestick.getHigh()) < 0) {
                high = candlestick.getHigh();
            }
            volume += candlestick.getVolume();
            turnover = turnover.add(candlestick.getTurnover());
            if (i == subList.size() - 1) {
                close = candlestick.getClose();
                timestamp = candlestick.getTimestamp();
            }
        }

        SecurityCandlestick summary = new SecurityCandlestick();
        summary.setSecurity(dayCandlesticks.getSecurity());
        List<Candlestick> summaryCandlesticks = new ArrayList<>();
        Candlestick summaryCandlestick = new Candlestick();
        summaryCandlestick.setHigh(high);
        summaryCandlestick.setLow(low);
        summaryCandlestick.setOpen(open);
        summaryCandlestick.setClose(close);
        summaryCandlestick.setVolume(volume);
        summaryCandlestick.setTurnover(turnover);
        summaryCandlestick.setTimestamp(timestamp);
        summaryCandlesticks.add(summaryCandlestick);
        summary.setCandlesticks(summaryCandlesticks);
        return summary;
    }

}
