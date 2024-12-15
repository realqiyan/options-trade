package me.dingtou.options.gateway.futu;

import me.dingtou.options.constant.Platform;
import me.dingtou.options.gateway.OptionsTradeGateway;
import me.dingtou.options.gateway.futu.executor.TradeExecutor;
import me.dingtou.options.gateway.futu.executor.func.trade.FuncCancelOrder;
import me.dingtou.options.gateway.futu.executor.func.trade.FuncPlaceOrder;
import me.dingtou.options.model.OwnerOrder;
import org.springframework.stereotype.Component;

@Component
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
}
