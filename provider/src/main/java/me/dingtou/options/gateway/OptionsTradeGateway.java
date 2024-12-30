package me.dingtou.options.gateway;

import me.dingtou.options.model.OwnerOrder;
import me.dingtou.options.model.OwnerStrategy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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
    OwnerOrder trade(OwnerOrder order);

    /**
     * 取消订单
     *
     * @param order 订单
     * @return 订单
     */
    OwnerOrder cancel(OwnerOrder order);


    /**
     * 删除订单
     *
     * @param order 订单
     */
    Boolean delete(OwnerOrder order);

    /**
     * 计算订单总费用
     *
     * @param strategy 策略
     * @param orders   订单
     * @return 订单总费用
     */
    Map<String, BigDecimal> totalFee(OwnerStrategy strategy, List<OwnerOrder> orders);

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
