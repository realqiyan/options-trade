package me.dingtou.options.gateway;

import me.dingtou.options.model.Order;

/**
 * 期权交易
 *
 * @author qiyan
 */
public interface OptionsTradeGateway {

    /**
     * 交易
     *
     * @param order 订单
     * @return 订单
     */
    Order trade(Order order);
}
