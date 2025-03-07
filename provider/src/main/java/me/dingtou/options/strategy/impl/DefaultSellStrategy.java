package me.dingtou.options.strategy.impl;

import me.dingtou.options.constant.IndicatorKey;
import me.dingtou.options.model.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
public class DefaultSellStrategy extends BaseStrategy {


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
        VixIndicator vixIndicator = optionsChain.getVixIndicator();
        StockIndicator stockIndicator = optionsChain.getStockIndicator();
        SecurityQuote securityQuote = stockIndicator.getSecurityQuote();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        BigDecimal securityPrice = securityQuote.getLastDone();
        StringBuilder prompt = new StringBuilder();
        prompt.append("我准备卖单腿期权收取权利金，交易").append(securityQuote.getSecurity().toString()).append("距离到期日").append(optionsStrikeDate.getOptionExpiryDateDistance()).append("天的期权。\n");
        prompt.append("当前日期是").append(sdf.format(new Date())).append("，接下来我将使用markdown格式给你提供一些信息，你需要根据信息给我交易建议。\n\n");

        // 最近几周的周K线
        prompt.append("## 原始周K线\n");
        List<Candlestick> weekCandlesticks = stockIndicator.getWeekCandlesticks();
        int subListSize = Math.min(weekCandlesticks.size(), 30);
        weekCandlesticks = weekCandlesticks.subList(weekCandlesticks.size() - subListSize, weekCandlesticks.size());

        prompt.append("| 日期 ").append("| 开盘价 ").append("| 收盘价 ").append("| 最高价 ").append("| 最低价 ").append("| 成交量 ").append("| 成交额 ").append("|\n");
        prompt.append("| --- ").append("| --- ").append("| --- ").append("| --- ").append("| --- ").append("| --- ").append("| --- ").append("|\n");

        weekCandlesticks.forEach(candlestick -> {
            Long timestamp = candlestick.getTimestamp();
            prompt.append("|").append(sdf.format(new Date(timestamp * 1000)))
                    .append("|").append(candlestick.getOpen())
                    .append("|").append(candlestick.getClose())
                    .append("|").append(candlestick.getHigh())
                    .append("|").append(candlestick.getLow())
                    .append("|").append(candlestick.getVolume())
                    .append("|").append(candlestick.getTurnover())
                    .append("|\n");
        });
        prompt.append("\n");

        prompt.append("## 技术指标汇总（周K线）\n");
        prompt.append("* 当前价格：").append(securityPrice).append("\n");
        if (null != vixIndicator && null != vixIndicator.getCurrentVix()) {
            prompt.append("* VIX指数：").append(vixIndicator.getCurrentVix().getValue()).append("\n");
        }
        Map<String, List<StockIndicatorItem>> lineMap = stockIndicator.getIndicatorMap();
        for (Map.Entry<String, List<StockIndicatorItem>> entry : lineMap.entrySet()) {
            IndicatorKey indicatorKey = IndicatorKey.of(entry.getKey());
            List<StockIndicatorItem> value = entry.getValue();
            prompt.append("* ").append(indicatorKey.getDisplayName()).append(":").append(value.get(0).getValue()).append("\n");
        }
        prompt.append("\n");

        int weekSize = 10;
        prompt.append("### 近").append(weekSize).append("周技术指标\n");
        for (Map.Entry<String, List<StockIndicatorItem>> entry : lineMap.entrySet()) {
            IndicatorKey indicatorKey = IndicatorKey.of(entry.getKey());
            List<StockIndicatorItem> value = entry.getValue();
            prompt.append("#### ").append(indicatorKey.getDisplayName()).append("\n");
            int size = Math.min(value.size(), weekSize);
            List<StockIndicatorItem> subList = value.subList(0, size);
            prompt.append("| 日期 ").append("| 指标值 ").append("|\n");
            prompt.append("| --- ").append("| --- ").append("|\n");
            subList.forEach(item -> {
                prompt.append("|").append(item.getDate())
                        .append("|").append(item.getValue())
                        .append("|\n");
            });
            prompt.append("\n");
        }

        prompt.append("\n");
        prompt.append("## 交易标的\n");
        prompt.append("| 代码 ").append("| 期权交易类型 ").append("| 行权价 ").append("| 当前价格 ").append("| 隐含波动率 ").append("| Delta ").append("| Theta ").append("| Gamma ").append("| 未平仓合约数 ").append("| 当天交易量 ").append("| 预估年化收益率 ").append("| 距离行权价涨跌幅 ").append("| 购买倾向 ").append("|\n");
        prompt.append("| --- ").append("| --- ").append("| --- ").append("| --- ").append("| --- ").append("| --- ").append("| --- ").append("| --- ").append("| --- ").append("| --- ").append("| --- ").append("| --- ").append("| --- ").append("|\n");
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
        prompt.append("## 要求\n");
        prompt.append("* 1.根据提供的原始周K线信息、以及技术指标分析总结当前股票走势方向和风险程度。\n");
        prompt.append("* 2.根据总结信息分析当前股票是否适合进行期权交易。\n");
        prompt.append("* 3.结合以上分析结论，列出综合最优和保守的单腿期权交易策略建议。\n");
        return prompt.toString();
    }

    private void buildOptionsPrompt(StringBuilder prompt, Options options) {
        if (Boolean.FALSE.equals(options.getStrategyData().getRecommend())) {
            return;
        }
        prompt.append("|").append(options.getBasic().getSecurity().getCode())
                .append("|").append(Integer.valueOf(1).equals(options.getOptionExData().getType()) ? "SellCall" : "SellPut")
                .append("|").append(options.getOptionExData().getStrikePrice())
                .append("|").append(options.getRealtimeData().getCurPrice())
                .append("|").append(options.getRealtimeData().getImpliedVolatility())
                .append("|").append(options.getRealtimeData().getDelta())
                .append("|").append(options.getRealtimeData().getTheta())
                .append("|").append(options.getRealtimeData().getGamma())
                .append("|").append(options.getRealtimeData().getOpenInterest())
                .append("|").append(options.getRealtimeData().getVolume())
                .append("|").append(options.getStrategyData().getSellAnnualYield()).append("%")
                .append("|").append(options.getStrategyData().getRange()).append("%")
                .append("|").append(options.getStrategyData().getRecommendLevel() <= 2 ? "一般" : "较强")
                .append("|\n");
    }

    @Override
    public boolean isSupport(OwnerStrategy strategy) {
        return null == strategy || "default".equalsIgnoreCase(strategy.getStrategyCode());
    }
}
