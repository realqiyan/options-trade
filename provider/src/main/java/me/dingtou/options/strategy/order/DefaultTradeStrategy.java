package me.dingtou.options.strategy.order;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson2.JSON;
import com.google.common.collect.Lists;

import me.dingtou.options.constant.CandlestickPeriod;
import me.dingtou.options.constant.OptionsStrategy;
import me.dingtou.options.constant.OrderExt;
import me.dingtou.options.constant.TradeSide;
import me.dingtou.options.model.Candlestick;
import me.dingtou.options.model.IndicatorDataFrame;
import me.dingtou.options.model.OptionsRealtimeData;
import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.model.OwnerOrder;
import me.dingtou.options.model.OwnerStrategy;
import me.dingtou.options.model.StockIndicator;
import me.dingtou.options.model.StrategySummary;
import me.dingtou.options.strategy.OrderTradeStrategy;
import me.dingtou.options.util.AccountExtUtils;
import me.dingtou.options.util.IndicatorDataFrameUtil;
import me.dingtou.options.util.TemplateRenderer;

/**
 * 默认订单交易策略
 * 
 * @author qiyan
 */
public class DefaultTradeStrategy implements OrderTradeStrategy {

    private StrategySummary summary;

    public DefaultTradeStrategy(StrategySummary summary) {
        this.summary = summary;
    }

    @Override
    public boolean isSupport(OwnerStrategy strategy) {
        return true;
    }

    @Override
    public void calculate(OwnerAccount account, OwnerOrder order, StockIndicator stockIndicator) {
        if (OwnerOrder.isStockOrder(order) || OwnerOrder.isClose(order) || !OwnerOrder.isTraded(order)) {
            return;
        }

        StringBuilder prompt = new StringBuilder();

        // 策略说明
        String strategyTemplate = String.format("strategy_%s.ftl", summary.getStrategy().getStrategyCode());
        String strategyPrompt = TemplateRenderer.render(strategyTemplate, new HashMap<>());
        prompt.append(strategyPrompt).append("\n");

        CandlestickPeriod period = AccountExtUtils.getKlinePeriod(account);

        // 最近K线
        List<Candlestick> candlesticks = stockIndicator.getCandlesticks();
        if (null != candlesticks && !candlesticks.isEmpty()) {
            int subListSize = Math.min(candlesticks.size(), 30);
            List<Candlestick> recentCandlesticks = candlesticks.subList(candlesticks.size() - subListSize,
                    candlesticks.size());

            Map<String, Object> data = new HashMap<>();
            data.put("candlesticks", Lists.reverse(new ArrayList<>(recentCandlesticks)));
            data.put("period", subListSize);
            data.put("periodName", period.getName());
            data.put("securityQuote", stockIndicator.getSecurityQuote());

            String table = TemplateRenderer.render("data_candlesticks.ftl", data);
            prompt.append(table).append("\n");
        }

        int dataSize = 20;
        // 使用模板渲染技术指标表格
        IndicatorDataFrame dataFrame = IndicatorDataFrameUtil.createDataFrame(stockIndicator, dataSize);
        Map<String, Object> indicatorsData = new HashMap<>();
        indicatorsData.put("dataFrame", dataFrame);
        indicatorsData.put("period", dataSize);
        indicatorsData.put("periodName", period.getName());
        indicatorsData.put("securityQuote", stockIndicator.getSecurityQuote());

        String indicatorsTable = TemplateRenderer.render("data_indicators.ftl", indicatorsData);
        prompt.append(indicatorsTable).append("\n");

        String extValue = order.getExtValue(OrderExt.ROLL_OPTIONS);
        if (null != extValue) {
            List<OptionsRealtimeData> optionsRealtimeDataList = JSON.parseArray(extValue, OptionsRealtimeData.class);

            if (null != optionsRealtimeDataList && !optionsRealtimeDataList.isEmpty()) {
                Map<String, Object> data = new HashMap<>();
                data.put("optionsRealtimeDataList", optionsRealtimeDataList);
                String table = TemplateRenderer.render("data_roll_options.ftl", data);
                prompt.append(table).append("\n");
            }
        }

        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        String sideName = TradeSide.of(order.getSide()).getName();

        OptionsStrategy strategy = OptionsStrategy.of(summary.getStrategy().getStrategyCode());
        prompt.append("当前使用的策略是").append(strategy.getName())
                .append("，策略持股：").append(summary.getHoldStockNum())
                .append("，平均持股成本：").append(summary.getAverageStockCost())
                .append("，策略Delta：").append(summary.getStrategyDelta())
                .append("。\n");

        prompt.append("我在").append(dateTimeFormat.format(order.getTradeTime())).append(sideName).append(order.getCode())
                .append("，行权日期为").append(dateFormat.format(order.getStrikeTime()))
                .append("，行权价为").append(OwnerOrder.strikePrice(order))
                .append("，").append(sideName).append("价格为").append(order.getPrice())
                .append("，").append(sideName).append("数量为").append(order.getQuantity())
                .append("，当前价格为").append(order.getExtValue(OrderExt.CUR_PRICE))
                .append("，当前股票价格为").append(stockIndicator.getSecurityQuote().getLastDone())
                .append("，当前日期是").append(dateFormat.format(new Date()))
                .append("，接下来我将使用markdown格式给你提供一些信息，你需要根据信息给我当前这笔订单的交易建议。");

        order.setExtValue(OrderExt.PROMPT, prompt.toString());
    }

}
