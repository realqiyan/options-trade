package me.dingtou.options.gateway.futu;

import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.constant.Platform;
import me.dingtou.options.gateway.OptionsTradeGateway;
import me.dingtou.options.gateway.futu.executor.TradeExecutor;
import me.dingtou.options.gateway.futu.executor.func.trade.FuncCancelOrder;
import me.dingtou.options.gateway.futu.executor.func.trade.FuncGetHistoryOrder;
import me.dingtou.options.gateway.futu.executor.func.trade.FuncGetHistoryOrderFill;
import me.dingtou.options.gateway.futu.executor.func.trade.FuncPlaceOrder;
import me.dingtou.options.model.OwnerOrder;
import me.dingtou.options.model.OwnerStrategy;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class OptionsTradeGatewayImpl implements OptionsTradeGateway {

    @Override
    public String trade(OwnerOrder order) {
        if (Platform.FUTU.getCode().equals(order.getPlatform())) {
            return TradeExecutor.submit(new FuncPlaceOrder(order));
        }
        throw new IllegalArgumentException("不支持的平台");
    }

    @Override
    public OwnerOrder cancel(OwnerOrder order) {
        if (Platform.FUTU.getCode().equals(order.getPlatform())) {
            return TradeExecutor.submit(new FuncCancelOrder(order));
        }
        throw new IllegalArgumentException("不支持的平台");
    }

    @Override
    public List<OwnerOrder> pullOrder(OwnerStrategy strategy) {
        if (!Platform.FUTU.getCode().equals(strategy.getPlatform())) {
            return Collections.emptyList();
        }

        return TradeExecutor.submit(new FuncGetHistoryOrder(strategy));
    }

    @Override
    public List<OwnerOrder> pullOrderFill(OwnerStrategy strategy) {
        if (!Platform.FUTU.getCode().equals(strategy.getPlatform())) {
            return Collections.emptyList();
        }
        return TradeExecutor.submit(new FuncGetHistoryOrderFill(strategy));
    }
}
