package me.dingtou.options.gateway.futu;

import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.constant.Platform;
import me.dingtou.options.gateway.OptionsTradeGateway;
import me.dingtou.options.gateway.futu.executor.TradeExecutor;
import me.dingtou.options.gateway.futu.executor.func.trade.FuncCancelOrder;
import me.dingtou.options.gateway.futu.executor.func.trade.FuncGetHistoryOrder;
import me.dingtou.options.gateway.futu.executor.func.trade.FuncPlaceOrder;
import me.dingtou.options.model.OwnerOrder;
import me.dingtou.options.model.OwnerStrategy;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Slf4j
public class OptionsTradeGatewayImpl implements OptionsTradeGateway {

    private static final boolean DEBUG = false;

    @Override
    public String trade(OwnerOrder order) {
        if (DEBUG) {
            return UUID.randomUUID().toString().replaceAll("-", "");
        }
        if (Platform.FUTU.getCode().equals(order.getPlatform())) {
            return TradeExecutor.submit(new FuncPlaceOrder(order));
        }
        throw new IllegalArgumentException("不支持的平台");
    }

    @Override
    public OwnerOrder cancel(OwnerOrder order) {
        if (DEBUG) {
            return order;
        }
        if (Platform.FUTU.getCode().equals(order.getPlatform())) {
            return TradeExecutor.submit(new FuncCancelOrder(order));
        }
        throw new IllegalArgumentException("不支持的平台");
    }

    @Override
    public List<OwnerOrder> syncOrder(OwnerStrategy strategy, List<OwnerOrder> orderList) {
        if (DEBUG) {
            return orderList;
        }
        List<OwnerOrder> ownerOrderList = orderList.stream()
                .filter(order -> Platform.FUTU.getCode().equals(order.getPlatform()))
                .filter(order -> strategy.getStrategyId().equals(order.getStrategyId()))
                .filter(order -> strategy.getAccountId().equals(order.getAccountId()))
                .collect(Collectors.toList());
        if (!ownerOrderList.isEmpty()) {
            return TradeExecutor.submit(new FuncGetHistoryOrder(strategy, ownerOrderList));
        }
        log.warn("没有需要同步的订单 strategy:{}", strategy);
        return Collections.emptyList();
    }
}
