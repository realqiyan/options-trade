package me.dingtou.options.strategy.trade;

import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.constant.IndicatorKey;
import me.dingtou.options.model.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Component
public class WheelStrategy extends BaseTradeStrategy {

    @Override
    public boolean isSupport(OwnerStrategy strategy) {
        return null != strategy && "wheel_strategy".equals(strategy.getStrategyCode());
    }

    @Override
    void processData(OwnerAccount account,
            OptionsChain optionsChain,
            StrategySummary summary) {

        if (null == summary) {
            return;
        }

        Integer holdStockNum = summary.getHoldStockNum();
        // 空仓执行sell put 否则执行cc
        boolean isSellPutStage = null == holdStockNum || holdStockNum == 0;
        boolean isCoveredCallStage = !isSellPutStage;

        optionsChain.getOptionsList().forEach(options -> {
            OptionsRealtimeData realtimeData = options.getRealtimeData();
            if (null != realtimeData) {
                realtimeData.setDelta(realtimeData.getDelta().multiply(BigDecimal.valueOf(-1)));
                realtimeData.setTheta(realtimeData.getTheta().multiply(BigDecimal.valueOf(-1)));
            } else {
                options.setRealtimeData(new OptionsRealtimeData());
            }

            // 根据策略阶段过滤不需要的期权类型:
            // 1. 如果是卖出看跌期权(Sell Put)阶段,过滤掉看涨期权(Call,type=1)
            // 2. 如果是备兑看涨期权(Covered Call)阶段,过滤掉看跌期权(Put,type=2)
            // 通过将推荐标志设为false和推荐等级设为0来实现过滤
            if (isSellPutStage && options.getOptionExData().getType() == 1) {
                options.getStrategyData().setRecommend(false);
                options.getStrategyData().setRecommendLevel(0);
            } else if (isCoveredCallStage && options.getOptionExData().getType() == 2) {
                options.getStrategyData().setRecommend(false);
                options.getStrategyData().setRecommendLevel(0);
            }
        });

        VixIndicator vixIndicator = optionsChain.getVixIndicator();
        StockIndicator stockIndicator = optionsChain.getStockIndicator();

        int tradeLevel = tradeLevel(vixIndicator, stockIndicator);
        optionsChain.setTradeLevel(tradeLevel);
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
        // ➠ 检查相对强弱指数RSI
        // • RSI＜30（超卖区域）：进行MACD验证
        // • RSI≥30：直接进入第四项检查
        // ➠ MACD指标验证（参数12,26,9）
        // • 动量向上：表明股价可能从支撑位反弹，进入最终检查
        // • 动量向下：规避该股（存在强烈下跌趋势）
        if (null != stockIndicator) {
            // 检查当前相对强弱指数RSI
            List<StockIndicatorItem> rsiList = stockIndicator.getIndicatorMap().get(IndicatorKey.RSI.getKey());
            if (null != rsiList && !rsiList.isEmpty()) {
                BigDecimal rsiVal = rsiList.get(0).getValue();
                // RSI＜30（超卖区域）：进行MACD验证
                if (rsiVal.compareTo(BigDecimal.valueOf(30)) < 0) {
                    List<StockIndicatorItem> macdList = stockIndicator.getIndicatorMap()
                            .get(IndicatorKey.MACD.getKey());
                    if (null != macdList && macdList.size() > 1) {
                        // 最近一周小于上一周 则不建议交易
                        if (macdList.get(0).getValue().compareTo(macdList.get(1).getValue()) < 0) {
                            log.warn("当前RSI为{}，近两{}MACD为{}，{}，不建议交易",
                                    rsiVal,
                                    stockIndicator.getPeriod().getName(),
                                    macdList.get(0).getValue(),
                                    macdList.get(1).getValue());
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

        // 4. 关键支撑位判定 观察1年周期周线图
        // 暂且人工确认 Bollinger Bands (Length: 20; Deviations: 2)

        return 1;
    }

    @Override
    StringBuilder processPrompt(OwnerAccount account, OptionsChain optionsChain, StrategySummary summary) {

        // 当前策略信息
        if (null == summary) {
            log.warn("策略信息为空，请检查！");
            return null;
        }

        Integer holdStockNum = summary.getHoldStockNum();
        // 空仓执行sell put 否则执行cc
        boolean isSellPutStage = null == holdStockNum || holdStockNum == 0;
        boolean isCoveredCallStage = !isSellPutStage;

        // 如果是cc阶段，需要找到最近的指派订单
        OwnerOrder currentUnderlyingOrder = null;
        if (isCoveredCallStage) {
            Optional<OwnerOrder> optionalOwnerOrder = summary.getStrategyOrders().stream()
                    .filter(order -> order.getCode().equals(order.getUnderlyingCode())).findFirst();
            if (optionalOwnerOrder.isPresent()) {
                currentUnderlyingOrder = optionalOwnerOrder.get();
            }
        }
        final OwnerOrder finalUnderlyingOrder = currentUnderlyingOrder;

        // 获取sellput可接受的行权价配置
        BigDecimal sellPutAcceptableStrikePrice = null;
        if (isSellPutStage && summary.getStrategy() != null) {
            String sellPutStrikePriceStr = summary.getStrategy()
                    .getExtValue(StrategyExt.WHEEL_SELLPUT_STRIKE_PRICE, null);
            if (StringUtils.isNotBlank(sellPutStrikePriceStr)) {
                try {
                    sellPutAcceptableStrikePrice = new BigDecimal(sellPutStrikePriceStr);
                    log.info("车轮策略Sell Put可接受的行权价配置: {}", sellPutAcceptableStrikePrice);
                } catch (Exception e) {
                    log.warn("解析车轮策略Sell Put可接受的行权价配置失败: {}", sellPutStrikePriceStr, e);
                }
            }
        }
        final BigDecimal finalSellPutAcceptableStrikePrice = sellPutAcceptableStrikePrice;

        VixIndicator vixIndicator = optionsChain.getVixIndicator();
        StockIndicator stockIndicator = optionsChain.getStockIndicator();

        // AI分析提示词
        SecurityQuote securityQuote = stockIndicator.getSecurityQuote();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        BigDecimal securityPrice = securityQuote.getLastDone();
        StringBuilder prompt = new StringBuilder();
        prompt.append("当前日期是").append(sdf.format(new Date()))
                .append("，我准备使用车轮策略（WheelStrategy）卖出").append(securityQuote.getSecurity().toString())
                .append("距离到期日").append(optionsChain.dte()).append("天的")
                .append(isSellPutStage ? "看跌期权（Cash-Secured Put）" : "看涨期权（Covered Call）");
        // prompt.append("，策略ID:").append(summary.getStrategy().getStrategyId());
        prompt.append("，倾向的期权Delta范围:0.25-0.35");
        if (isCoveredCallStage && null != finalUnderlyingOrder) {
            prompt.append("，当前指派的股票价格：").append(finalUnderlyingOrder.getPrice());
        }
        if (isSellPutStage && finalSellPutAcceptableStrikePrice != null) {
            prompt.append("，SellPut可接受最高指派价").append(finalSellPutAcceptableStrikePrice)
                    .append("，行权价高于").append(finalSellPutAcceptableStrikePrice)
                    .append("的Put如果风险可控也接受卖出，但是快被指派前需要Rolling");
            prompt.append("，年化收益率计算公式:(期权权利金*100)/(DTE)*365/股票价格*100");
        }
        if (null != summary.getHoldStockNum()) {
            prompt.append("，当前持有股票数量：").append(summary.getHoldStockNum());
            // 展示持有股票成本价
            if (summary.getHoldStockCost() != null && summary.getHoldStockCost().compareTo(BigDecimal.ZERO) > 0) {
                prompt.append("，持有股票成本价：").append(summary.getHoldStockCost());
            }
        }
        prompt.append("，当前股票价格是").append(securityPrice)
                .append(null != vixIndicator && null != vixIndicator.getCurrentVix()
                        ? "，当前VIX指数是" + vixIndicator.getCurrentVix().getValue()
                        : "")
                .append("，接下来我将使用markdown格式给你提供一些信息，你需要根据信息给我交易建议。\n\n");

        return prompt;
    }

}
