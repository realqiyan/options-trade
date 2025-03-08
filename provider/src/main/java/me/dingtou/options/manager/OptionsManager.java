package me.dingtou.options.manager;

import me.dingtou.options.constant.CandlestickAdjustType;
import me.dingtou.options.constant.CandlestickPeriod;
import me.dingtou.options.constant.IndicatorKey;
import me.dingtou.options.gateway.CandlestickGateway;
import me.dingtou.options.gateway.OptionsChainGateway;
import me.dingtou.options.gateway.SecurityQuoteGateway;
import me.dingtou.options.gateway.VixQueryGateway;
import me.dingtou.options.model.*;
import me.dingtou.options.util.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.*;
import org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsUpperIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Component
public class OptionsManager {

    @Autowired
    private OptionsChainGateway optionsChainGateway;

    @Autowired
    private SecurityQuoteGateway securityQuoteGateway;

    @Autowired
    private CandlestickGateway candlestickGateway;

    @Autowired
    private VixQueryGateway vixQueryGateway;

    public List<OptionsStrikeDate> queryOptionsExpDate(String code, Integer market) {
        if (StringUtils.isBlank(code) || null == market) {
            return Collections.emptyList();
        }
        return optionsChainGateway.getOptionsExpDate(Security.of(code, market));
    }

    public OptionsChain queryOptionsChain(OwnerAccount ownerAccount, Security security, OptionsStrikeDate optionsStrikeDate) {
        if (null == security || null == optionsStrikeDate || null == optionsStrikeDate.getStrikeTime()) {
            return null;
        }
        // 期权标的价格
        SecurityQuote securityQuote = securityQuoteGateway.quote(ownerAccount, security);


        // 期权链
        OptionsChain optionsChain = optionsChainGateway.queryOptionsChain(security, optionsStrikeDate.getStrikeTime(), securityQuote.getLastDone());


        // 策略分析提供基础指标数据
        StockIndicator stockIndicator = new StockIndicator();
        stockIndicator.setSecurityQuote(securityQuote);
        // 日K线
        // SecurityCandlestick candlesticks = candlestickGateway.getCandlesticks(security, CandlestickPeriod.DAY, 60, CandlestickAdjustType.FORWARD_ADJUST);
        // 周K线
        SecurityCandlestick candlesticks = candlestickGateway.getCandlesticks(ownerAccount, security, CandlestickPeriod.WEEK, 70, CandlestickAdjustType.FORWARD_ADJUST);
        if (null != candlesticks && !CollectionUtils.isEmpty(candlesticks.getCandlesticks())) {
            stockIndicator.setWeekCandlesticks(candlesticks.getCandlesticks());

            SecurityCandlestick weekCandlesticks = summarySecurityCandlestick(candlesticks, 2);
            SecurityCandlestick monthCandlesticks = summarySecurityCandlestick(candlesticks, 5);

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
            stockIndicator.addIndicator(IndicatorKey.RSI.getKey(), getValueList(new RSIIndicator(closePrice, 14), 14));
            // MACD
            stockIndicator.addIndicator(IndicatorKey.MACD.getKey(), getValueList(new MACDIndicator(closePrice, 12, 26), 26));
            // EMA
            stockIndicator.addIndicator(IndicatorKey.EMA5.getKey(), getValueList(new EMAIndicator(closePrice, 5), 20));
            stockIndicator.addIndicator(IndicatorKey.EMA50.getKey(), getValueList(new EMAIndicator(closePrice, 50), 0));
            // BOLL
            BollingerBandsMiddleIndicator bollingerMiddle = new BollingerBandsMiddleIndicator(new SMAIndicator(closePrice, 20));
            Indicator<Num> deviation = new StandardDeviationIndicator(closePrice, 20);
            Num k = DecimalNum.valueOf(2);
            BollingerBandsUpperIndicator bollingerUpper = new BollingerBandsUpperIndicator(bollingerMiddle, deviation, k); // 上轨通常是中轨加上2倍标准差
            BollingerBandsLowerIndicator bollingerLower = new BollingerBandsLowerIndicator(bollingerMiddle, deviation, k); // 下轨通常是中轨减去2倍标准差
            stockIndicator.addIndicator(IndicatorKey.BOLL_MIDDLE.getKey(), getValueList(bollingerMiddle, 20));
            stockIndicator.addIndicator(IndicatorKey.BOLL_UPPER.getKey(), getValueList(bollingerUpper, 20));
            stockIndicator.addIndicator(IndicatorKey.BOLL_LOWER.getKey(), getValueList(bollingerLower, 20));

        }
        optionsChain.setStockIndicator(stockIndicator);
        VixIndicator vixIndicator = vixQueryGateway.queryCurrentVix();
        optionsChain.setVixIndicator(vixIndicator);

        return optionsChain;
    }

    private List<StockIndicatorItem> getValueList(CachedIndicator<Num> indicator, int offset) {
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

    private BarSeries convertToBarSeries(SecurityCandlestick candlesticks) {
        BarSeries series = new BaseBarSeriesBuilder().withName(candlesticks.getSecurity().toString()).build();
        for (Candlestick candlestick : candlesticks.getCandlesticks()) {
            // candlestick.getTimestamp()转ZonedDateTime
            Instant instant = Instant.ofEpochSecond(candlestick.getTimestamp());
            series.addBar(instant.atZone(ZoneId.systemDefault()),
                    candlestick.getOpen(),
                    candlestick.getHigh(),
                    candlestick.getLow(),
                    candlestick.getClose(),
                    candlestick.getVolume(),
                    candlestick.getTurnover());
        }
        return series;
    }

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
        ArrayList<Candlestick> summaryCandlesticks = new ArrayList<>();
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
