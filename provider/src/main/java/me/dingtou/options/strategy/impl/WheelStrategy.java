package me.dingtou.options.strategy.impl;


import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.model.*;
import me.dingtou.options.strategy.OptionsStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@Component
public class WheelStrategy extends BaseStrategy implements OptionsStrategy {


    @Override
    public boolean isSupport(OwnerStrategy strategy) {
        return null != strategy && "wheel_strategy".equals(strategy.getStrategyCode());
    }


    @Override
    public void process(OptionsStrikeDate optionsStrikeDate, OptionsChain optionsChain, StrategySummary strategySummary) {
        // 当前策略信息
        if (null == strategySummary) {
            log.warn("策略信息为空，请检查！");
            return;
        }

        Integer holdSecurityNum = strategySummary.getHoldSecurityNum();
        // 空仓执行sell put 否则执行cc
        boolean isSellPutStage = null == holdSecurityNum || holdSecurityNum == 0;
        boolean isCoveredCallStage = !isSellPutStage;


        // 如果是cc阶段，需要找到最近的指派订单
        OwnerOrder currentUnderlyingOrder = null;
        if (isCoveredCallStage) {
            Optional<OwnerOrder> optionalOwnerOrder = strategySummary.getStrategyOrders().stream().filter(order -> order.getCode().equals(order.getUnderlyingCode())).findFirst();
            if (optionalOwnerOrder.isPresent()) {
                currentUnderlyingOrder = optionalOwnerOrder.get();
            }
        }
        final OwnerOrder finalUnderlyingOrder = currentUnderlyingOrder;

        optionsChain.getOptionList().forEach(optionsTuple -> {
            Options call = optionsTuple.getCall();
            if (null != call) {
                if (isSellPutStage) {
                    call.getStrategyData().setRecommend(false);
                    call.getStrategyData().setRecommendLevel(0);
                }
                if (isCoveredCallStage) {
                    // call的价格要高于指派的股票价格
                    if (null != finalUnderlyingOrder && call.getOptionExData().getStrikePrice().compareTo(finalUnderlyingOrder.getPrice()) < 0) {
                        call.getStrategyData().setRecommend(false);
                        call.getStrategyData().setRecommendLevel(0);
                    }
                }
            }

            Options put = optionsTuple.getPut();
            if (null != put) {
                if (isCoveredCallStage) {
                    put.getStrategyData().setRecommend(false);
                    put.getStrategyData().setRecommendLevel(0);
                }
            }
        });


        // AI分析提示词
        SecurityQuote securityQuote = optionsChain.getSecurityQuote();
        BigDecimal securityPrice = securityQuote.getLastDone();
        StringBuilder aiPrompt = new StringBuilder();
        aiPrompt.append("我在做期权的车轮策略（WheelStrategy），底层资产是").append(securityQuote.getSecurity().toString())
                .append("，当前阶段是").append(isSellPutStage ? "卖出看跌期权（Cash-Secured Put）" : "卖出看涨期权（Covered Call）")
                .append("，当前股价").append(securityPrice)
                .append("，近一周价格波动").append(optionsChain.getWeekPriceRange())
                .append("，近一月价格波动").append(optionsChain.getMonthPriceRange())
                .append("，当前期权距离到期时间").append(optionsStrikeDate.getOptionExpiryDateDistance())
                .append("天，我当前计划交易的期权实时信息如下：\n");
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
        optionsChain.setAiPrompt(aiPrompt.toString());
    }

    private void buildOptionsAiPrompt(StringBuilder aiPrompt, Options options) {
        if (Boolean.FALSE.equals(options.getStrategyData().getRecommend())) {
            return;
        }
        aiPrompt.append("标的:").append(options.getBasic().getSecurity().getCode())
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
}
