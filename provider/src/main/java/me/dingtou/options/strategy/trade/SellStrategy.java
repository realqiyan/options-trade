package me.dingtou.options.strategy.trade;

import me.dingtou.options.constant.CandlestickPeriod;
import me.dingtou.options.model.*;
import me.dingtou.options.util.IndicatorDataFrameUtil;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Component
public class SellStrategy extends BaseTradeStrategy {

    @Override
    void process(OwnerAccount account,
            OptionsChain optionsChain,
            StrategySummary summary) {
        String prompt = buildPrompt(account, optionsChain, summary);
        optionsChain.setPrompt(prompt);
    }

    private String buildPrompt(OwnerAccount account,
            OptionsChain optionsChain,
            StrategySummary summary) {

        // 反转delta和theta
        optionsChain.getOptionsList().forEach(options -> {
            OptionsRealtimeData realtimeData = options.getRealtimeData();
            if (null != realtimeData) {
                realtimeData.setDelta(realtimeData.getDelta().multiply(BigDecimal.valueOf(-1)));
                realtimeData.setTheta(realtimeData.getTheta().multiply(BigDecimal.valueOf(-1)));
            } else {
                options.setRealtimeData(new OptionsRealtimeData());
            }
        });

        // AI分析提示词
        VixIndicator vixIndicator = optionsChain.getVixIndicator();
        StockIndicator stockIndicator = optionsChain.getStockIndicator();
        SecurityQuote securityQuote = stockIndicator.getSecurityQuote();

        CandlestickPeriod period = stockIndicator.getPeriod();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        BigDecimal securityPrice = securityQuote.getLastDone();
        StringBuilder prompt = new StringBuilder();
        prompt.append("我准备卖出").append(securityQuote.getSecurity().toString())
                .append("距离到期日").append(optionsChain.dte()).append("天的期权");
        if (null != summary) {
            prompt.append("，策略ID:").append(summary.getStrategy().getStrategyId());
        }
        prompt.append("当前股票价格是").append(securityPrice)
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
        optionsChain.getOptionsList().forEach(options -> {
            buildOptionsPrompt(prompt, options);
        });

        return prompt.toString();
    }

    private void buildOptionsPrompt(StringBuilder prompt, Options options) {
        if (Boolean.FALSE.equals(options.getStrategyData().getRecommend())) {
            return;
        }
        prompt.append("| ").append(options.getBasic().getSecurity().getCode())
                .append(" | ").append(Integer.valueOf(1).equals(options.getOptionExData().getType()) ? "Call" : "Put")
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

    @Override
    public boolean isSupport(OwnerStrategy strategy) {
        return null == strategy || "default".equalsIgnoreCase(strategy.getStrategyCode());
    }
}
