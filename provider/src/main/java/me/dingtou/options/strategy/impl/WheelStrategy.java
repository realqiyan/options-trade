package me.dingtou.options.strategy.impl;


import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.constant.IndicatorKey;
import me.dingtou.options.model.*;
import me.dingtou.options.strategy.OptionsStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
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

        Integer holdStockNum = strategySummary.getHoldStockNum();
        // 空仓执行sell put 否则执行cc
        boolean isSellPutStage = null == holdStockNum || holdStockNum == 0;
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
                call.getRealtimeData().setDelta(call.getRealtimeData().getDelta().multiply(BigDecimal.valueOf(-1)));
                call.getRealtimeData().setTheta(call.getRealtimeData().getTheta().multiply(BigDecimal.valueOf(-1)));
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
                put.getRealtimeData().setDelta(put.getRealtimeData().getDelta().multiply(BigDecimal.valueOf(-1)));
                put.getRealtimeData().setTheta(put.getRealtimeData().getTheta().multiply(BigDecimal.valueOf(-1)));
                if (isCoveredCallStage) {
                    put.getStrategyData().setRecommend(false);
                    put.getStrategyData().setRecommendLevel(0);
                }
            }
        });

        VixIndicator vixIndicator = optionsChain.getVixIndicator();
        StockIndicator stockIndicator = optionsChain.getStockIndicator();


        int tradeLevel = tradeLevel(vixIndicator, stockIndicator);
        optionsChain.setTradeLevel(tradeLevel);


        // AI分析提示词

        SecurityQuote securityQuote = stockIndicator.getSecurityQuote();

        BigDecimal securityPrice = securityQuote.getLastDone();
        StringBuilder prompt = new StringBuilder();
        prompt.append("我在做期权的车轮策略（WheelStrategy），我会检查VIX是否大于30，VIX大于30我会暂停交易，其次会检查RSI，RSI小于30时MACD指标动量向上我才会继续交易，我还会检查关键支撑位")
                .append("，当前操作的底层资产是").append(securityQuote.getSecurity().toString())
                .append("，当前阶段是").append(isSellPutStage ? "卖出看跌期权（Cash-Secured Put）" : "卖出看涨期权（Covered Call）");

        if (isCoveredCallStage && null != finalUnderlyingOrder) {
            prompt.append("，当前指派的股票价格是").append(finalUnderlyingOrder.getPrice());
        }
        if (null != vixIndicator && null != vixIndicator.getCurrentVix()) {
            prompt.append("，当前VIX指数为").append(vixIndicator.getCurrentVix().getValue());
        }
        prompt.append("，当前股价").append(securityPrice)
                .append("，近一周价格波动").append(stockIndicator.getWeekPriceRange())
                .append("，近一周价格波动").append(stockIndicator.getWeekPriceRange())
                .append("，近一月价格波动").append(stockIndicator.getMonthPriceRange());

        Map<String, List<StockIndicatorItem>> lineMap = stockIndicator.getIndicatorMap();
        int weekSize = 10;
        for (Map.Entry<String, List<StockIndicatorItem>> entry : lineMap.entrySet()) {
            String key = entry.getKey();
            List<StockIndicatorItem> value = entry.getValue();
            prompt.append("，当前").append(key).append("为").append(value.get(0).getValue())
                    .append("（最近").append(weekSize).append("周的周线").append(key).append("分别是：");

            int size = Math.min(value.size(), weekSize);
            List<StockIndicatorItem> subList = value.subList(0, size);

            subList.forEach(item -> {
                prompt.append(item.getDate()).append("这周的").append(key).append("为").append(item.getValue()).append("，");
            });
        }

        prompt.append("），当前期权距离到期时间").append(optionsStrikeDate.getOptionExpiryDateDistance())
                .append("天，我当前计划交易的期权实时信息如下：\n");
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
        prompt.append("请帮我分析当前股票指标和这些期权标的，帮我检查是否适合交易，如何适合交易请给我综合最优的交易建议和保守的交易建议。");
        optionsChain.setPrompt(prompt.toString());
    }

    private int tradeLevel(VixIndicator vixIndicator, StockIndicator stockIndicator) {
        // 1. 市场安全评估,检查VIX恐慌指数. VIX＞30：停止操作（市场波动剧烈）
        if (null != vixIndicator && null != vixIndicator.getCurrentVix()) {
            BigDecimal vixVal = vixIndicator.getCurrentVix().getValue();
            if (vixVal.compareTo(BigDecimal.valueOf(30)) > 0) {
                log.warn("当前VIX指数为{}，不建议交易", vixVal);
                return 0;
            }
        }

        // 2. 个股交易安全性,检查期权到期前是否有财报发布
        // 暂且人工确认

        // 3. 技术指标分析（基于周线图）
        // 	  ➠ 检查相对强弱指数RSI
        //	    • RSI＜30（超卖区域）：进行MACD验证
        //      • RSI≥30：直接进入第四项检查
        //	  ➠ MACD指标验证（参数12,26,9）
        //	    • 动量向上：表明股价可能从支撑位反弹，进入最终检查
        //	    • 动量向下：规避该股（存在强烈下跌趋势）
        if (null != stockIndicator) {
            // 检查当前相对强弱指数RSI
            List<StockIndicatorItem> rsiList = stockIndicator.getIndicatorMap().get(IndicatorKey.RSI);
            if (null != rsiList && !rsiList.isEmpty()) {
                BigDecimal rsiVal = rsiList.get(0).getValue();
                // RSI＜30（超卖区域）：进行MACD验证
                if (rsiVal.compareTo(BigDecimal.valueOf(30)) < 0) {
                    List<StockIndicatorItem> macdList = stockIndicator.getIndicatorMap().get(IndicatorKey.MACD);
                    if (null != macdList && macdList.size() > 1) {
                        // 最近一周小于上一周 则不建议交易
                        if (macdList.get(0).getValue().compareTo(macdList.get(1).getValue()) < 0) {
                            log.warn("当前RSI为{}，近两周MACD为{}，{}，不建议交易", rsiVal, macdList.get(0).getValue(), macdList.get(1).getValue());
                            return 0;
                        }
                    }
                }

                // 【加项】RSI>70也不推荐
                if (rsiVal.compareTo(BigDecimal.valueOf(70)) > 0) {
                    log.warn("当前RSI为{}，超买状态不建议交易", rsiVal);
                    return 0;
                }
            }
        }

        // 4. 关键支撑位判定  观察1年周期周线图
        // 暂且人工确认 Bollinger Bands (Length: 20; Deviations: 2)

        return 1;
    }

    private void buildOptionsPrompt(StringBuilder prompt, Options options) {
        if (Boolean.FALSE.equals(options.getStrategyData().getRecommend())) {
            return;
        }
        prompt.append("标的:").append(options.getBasic().getSecurity().getCode())
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
