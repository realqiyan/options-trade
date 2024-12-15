package me.dingtou.options.service;

import me.dingtou.options.constant.OrderAction;
import me.dingtou.options.constant.TradeSide;
import me.dingtou.options.model.Options;
import me.dingtou.options.model.OwnerOrder;

import java.math.BigDecimal;
import java.util.List;

/**
 * 期权交易服务
 *
 * @author qiyan
 */
public interface OptionsTradeService {

    /**
     * 交易
     *
     * @param strategyId 策略ID
     * @param side       1:买，2:卖
     * @param quantity   交易数量
     * @param price      交易价格
     * @param options    期权
     * @return 订单
     */
    OwnerOrder submit(String strategyId, TradeSide side, Integer quantity, BigDecimal price, Options options);

    /**
     * 关闭头寸
     *
     * @param strategyId 策略ID
     * @param side       1:买，2:卖
     * @param quantity   交易数量
     * @param price      交易价格
     * @param order      订单
     * @return 订单
     */
    OwnerOrder close(String strategyId, TradeSide side, Integer quantity, BigDecimal price, OwnerOrder order);

    /**
     * 修改订单
     *
     * @param order  订单
     * @param action 操作
     * @return 订单
     */
    OwnerOrder modify(OwnerOrder order, OrderAction action);

    /**
     * 同步订单
     *
     * @param owner 账号
     * @return 订单列表
     */
    List<OwnerOrder> sync(String owner);


}
