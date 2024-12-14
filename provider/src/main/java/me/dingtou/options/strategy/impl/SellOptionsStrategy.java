package me.dingtou.options.strategy.impl;

import me.dingtou.options.model.*;
import me.dingtou.options.strategy.OptionsStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class SellOptionsStrategy implements OptionsStrategy {
    @Override
    public void calculate(OptionsStrikeDate optionsStrikeDate, OptionsChain optionsChain) {
        SecurityQuote securityQuote = optionsChain.getSecurityQuote();
        BigDecimal securityPrice = securityQuote.getLastDone();
        BigDecimal dte = new BigDecimal(optionsStrikeDate.getOptionExpiryDateDistance());
        optionsChain.getOptionList().forEach(optionsTuple -> {
            Options call = optionsTuple.getCall();
            if (null != call && call.getOptionExData().getStrikePrice().compareTo(securityPrice) > 0) {
                OptionsStrategyData callStrategyData = new OptionsStrategyData();
                BigDecimal sellCallAnnualYield = calculateAnnualYield(call, securityPrice, dte);
                callStrategyData.setSellAnnualYield(sellCallAnnualYield);
                call.setStrategyData(callStrategyData);
            }

            Options put = optionsTuple.getPut();
            if (null != put && put.getOptionExData().getStrikePrice().compareTo(securityPrice) < 0) {
                OptionsStrategyData putStrategyData = new OptionsStrategyData();
                BigDecimal sellPutAnnualYield = calculateAnnualYield(put, securityPrice, dte);
                putStrategyData.setSellAnnualYield(sellPutAnnualYield);
                put.setStrategyData(putStrategyData);
            }
        });
    }

    /**
     * //（期权合约价格 x lotSize）/（DTE） * 一年 365 天/（除以股价 x lotSize）
     *
     * @param options       期权
     * @param securityPrice 股票价格
     * @param dte           天数
     * @return 年化收益
     */
    private static BigDecimal calculateAnnualYield(Options options, BigDecimal securityPrice, BigDecimal dte) {
        BigDecimal lotSize = new BigDecimal(options.getBasic().getLotSize());
        OptionsRealtimeData realtimeData = options.getRealtimeData();
        if (null == realtimeData) {
            return BigDecimal.ZERO;
        }
        BigDecimal income = realtimeData.getCurPrice().multiply(lotSize);
        BigDecimal fee = calculateFee(options.getBasic().getSecurity());
        BigDecimal afterIncome = income.subtract(fee);
        BigDecimal totalPrice = securityPrice.multiply(lotSize);
        // 到期日当天也计算持有日
        dte = dte.add(BigDecimal.ONE);
        if (dte.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return afterIncome
                .divide(dte, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal(365))
                .divide(totalPrice, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal calculateFee(Security security) {
        // 富途手续费预估
        return BigDecimal.valueOf(2.52);
    }
}
