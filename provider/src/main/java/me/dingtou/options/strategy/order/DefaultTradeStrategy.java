package me.dingtou.options.strategy.order;

import java.text.SimpleDateFormat;
import java.util.Date;
import me.dingtou.options.constant.OrderExt;
import me.dingtou.options.constant.TradeSide;
import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.model.OwnerOrder;
import me.dingtou.options.model.OwnerStrategy;
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
    public void calculate(OwnerAccount account, OwnerOrder order) {
        if (OwnerOrder.isStockOrder(order) || OwnerOrder.isClose(order) || !OwnerOrder.isTraded(order)) {
            return;
        }

        StringBuilder prompt = new StringBuilder();

        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        String sideName = TradeSide.of(order.getSide()).getName();

        prompt.append("我在").append(dateTimeFormat.format(order.getTradeTime())).append(sideName).append(order.getCode())
                .append("，订单编号：").append(order.getPlatformOrderId())
                .append("，行权日期为：").append(dateFormat.format(order.getStrikeTime()))
                .append("，行权价为：").append(OwnerOrder.strikePrice(order))
                .append("，").append(sideName).append("价格为：").append(order.getPrice())
                .append("，").append(sideName).append("数量为：").append(order.getQuantity())
                .append("，现在时间是：").append(dateTimeFormat.format(new Date()))
                .append("，策略ID：").append(summary.getStrategy().getStrategyId())
                .append("，期权策略Code：").append(summary.getStrategy().getStrategyCode())
                .append("，期权策略：").append(summary.getOptionsStrategy().getTitle())
                .append("，策略整体Delta：").append(summary.getStrategyDelta())
                .append("，策略平均每股Delta：").append(summary.getAvgDelta())
                .append("。请结合期权策略、股票趋势等信息，帮我分析当前订单如何处理。订单是继续持有，还是进行Roll，或是平仓，给我一些指导。");

        order.setExtValue(OrderExt.PROMPT, prompt.toString());
    }

}
