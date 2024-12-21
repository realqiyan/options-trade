package me.dingtou.options.gateway;

import me.dingtou.options.model.OwnerOrder;
import me.dingtou.options.model.OwnerStrategy;

import java.util.List;

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
     * @return 外部订单号
     */
    String trade(OwnerOrder order);

    /**
     * 取消订单
     *
     * @param order 订单
     * @return 订单
     */
    OwnerOrder cancel(OwnerOrder order);

    /**
     * 同步订单
     *
     * @param strategy 策略
     * @return 订单列表
     */
    List<OwnerOrder> pullOrder(OwnerStrategy strategy);

    /**
     * 拉取成交单
     *
     * @param ownerStrategy 策略
     * @return 订单列表
     */
    List<OwnerOrder> pullOrderFill(OwnerStrategy ownerStrategy);
}
