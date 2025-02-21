package me.dingtou.options.strategy.impl;

import me.dingtou.options.model.*;
import me.dingtou.options.strategy.OptionsStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DefaultSellStrategy extends BaseStrategy implements OptionsStrategy {


    @Override
    void process(OptionsStrikeDate optionsStrikeDate, OptionsChain optionsChain, StrategySummary strategySummary) {
        String prompt = buildPrompt(optionsStrikeDate, optionsChain);
        optionsChain.setPrompt(prompt);
    }

    private String buildPrompt(OptionsStrikeDate optionsStrikeDate, OptionsChain optionsChain) {
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
        StockIndicator stockIndicator = optionsChain.getStockIndicator();
        SecurityQuote securityQuote = stockIndicator.getSecurityQuote();
        BigDecimal securityPrice = securityQuote.getLastDone();
        StringBuilder prompt = new StringBuilder();
        // 股票{{=d.currentCode}}当前股价{{=lastDone}},近一周价格波动{{=d.weekPriceRange}}，近一月价格波动{{=d.monthPriceRange}}，当前齐全
        prompt.append("我在做卖期权的策略，目的是通过卖").append(securityQuote.getSecurity().toString())
                .append("的期权赚取权利金，当前股价").append(securityPrice)
                .append("，近一周价格波动").append(stockIndicator.getWeekPriceRange())
                .append("，近一月价格波动").append(stockIndicator.getMonthPriceRange())
                .append("，当前EMA5为").append(stockIndicator.getEma5().get(0))
                .append("（最近几天由近到远的EMA5分别是：").append(stockIndicator.getEma5().toString())
                .append("），当前EMA20为").append(stockIndicator.getEma20().get(0))
                .append("（最近几天由近到远的EMA20分别是：").append(stockIndicator.getEma20().toString())
                .append("），当前RSI为").append(stockIndicator.getRsi().get(0))
                .append("（最近几天由近到远的RSI分别是：").append(stockIndicator.getRsi().toString())
                .append("），当前MACD为").append(stockIndicator.getMacd().get(0))
                .append("（最近几天由近到远的MACD分别是：").append(stockIndicator.getMacd().toString())
                .append("），当前期权距离到期时间").append(optionsStrikeDate.getOptionExpiryDateDistance())
                .append("，当前期权距离到期时间").append(optionsStrikeDate.getOptionExpiryDateDistance())
                .append("天，准备交易的期权实时信息如下：\n");
        optionsChain.getOptionList().forEach(optionsTuple -> {
            Options call = optionsTuple.getCall();
            if (null != call) {
                buildOptionsPrompt(prompt, call);
            }
            Options put = optionsTuple.getPut();
            if (null != put) {
                buildOptionsPrompt(prompt, put);
            }
        });
        prompt.append("请帮我分析这些期权标的，告诉我是否适合交易，给我最优交易建议。");
        return prompt.toString();
    }

    private void buildOptionsPrompt(StringBuilder prompt, Options options) {
        if (null == options.getStrategyData() || Boolean.FALSE.equals(options.getStrategyData().getRecommend())) {
            return;
        }
        if (Integer.valueOf(1).equals(options.getOptionExData().getType())) {
            prompt.append("SellCall标的:");
        } else if (Integer.valueOf(2).equals(options.getOptionExData().getType())) {
            prompt.append("SellPut标的:");
        }
        prompt.append(options.getBasic().getSecurity().getCode())
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
