package me.dingtou.options.gateway.futu;

import me.dingtou.options.constant.Platform;
import me.dingtou.options.gateway.OptionsTradeGateway;
import me.dingtou.options.model.OwnerOrder;
import org.springframework.stereotype.Component;

@Component
public class OptionsTradeGatewayImpl implements OptionsTradeGateway {


    @Override
    public String trade(OwnerOrder ownerOrder) {
        if (Platform.FUTU.getCode().equals(ownerOrder.getPlatform())) {
            return PlaceOrderExecutor.placeOrder(ownerOrder);
        }
        throw new IllegalArgumentException("不支持的平台");
    }
}
