package me.dingtou.options.strategy.impl;

import me.dingtou.options.model.*;
import me.dingtou.options.strategy.OptionsStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DefaultSellStrategy extends BaseStrategy implements OptionsStrategy {


    @Override
    void process(OptionsStrikeDate optionsStrikeDate, OptionsChain optionsChain, StrategySummary strategySummary) {
        String aiPrompt = buildAiPrompt(optionsStrikeDate, optionsChain);
        optionsChain.setAiPrompt(aiPrompt);
    }

    private String buildAiPrompt(OptionsStrikeDate optionsStrikeDate, OptionsChain optionsChain) {
        optionsChain.getOptionList().forEach(optionsTuple -> {
            Options call = optionsTuple.getCall();
            if (null != call) {
                call.getRealtimeData().setDelta(call.getRealtimeData().getDelta().multiply(BigDecimal.valueOf(-1)));
                call.getRealtimeData().setTheta(call.getRealtimeData().getTheta().multiply(BigDecimal.valueOf(-1)));
            }
            Options put = optionsTuple.getPut();
            if (null != put) {
                put.getRealtimeData().setDelta(put.getRealtimeData().getDelta().multiply(BigDecimal.valueOf(-1)));
                put.getRealtimeData().setTheta(put.getRealtimeData().getTheta().multiply(BigDecimal.valueOf(-1)));
            }
        });
        // AI分析提示词
        SecurityQuote securityQuote = optionsChain.getSecurityQuote();
        BigDecimal securityPrice = securityQuote.getLastDone();
        StringBuilder aiPrompt = new StringBuilder();
        // 股票{{=d.currentCode}}当前股价{{=lastDone}},近一周价格波动{{=d.weekPriceRange}}，近一月价格波动{{=d.monthPriceRange}}，当前齐全
        aiPrompt.append("我在做卖期权的策略，目的是通过卖").append(securityQuote.getSecurity().toString())
                .append("的期权赚取权利金，当前股价").append(securityPrice)
                .append("，近一周价格波动").append(optionsChain.getWeekPriceRange())
                .append("，近一月价格波动").append(optionsChain.getMonthPriceRange())
                .append("，当前期权距离到期时间").append(optionsStrikeDate.getOptionExpiryDateDistance())
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
        aiPrompt.append("请帮我分析这些期权标的，告诉我是否适合交易，给我最优交易建议。");
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
                .append("，行权价:").append(options.getOptionExData().getStrikePrice())
                .append("，当前价格:").append(options.getRealtimeData().getCurPrice())
                .append("，隐含波动率:").append(options.getRealtimeData().getImpliedVolatility())
                .append("，Delta:").append(options.getRealtimeData().getDelta())
                .append("，Theta:").append(options.getRealtimeData().getTheta())
                .append("，Gamma:").append(options.getRealtimeData().getGamma())
                .append("，未平仓合约数:").append(options.getRealtimeData().getOpenInterest())
                .append("，当天交易量:").append(options.getRealtimeData().getVolume())
                .append("，预估年化收益率:").append(options.getStrategyData().getSellAnnualYield())
                .append("%，距离行权价涨跌幅:").append(options.getStrategyData().getRange())
                .append("%，我的购买倾向").append(options.getStrategyData().getRecommendLevel() <= 2 ? "一般" : "较强")
                .append("；\n");
    }

    @Override
    public boolean isSupport(OwnerStrategy strategy) {
        return null == strategy || "default".equalsIgnoreCase(strategy.getStrategyCode());
    }
}
