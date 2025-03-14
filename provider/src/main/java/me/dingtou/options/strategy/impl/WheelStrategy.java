package me.dingtou.options.strategy.impl;

import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.constant.CandlestickPeriod;
import me.dingtou.options.constant.IndicatorKey;
import me.dingtou.options.model.*;
import me.dingtou.options.util.AccountExtUtils;
import me.dingtou.options.util.IndicatorDataFrameUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Component
public class WheelStrategy extends BaseStrategy {

    @Override
    public boolean isSupport(OwnerStrategy strategy) {
        return null != strategy && "wheel_strategy".equals(strategy.getStrategyCode());
    }

    @Override
    public void process(OwnerAccount account,
            OptionsStrikeDate optionsStrikeDate,
            OptionsChain optionsChain,
            StrategySummary strategySummary) {
        // 当前策略信息
        if (null == strategySummary) {
            log.warn("策略信息为空，请检查！");
            return;
        }

        CandlestickPeriod period = AccountExtUtils.getKlinePeriod(account);

        Integer holdStockNum = strategySummary.getHoldStockNum();
        // 空仓执行sell put 否则执行cc
        boolean isSellPutStage = null == holdStockNum || holdStockNum == 0;
        boolean isCoveredCallStage = !isSellPutStage;

        // 如果是cc阶段，需要找到最近的指派订单
        OwnerOrder currentUnderlyingOrder = null;
        if (isCoveredCallStage) {
            Optional<OwnerOrder> optionalOwnerOrder = strategySummary.getStrategyOrders().stream()
                    .filter(order -> order.getCode().equals(order.getUnderlyingCode())).findFirst();
            if (optionalOwnerOrder.isPresent()) {
                currentUnderlyingOrder = optionalOwnerOrder.get();
            }
        }
        final OwnerOrder finalUnderlyingOrder = currentUnderlyingOrder;

        // 获取sellput可接受的行权价配置
        BigDecimal sellPutAcceptableStrikePrice = null;
        if (isSellPutStage && strategySummary.getStrategy() != null) {
            String sellPutStrikePriceStr = strategySummary.getStrategy()
                    .getExtValue(StrategyExt.WHEEL_SELLPUT_STRIKE_PRICE);
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
                    if (null != finalUnderlyingOrder
                            && call.getOptionExData().getStrikePrice().compareTo(finalUnderlyingOrder.getPrice()) < 0) {
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

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        BigDecimal securityPrice = securityQuote.getLastDone();
        StringBuilder prompt = new StringBuilder();
        prompt.append("我准备使用车轮策略（WheelStrategy）卖出").append(securityQuote.getSecurity().toString())
                .append("距离到期日").append(optionsStrikeDate.getOptionExpiryDateDistance()).append("天的")
                .append(isSellPutStage ? "看跌期权（Cash-Secured Put）" : "看涨期权（Covered Call）");
        if (isCoveredCallStage && null != finalUnderlyingOrder) {
            prompt.append("，当前指派的股票价格：").append(finalUnderlyingOrder.getPrice());
        }
        if (isSellPutStage && finalSellPutAcceptableStrikePrice != null) {
            prompt.append("，Sell Put可接受的最高行权价：").append(finalSellPutAcceptableStrikePrice).append("（可以接受Rolling）");
        }
        prompt.append("，当前股票价格是").append(securityPrice)
                .append(null != vixIndicator && null != vixIndicator.getCurrentVix()
                        ? "，当前VIX指数是" + vixIndicator.getCurrentVix().getValue()
                        : "")
                .append("，当前日期是").append(sdf.format(new Date()))
                .append("，接下来我将使用markdown格式给你提供一些信息，你需要根据信息给我交易建议。\n\n");

        // 最近K线
        List<Candlestick> candlesticks = stockIndicator.getCandlesticks();
        if (null != candlesticks && !candlesticks.isEmpty()) {
            prompt.append("## 原始").append(period.getName()).append("K线\n");
            int subListSize = Math.min(candlesticks.size(), 30);
            candlesticks = candlesticks.subList(candlesticks.size() - subListSize, candlesticks.size());

            prompt.append("| 日期 ").append("| 开盘价 ").append("| 收盘价 ").append("| 最高价 ").append("| 最低价 ").append("| 成交量 ")
                    .append("| 成交额 ").append("|\n");
            prompt.append("| --- ").append("| --- ").append("| --- ").append("| --- ").append("| --- ").append("| --- ")
                    .append("| --- ").append("|\n");

            candlesticks.forEach(candlestick -> {
                Long timestamp = candlestick.getTimestamp();
                prompt.append("| ").append(sdf.format(new Date(timestamp * 1000)))
                        .append(" | ").append(candlestick.getOpen())
                        .append(" | ").append(candlestick.getClose())
                        .append(" | ").append(candlestick.getHigh())
                        .append(" | ").append(candlestick.getLow())
                        .append(" | ").append(candlestick.getVolume())
                        .append(" | ").append(candlestick.getTurnover())
                        .append(" |\n");
            });
            prompt.append("\n");
        }

        int dataSize = 20;
        prompt.append("### 近").append(dataSize).append(period.getName()).append("技术指标\n");

        // 使用IndicatorDataFrameUtil生成技术指标表格
        prompt.append(IndicatorDataFrameUtil.createMarkdownTable(stockIndicator, dataSize));

        prompt.append("\n");
        prompt.append("## 交易标的\n");
        prompt.append("| 代码 ").append("| 期权类型 ").append("| 行权价 ").append("| 当前价格 ").append("| 隐含波动率 ")
                .append("| Delta ").append("| Theta ").append("| Gamma ").append("| 未平仓合约数 ").append("| 当天交易量 ")
                .append("| 预估年化收益率 ").append("| 距离行权价涨跌幅 ").append("| 购买倾向 ").append("|\n");
        prompt.append("| --- ").append("| --- ").append("| --- ").append("| --- ").append("| --- ").append("| --- ")
                .append("| --- ").append("| --- ").append("| --- ").append("| --- ").append("| --- ").append("| --- ")
                .append("| --- ").append("|\n");
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
        prompt.append("\n");
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

    private void buildOptionsPrompt(StringBuilder prompt, Options options) {
        if (Boolean.FALSE.equals(options.getStrategyData().getRecommend())) {
            return;
        }
        prompt.append("| ").append(options.getBasic().getSecurity().getCode())
                .append(" | ")
                .append(Integer.valueOf(1).equals(options.getOptionExData().getType()) ? "Call" : "Put")
                .append(" | ").append(options.getOptionExData().getStrikePrice())
                .append(" | ").append(options.getRealtimeData().getCurPrice())
                .append(" | ").append(options.getRealtimeData().getImpliedVolatility())
                .append(" | ").append(options.getRealtimeData().getDelta())
                .append(" | ").append(options.getRealtimeData().getTheta())
                .append(" | ").append(options.getRealtimeData().getGamma())
                .append(" | ").append(options.getRealtimeData().getOpenInterest())
                .append(" | ").append(options.getRealtimeData().getVolume())
                .append(" | ").append(options.getStrategyData().getSellAnnualYield()).append("%")
                .append(" | ").append(options.getStrategyData().getRange()).append("%")
                .append(" | ").append(options.getStrategyData().getRecommendLevel() <= 1 ? "一般" : "倾向")
                .append(" |\n");
    }
}
