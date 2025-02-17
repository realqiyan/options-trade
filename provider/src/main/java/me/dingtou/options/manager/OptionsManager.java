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

import java.math.BigDecimal;
import java.util.Collections;
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

        // 周K线
        SecurityCandlestick weekCandlesticks = candlestickGateway.getCandlesticks(security, CandlestickPeriod.WEEK, 1, CandlestickAdjustType.FORWARD_ADJUST);
        // 月K线
        SecurityCandlestick monthCandlesticks = candlestickGateway.getCandlesticks(security, CandlestickPeriod.MONTH, 1, CandlestickAdjustType.FORWARD_ADJUST);


        // 期权链
        OptionsChain optionsChain = optionsChainGateway.queryOptionsChain(security, optionsStrikeDate.getStrikeTime(), securityQuote.getLastDone());
        optionsChain.setSecurityQuote(securityQuote);

        // K线信息
        if (!weekCandlesticks.getCandlesticks().isEmpty()) {
            Candlestick candlestick = weekCandlesticks.getCandlesticks().get(0);
            // 获取K线波动幅度
            BigDecimal priceRange = candlestick.getHigh().subtract(candlestick.getLow());
            optionsChain.setWeekPriceRange(priceRange);
            optionsChain.setWeekCandlestick(candlestick);
        }
        if (!monthCandlesticks.getCandlesticks().isEmpty()) {
            Candlestick candlestick = monthCandlesticks.getCandlesticks().get(0);
            // 获取K线波动幅度
            BigDecimal priceRange = candlestick.getHigh().subtract(candlestick.getLow());
            optionsChain.setMonthPriceRange(priceRange);
            optionsChain.setMonthCandlestick(candlestick);
        }

        // 恐慌指数
        //Security vixSecurity = Security.of("VIX", Market.US.getCode());
        //SecurityQuote vixQuote = securityQuoteGateway.quote(vixSecurity);
        //optionsChain.setVixQuote(vixQuote);

        return optionsChain;
    }

}
