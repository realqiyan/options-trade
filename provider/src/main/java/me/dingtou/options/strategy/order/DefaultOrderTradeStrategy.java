package me.dingtou.options.strategy.order;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.alibaba.fastjson2.JSON;

import me.dingtou.options.constant.CandlestickPeriod;
import me.dingtou.options.constant.OrderExt;
import me.dingtou.options.constant.TradeSide;
import me.dingtou.options.model.Candlestick;
import me.dingtou.options.model.OptionsRealtimeData;
import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.model.OwnerOrder;
import me.dingtou.options.model.OwnerStrategy;
import me.dingtou.options.model.StockIndicator;
import me.dingtou.options.strategy.OrderTradeStrategy;
import me.dingtou.options.util.AccountExtUtils;
import me.dingtou.options.util.IndicatorDataFrameUtil;

/**
 * 默认订单交易策略
 * 
 * @author qiyan
 */
public class DefaultOrderTradeStrategy implements OrderTradeStrategy {

    @Override
    public boolean isSupport(OwnerStrategy strategy) {
        return true;
    }

    @Override
    public void calculate(OwnerAccount account, OwnerOrder order, StockIndicator stockIndicator) {
        if (OwnerOrder.isStockOrder(order) || OwnerOrder.isClose(order) || !OwnerOrder.isTraded(order)) {
            return;
        }

        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        StringBuilder prompt = new StringBuilder();
        String sideName = TradeSide.of(order.getSide()).getName();
        prompt.append("我在").append(dateTimeFormat.format(order.getTradeTime())).append(sideName).append(order.getCode())
                .append("，行权日期为").append(dateFormat.format(order.getStrikeTime()))
                .append("，行权价为").append(OwnerOrder.strikePrice(order))
                .append("，").append(sideName).append("价格为").append(order.getPrice())
                .append("，").append(sideName).append("数量为").append(order.getQuantity())
                .append("，当前价格为").append(order.getExtValue(OrderExt.CUR_PRICE))
                .append("，当前股票价格为").append(stockIndicator.getSecurityQuote().getLastDone())
                .append("，当前日期是").append(dateFormat.format(new Date()))
                .append("，接下来我将使用markdown格式给你提供一些信息，你需要根据信息给我当前这笔订单的交易建议。\n\n");

        CandlestickPeriod period = AccountExtUtils.getKlinePeriod(account);

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
                prompt.append("| ").append(dateFormat.format(new Date(timestamp * 1000)))
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

        String extValue = order.getExtValue(OrderExt.ROLL_OPTIONS);

        if (null != extValue) {
            List<OptionsRealtimeData> optionsRealtimeDataList = JSON.parseArray(extValue, OptionsRealtimeData.class);

            if (null != optionsRealtimeDataList && !optionsRealtimeDataList.isEmpty()) {
                prompt.append("\n");
                prompt.append("## Roll备选期权列表\n");
                prompt.append(
                        "| 期权代码 | 期权类型 | 行权价 | 当前价格 | Delta | Gamma | Theta | Vega | 隐含波动率 | 溢价 | 未平仓数量 | 成交量 |\n");
                prompt.append("| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |\n");

                for (OptionsRealtimeData data : optionsRealtimeDataList) {
                    if (data == null || data.getSecurity() == null) {
                        continue;
                    }

                    // 获取期权类型（1为看涨，2为看跌）
                    String optionType = "未知";
                    if (data.getSecurity().getCode().contains("C")) {
                        optionType = "Call";
                    } else if (data.getSecurity().getCode().contains("P")) {
                        optionType = "Put";
                    }

                    // 从期权代码中提取行权价
                    String strikePrice = "未知";
                    String code = data.getSecurity().getCode();
                    int strikeIndex = code.lastIndexOf("C") > code.lastIndexOf("P") ? code.lastIndexOf("C") + 1
                            : code.lastIndexOf("P") + 1;
                    if (strikeIndex > 0 && strikeIndex < code.length()) {
                        strikePrice = code.substring(strikeIndex);
                        strikePrice = new BigDecimal(strikePrice).divide(new BigDecimal(1000)).toPlainString();
                    }

                    prompt.append("| ")
                            .append(data.getSecurity().getCode())
                            .append(" | ")
                            .append(optionType)
                            .append(" | ")
                            .append(strikePrice)
                            .append(" | ")
                            .append(data.getCurPrice() != null ? data.getCurPrice() : "-")
                            .append(" | ")
                            .append(data.getDelta() != null ? data.getDelta() : "-")
                            .append(" | ")
                            .append(data.getGamma() != null ? data.getGamma() : "-")
                            .append(" | ")
                            .append(data.getTheta() != null ? data.getTheta() : "-")
                            .append(" | ")
                            .append(data.getVega() != null ? data.getVega() : "-")
                            .append(" | ")
                            .append(data.getImpliedVolatility() != null ? data.getImpliedVolatility() + "%" : "-")
                            .append(" | ")
                            .append(data.getPremium() != null ? data.getPremium() + "%" : "-")
                            .append(" | ")
                            .append(data.getOpenInterest() != null ? data.getOpenInterest() : "-")
                            .append(" | ")
                            .append(data.getVolume() != null ? data.getVolume() : "-")
                            .append(" |\n");
                }
                prompt.append("\n");
            }
        }

        order.setExtValue(OrderExt.PROMPT, prompt.toString());
    }

}
