package me.dingtou.options.gateway.futu;

import me.dingtou.options.gateway.OptionsTradeGateway;
import me.dingtou.options.model.Order;
import org.springframework.stereotype.Component;

@Component
public class OptionsTradeGatewayImpl implements OptionsTradeGateway {


    @Override
    public Order trade(Order order) {
        return PlaceOrderExecutor.placeOrder(order);
    }
}
