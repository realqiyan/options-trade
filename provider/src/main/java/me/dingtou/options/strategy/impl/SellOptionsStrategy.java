package me.dingtou.options.strategy.impl;

import me.dingtou.options.model.*;
import me.dingtou.options.strategy.OptionsStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class SellOptionsStrategy implements OptionsStrategy {

    /**
     * Sell年化收益率
     */
    private static final BigDecimal SELL_ANNUAL_YIELD = BigDecimal.valueOf(15);

    @Override
    public void calculate(OptionsStrikeDate optionsStrikeDate, OptionsChain optionsChain) {
        SecurityQuote securityQuote = optionsChain.getSecurityQuote();
        BigDecimal securityPrice = securityQuote.getLastDone();
        BigDecimal dte = new BigDecimal(optionsStrikeDate.getOptionExpiryDateDistance());
        optionsChain.getOptionList().forEach(optionsTuple -> {
            Options call = optionsTuple.getCall();
            if (null != call) {
                BigDecimal strikePrice = call.getOptionExData().getStrikePrice();
                if (strikePrice.compareTo(securityPrice) > 0 && !BigDecimal.ZERO.equals(securityPrice)) {
                    OptionsStrategyData callStrategyData = new OptionsStrategyData();
                    // 计算卖出收益
                    BigDecimal sellCallAnnualYield = calculateAnnualYield(call, securityPrice, dte);
                    callStrategyData.setSellAnnualYield(sellCallAnnualYield);

                    // 是否推荐卖出
                    int level = 0;
                    BigDecimal delta = call.getRealtimeData().getDelta().abs().setScale(4, RoundingMode.HALF_UP);
                    // delta小于0.20
                    boolean deltaRecommend = delta.compareTo(BigDecimal.valueOf(0.20)) <= 0;
                    // 卖出收益大于SELL_ANNUAL_YIELD
                    boolean annualYieldRecommend = sellCallAnnualYield.compareTo(SELL_ANNUAL_YIELD) > 0;
                    if (deltaRecommend && annualYieldRecommend) {
                        level++;
                        // 当前价+价格波幅 行权价大于这个值 level++
                        // 周价格波幅
                        BigDecimal maxWeekPriceRange = securityPrice.add(optionsChain.getWeekPriceRange());
                        if (maxWeekPriceRange.compareTo(call.getOptionExData().getStrikePrice()) < 0) level++;
                        // 月价格波幅
                        BigDecimal maxMonthPriceRange = securityPrice.add(optionsChain.getMonthPriceRange());
                        if (maxMonthPriceRange.compareTo(call.getOptionExData().getStrikePrice()) < 0) level++;
                    }

                    callStrategyData.setRecommendLevel(level);
                    callStrategyData.setRecommend(deltaRecommend && annualYieldRecommend);

                    // 涨跌幅
                    callStrategyData.setRange(calculateRange(strikePrice, securityPrice));

                    call.setStrategyData(callStrategyData);

                }
            }

            Options put = optionsTuple.getPut();
            if (null != put) {
                BigDecimal strikePrice = put.getOptionExData().getStrikePrice();
                if (strikePrice.compareTo(securityPrice) < 0) {
                    OptionsStrategyData putStrategyData = new OptionsStrategyData();
                    // 计算卖出收益
                    int level = 0;
                    BigDecimal sellPutAnnualYield = calculateAnnualYield(put, securityPrice, dte);
                    putStrategyData.setSellAnnualYield(sellPutAnnualYield);
                    // 是否推荐卖出
                    BigDecimal delta = put.getRealtimeData().getDelta().abs().setScale(4, RoundingMode.HALF_UP);
                    // delta小于0.30
                    boolean deltaRecommend = delta.compareTo(BigDecimal.valueOf(0.30)) <= 0;
                    // 卖出收益大于SELL_ANNUAL_YIELD
                    boolean annualYieldRecommend = sellPutAnnualYield.compareTo(SELL_ANNUAL_YIELD) > 0;
                    if (deltaRecommend && annualYieldRecommend) {
                        level++;
                        // 当前价-价格波幅 行权价小于这个值 level++
                        // 周价格波幅
                        BigDecimal minWeekPriceRange = securityPrice.subtract(optionsChain.getWeekPriceRange());
                        if (minWeekPriceRange.compareTo(put.getOptionExData().getStrikePrice()) > 0) level++;
                        // 月价格波幅
                        BigDecimal minMonthPriceRange = securityPrice.subtract(optionsChain.getMonthPriceRange());
                        if (minMonthPriceRange.compareTo(put.getOptionExData().getStrikePrice()) > 0) level++;
                    }

                    putStrategyData.setRecommendLevel(level);
                    putStrategyData.setRecommend(deltaRecommend && annualYieldRecommend);

                    // 涨跌幅
                    putStrategyData.setRange(calculateRange(strikePrice, securityPrice));
                    put.setStrategyData(putStrategyData);
                }
            }
        });

        String aiPrompt = buildAiPrompt(optionsStrikeDate, optionsChain);
        optionsChain.setAiPrompt(aiPrompt);
    }

    private String buildAiPrompt(OptionsStrikeDate optionsStrikeDate, OptionsChain optionsChain) {
        // AI分析提示词
        SecurityQuote securityQuote = optionsChain.getSecurityQuote();
        BigDecimal securityPrice = securityQuote.getLastDone();
        StringBuilder aiPrompt = new StringBuilder();
        // 股票{{=d.currentCode}}当前股价{{=lastDone}},近一周价格波动{{=d.weekPriceRange}}，近一月价格波动{{=d.monthPriceRange}}，当前齐全
        aiPrompt.append("我在做卖期权的策略，底层资产是")
                .append(securityQuote.getSecurity().toString())
                .append("，当前股价")
                .append(securityPrice)
                .append("，近一周价格波动")
                .append(optionsChain.getWeekPriceRange())
                .append("，近一月价格波动")
                .append(optionsChain.getMonthPriceRange())
                .append("，当前期权距离到期时间")
                .append(optionsStrikeDate.getOptionExpiryDateDistance())
                .append("天，准备交易的期权实时信息如下：\n");
        optionsChain.getOptionList().forEach(optionsTuple -> {
            Options call = optionsTuple.getCall();
            if (null != call) {
                buildOptionsAiPrompt(aiPrompt, call);
            }
            Options put = optionsTuple.getPut();
            if (null != put) {
                buildOptionsAiPrompt(aiPrompt, put);
            }
        });
        aiPrompt.append("帮我分析以上期权标的信息，请给我交易建议。");
        return aiPrompt.toString();
    }

    private void buildOptionsAiPrompt(StringBuilder aiPrompt, Options options) {
        if (Boolean.FALSE.equals(options.getStrategyData().getRecommend())) {
            return;
        }
        if (Integer.valueOf(1).equals(options.getOptionExData().getType())) {
            aiPrompt.append("SellCall标的:");
        } else if (Integer.valueOf(2).equals(options.getOptionExData().getType())) {
            aiPrompt.append("SellPut标的:");
        }
        aiPrompt.append(options.getBasic().getSecurity().getCode())
                .append("，行权价")
                .append(options.getOptionExData().getStrikePrice())
                .append("，当前价格")
                .append(options.getRealtimeData().getCurPrice())
                .append("，隐含波动率")
                .append(options.getRealtimeData().getImpliedVolatility())
                .append("，Delta")
                .append(options.getRealtimeData().getDelta())
                .append("，Theta")
                .append(options.getRealtimeData().getTheta())
                .append("，Gamma")
                .append(options.getRealtimeData().getGamma())
                .append("，未平仓合约数")
                .append(options.getRealtimeData().getOpenInterest())
                .append("，当天交易量")
                .append(options.getRealtimeData().getVolume())
                .append("，预估年化收益率")
                .append(options.getStrategyData().getSellAnnualYield())
                .append("%，距离行权价涨跌幅")
                .append(options.getStrategyData().getRange())
                .append("%，购买倾向分")
                .append(options.getStrategyData().getRecommendLevel())
                .append("；\n");
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
        if (null == realtimeData) {
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
        return afterIncome
                .divide(dte, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal(365))
                .divide(totalPrice, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateFee(Security security) {
        // 使用富途手续费预估： 佣金1.99 平台使用费0.3 证监会规费 0.01 期权监管费 0.012 期权清算费 0.02 期权交手费 0.18
        return BigDecimal.valueOf(2.52);
    }

    private BigDecimal calculateRange(BigDecimal strikePrice, BigDecimal securityPrice) {
        return strikePrice.subtract(securityPrice)
                .divide(securityPrice, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
