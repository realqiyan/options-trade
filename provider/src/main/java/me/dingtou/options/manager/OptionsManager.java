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

import java.math.BigDecimal;
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
        optionsChain.setSecurityQuote(securityQuote);

        // 日K线
        SecurityCandlestick dayCandlesticks = candlestickGateway.getCandlesticks(security, CandlestickPeriod.DAY, 25, CandlestickAdjustType.FORWARD_ADJUST);
        if (null != dayCandlesticks && !CollectionUtils.isEmpty(dayCandlesticks.getCandlesticks())) {

            SecurityCandlestick weekCandlesticks = summarySecurityCandlestick(dayCandlesticks, 5);
            SecurityCandlestick monthCandlesticks = summarySecurityCandlestick(dayCandlesticks, dayCandlesticks.getCandlesticks().size());

            if (null != weekCandlesticks && !CollectionUtils.isEmpty(weekCandlesticks.getCandlesticks())) {
                Candlestick candlestick = weekCandlesticks.getCandlesticks().get(0);
                // 获取K线波动幅度
                BigDecimal priceRange = candlestick.getHigh().subtract(candlestick.getLow());
                optionsChain.setWeekPriceRange(priceRange);
                optionsChain.setWeekCandlestick(candlestick);
            }
            if (null != monthCandlesticks && !CollectionUtils.isEmpty(monthCandlesticks.getCandlesticks())) {
                Candlestick candlestick = monthCandlesticks.getCandlesticks().get(0);
                // 获取K线波动幅度
                BigDecimal priceRange = candlestick.getHigh().subtract(candlestick.getLow());
                optionsChain.setMonthPriceRange(priceRange);
                optionsChain.setMonthCandlestick(candlestick);
            }
        }

        // 恐慌指数
        //Security vixSecurity = Security.of("VIX", Market.US.getCode());
        //SecurityQuote vixQuote = securityQuoteGateway.quote(vixSecurity);
        //optionsChain.setVixQuote(vixQuote);

        return optionsChain;
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
