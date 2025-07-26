package me.dingtou.options.strategy.order;

import java.text.SimpleDateFormat;
import java.util.Date;
import me.dingtou.options.constant.OptionsStrategy;
import me.dingtou.options.constant.OrderExt;
import me.dingtou.options.constant.TradeSide;
import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.model.OwnerOrder;
import me.dingtou.options.model.OwnerStrategy;
import me.dingtou.options.model.StockIndicator;
import me.dingtou.options.model.StrategySummary;
import me.dingtou.options.strategy.OrderTradeStrategy;

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

        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        String sideName = TradeSide.of(order.getSide()).getName();

        OptionsStrategy strategy = OptionsStrategy.of(summary.getStrategy().getStrategyCode());

        prompt.append("我在").append(dateTimeFormat.format(order.getTradeTime())).append(sideName).append(order.getCode())
                .append("，行权日期为").append(dateFormat.format(order.getStrikeTime()))
                .append("，行权价为").append(OwnerOrder.strikePrice(order))
                .append("，").append(sideName).append("价格为").append(order.getPrice())
                .append("，").append(sideName).append("数量为").append(order.getQuantity())
                .append("，当前价格为").append(order.getExtValue(OrderExt.CUR_PRICE))
                .append("，当前股票价格为").append(stockIndicator.getSecurityQuote().getLastDone())
                .append("，现在时间是").append(dateTimeFormat.format(new Date()))
                .append("，当前使用的策略是").append(strategy.getName())
                .append("，策略ID：").append(summary.getStrategy().getStrategyId())
                .append("，策略Delta：").append(summary.getStrategyDelta())
                .append("，请给我一些交易建议。");

        order.setExtValue(OrderExt.PROMPT, prompt.toString());
    }

}
