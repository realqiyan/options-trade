package me.dingtou.options.strategy.impl;

import me.dingtou.options.model.*;
import me.dingtou.options.strategy.OptionsStrategy;
import me.dingtou.options.util.NumberUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 基础策略
 *
 * @author qiyan
 */
public abstract class BaseStrategy implements OptionsStrategy {

    /**
     * Sell年化收益率
     */
    private static final BigDecimal SELL_ANNUAL_YIELD = BigDecimal.valueOf(15);
    /**
     * call delta
     */
    private static final BigDecimal CALL_DELTA = BigDecimal.valueOf(0.3);
    /**
     * sell delta
     */
    private static final BigDecimal SELL_DELTA = BigDecimal.valueOf(0.3);


    /**
     * 继续加工
     *
     * @param optionsStrikeDate 期权到期日
     * @param optionsChain      期权链
     * @param strategySummary   策略信息（可选）
     */
    abstract void process(OptionsStrikeDate optionsStrikeDate, OptionsChain optionsChain, StrategySummary strategySummary);


    @Override
    final public void calculate(OptionsStrikeDate optionsStrikeDate, OptionsChain optionsChain, StrategySummary strategySummary) {
        StockIndicator stockIndicator = optionsChain.getStockIndicator();
        SecurityQuote securityQuote = stockIndicator.getSecurityQuote();
        BigDecimal securityPrice = securityQuote.getLastDone();
        BigDecimal dte = new BigDecimal(optionsStrikeDate.getOptionExpiryDateDistance());
        optionsChain.setTradeLevel(1);
        optionsChain.getOptionList().forEach(optionsTuple -> {
            Options call = optionsTuple.getCall();
            if (null != call) {
                OptionsStrategyData callStrategyData = new OptionsStrategyData();
                call.setStrategyData(callStrategyData);
                BigDecimal strikePrice = call.getOptionExData().getStrikePrice();
                //if (strikePrice.compareTo(securityPrice) > 0 && !BigDecimal.ZERO.equals(securityPrice)) {

                // 计算卖出收益
                BigDecimal sellCallAnnualYield = calculateAnnualYield(call, securityPrice, dte);
                callStrategyData.setSellAnnualYield(sellCallAnnualYield);

                // 是否推荐卖出
                int level = 0;
                BigDecimal delta = NumberUtils.scale(call.getRealtimeData().getDelta().abs());
                // delta小于0.30
                boolean deltaRecommend = delta.compareTo(CALL_DELTA) <= 0;
                // 卖出收益大于SELL_ANNUAL_YIELD
                boolean annualYieldRecommend = sellCallAnnualYield.compareTo(SELL_ANNUAL_YIELD) > 0;
                if (deltaRecommend && annualYieldRecommend) {
                    level++;
                    // 当前价+价格波幅 行权价大于这个值 level++
                    // 周价格波幅
                    BigDecimal maxWeekPriceRange = securityPrice.add(stockIndicator.getWeekPriceRange());
                    if (maxWeekPriceRange.compareTo(call.getOptionExData().getStrikePrice()) < 0) level++;
                    // 月价格波幅
                    BigDecimal maxMonthPriceRange = securityPrice.add(stockIndicator.getMonthPriceRange());
                    if (maxMonthPriceRange.compareTo(call.getOptionExData().getStrikePrice()) < 0) level++;
                }

                callStrategyData.setRecommendLevel(level);
                callStrategyData.setRecommend(deltaRecommend && annualYieldRecommend);

                // 涨跌幅
                callStrategyData.setRange(calculateRange(strikePrice, securityPrice));
                //}
            }

            Options put = optionsTuple.getPut();
            if (null != put) {
                OptionsStrategyData putStrategyData = new OptionsStrategyData();
                put.setStrategyData(putStrategyData);
                BigDecimal strikePrice = put.getOptionExData().getStrikePrice();
                //if (strikePrice.compareTo(securityPrice) < 0) {
                // 计算卖出收益
                int level = 0;
                BigDecimal sellPutAnnualYield = calculateAnnualYield(put, securityPrice, dte);
                putStrategyData.setSellAnnualYield(sellPutAnnualYield);
                // 是否推荐卖出
                BigDecimal delta = NumberUtils.scale(put.getRealtimeData().getDelta().abs());
                // delta小于0.30
                boolean deltaRecommend = delta.compareTo(SELL_DELTA) <= 0;
                // 卖出收益大于SELL_ANNUAL_YIELD
                boolean annualYieldRecommend = sellPutAnnualYield.compareTo(SELL_ANNUAL_YIELD) > 0;
                if (deltaRecommend && annualYieldRecommend) {
                    level++;
                    // 当前价-价格波幅 行权价小于这个值 level++
                    // 周价格波幅
                    BigDecimal minWeekPriceRange = securityPrice.subtract(stockIndicator.getWeekPriceRange());
                    if (minWeekPriceRange.compareTo(put.getOptionExData().getStrikePrice()) > 0) level++;
                    // 月价格波幅
                    BigDecimal minMonthPriceRange = securityPrice.subtract(stockIndicator.getMonthPriceRange());
                    if (minMonthPriceRange.compareTo(put.getOptionExData().getStrikePrice()) > 0) level++;
                }

                putStrategyData.setRecommendLevel(level);
                putStrategyData.setRecommend(deltaRecommend && annualYieldRecommend);

                // 涨跌幅
                putStrategyData.setRange(calculateRange(strikePrice, securityPrice));
                //}
            }
        });
        process(optionsStrikeDate, optionsChain, strategySummary);
    }

    /**
     * //（期权合约价格 x lotSize）/（DTE） * 一年 365 天/（除以股价 x lotSize）
     *
     * @param options       期权
     * @param securityPrice 股票价格
     * @param dte           天数
     * @return 年化收益
     */
    private BigDecimal calculateAnnualYield(Options options, BigDecimal securityPrice, BigDecimal dte) {
        BigDecimal lotSize = new BigDecimal(options.getBasic().getLotSize());
        OptionsRealtimeData realtimeData = options.getRealtimeData();
        if (null == realtimeData || BigDecimal.ZERO.equals(securityPrice)) {
            return BigDecimal.ZERO;
        }
        BigDecimal curPrice = realtimeData.getCurPrice();
        BigDecimal income = curPrice.multiply(lotSize);
        BigDecimal fee = calculateFee(options.getBasic().getSecurity());
        BigDecimal afterIncome = income.subtract(fee);
        BigDecimal totalPrice = securityPrice.multiply(lotSize);
        // 到期日当天也计算持有日
        dte = dte.add(BigDecimal.ONE);
        if (dte.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return NumberUtils.scale(afterIncome
                .divide(dte, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal(365))
                .divide(totalPrice, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)));
    }

    private BigDecimal calculateFee(Security security) {
        // 使用富途手续费预估： 佣金1.99 平台使用费0.3 证监会规费 0.01 期权监管费 0.012 期权清算费 0.02 期权交手费 0.18
        return BigDecimal.valueOf(2.52);
    }

    private BigDecimal calculateRange(BigDecimal strikePrice, BigDecimal securityPrice) {
        if (BigDecimal.ZERO.equals(securityPrice)) {
            return BigDecimal.ZERO;
        }
        return NumberUtils.scale(strikePrice.subtract(securityPrice)
                .divide(securityPrice, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)));
    }
}
