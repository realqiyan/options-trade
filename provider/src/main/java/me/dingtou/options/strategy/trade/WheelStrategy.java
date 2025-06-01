package me.dingtou.options.strategy.trade;

import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.constant.IndicatorKey;
import me.dingtou.options.model.*;
import me.dingtou.options.util.TemplateRenderer;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.HashMap;
import java.util.Map;

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

        // 准备模板数据
        Map<String, Object> data = new HashMap<>();
        data.put("securityQuote", optionsChain.getStockIndicator().getSecurityQuote());
        data.put("optionsChain", optionsChain);
        data.put("summary", summary);
        data.put("securityPrice", optionsChain.getStockIndicator().getSecurityQuote().getLastDone());
        data.put("vixIndicator", optionsChain.getVixIndicator());
        data.put("currentDate", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        
        // 添加WheelStrategy特有的变量
        Integer holdStockNum = summary.getHoldStockNum();
        // 使用局部变量避免类型转换问题
        boolean isSellPutStage = (null == holdStockNum || holdStockNum == 0);
        boolean isCoveredCallStage = !isSellPutStage;
        data.put("isSellPutStage", isSellPutStage);
        data.put("isCoveredCallStage", isCoveredCallStage);
        
        // 如果是cc阶段，需要找到最近的指派订单
        OwnerOrder currentUnderlyingOrder = null;
        if (isCoveredCallStage) {
            Optional<OwnerOrder> optionalOwnerOrder = summary.getStrategyOrders().stream()
                    .filter(order -> order.getCode().equals(order.getUnderlyingCode())).findFirst();
            if (optionalOwnerOrder.isPresent()) {
                currentUnderlyingOrder = optionalOwnerOrder.get();
            }
        }
        data.put("finalUnderlyingOrder", currentUnderlyingOrder);

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
        data.put("finalSellPutAcceptableStrikePrice", sellPutAcceptableStrikePrice);
        
        String promptStr = TemplateRenderer.render("trade_wheel_strategy.ftl", data);
        return new StringBuilder(promptStr);
    }

}
