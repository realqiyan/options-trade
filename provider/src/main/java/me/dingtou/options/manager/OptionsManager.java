package me.dingtou.options.manager;

import me.dingtou.options.constant.CandlestickAdjustType;
import me.dingtou.options.constant.CandlestickPeriod;
import me.dingtou.options.gateway.CandlestickGateway;
import me.dingtou.options.gateway.OptionsChainGateway;
import me.dingtou.options.gateway.SecurityQuoteGateway;
import me.dingtou.options.model.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
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

    public List<OptionsStrikeDate> queryOptionsExpDate(String code, Integer market) {
        if (StringUtils.isBlank(code) || null == market) {
            return Collections.emptyList();
        }
        return optionsChainGateway.getOptionsExpDate(Security.of(code, market));
    }

    public OptionsChain queryOptionsChain(Security security, OptionsStrikeDate optionsStrikeDate) {
        if (null == security || null == optionsStrikeDate || null == optionsStrikeDate.getStrikeTime()) {
            return null;
        }
        // 期权标的价格
        SecurityQuote securityQuote = securityQuoteGateway.quote(security);


        // 期权链
        OptionsChain optionsChain = optionsChainGateway.queryOptionsChain(security, optionsStrikeDate.getStrikeTime(), securityQuote.getLastDone());
        StockIndicator stockIndicator = new StockIndicator();
        optionsChain.setStockIndicator(stockIndicator);
        stockIndicator.setSecurityQuote(securityQuote);

        // 日K线
        SecurityCandlestick dayCandlesticks = candlestickGateway.getCandlesticks(security, CandlestickPeriod.DAY, 50, CandlestickAdjustType.FORWARD_ADJUST);
        if (null != dayCandlesticks && !CollectionUtils.isEmpty(dayCandlesticks.getCandlesticks())) {

            SecurityCandlestick weekCandlesticks = summarySecurityCandlestick(dayCandlesticks, 5);
            SecurityCandlestick monthCandlesticks = summarySecurityCandlestick(dayCandlesticks, 25);

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
            BarSeries barSeries = convertToBarSeries(dayCandlesticks);
            ClosePriceIndicator closePrice = new ClosePriceIndicator(barSeries);

            stockIndicator.setRsi(getValueList(new RSIIndicator(closePrice, 14)));
            stockIndicator.setEma5(getValueList(new EMAIndicator(closePrice, 5)));
            stockIndicator.setEma20(getValueList(new EMAIndicator(closePrice, 20)));
            stockIndicator.setMacd(getValueList(new MACDIndicator(closePrice, 12, 26)));

        }

        return optionsChain;
    }

    private List<BigDecimal> getValueList(CachedIndicator<Num> indicator) {
        int endIndex = indicator.getBarSeries().getEndIndex();
        List<BigDecimal> result = new ArrayList<>();
        // 排除掉 unstableBars 和 最早3天的数据
        for (int i = endIndex; i >= indicator.getUnstableBars() + 3; i--) {
            Num value = indicator.getValue(i);
            result.add(value.bigDecimalValue().setScale(2, RoundingMode.HALF_UP));
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
