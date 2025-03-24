package me.dingtou.options.service;

import me.dingtou.options.constant.OrderAction;
import me.dingtou.options.constant.TradeSide;
import me.dingtou.options.model.Options;
import me.dingtou.options.model.OwnerOrder;

import java.math.BigDecimal;
import java.util.Date;
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
     * @param owner      账号
     * @param orderId    订单ID
     * @param price      交易价格
     * @param cannelTime 平仓截止时间
     * @return 订单
     */
    OwnerOrder close(String owner, Long orderId, BigDecimal price, Date cannelTime);

    /**
     * 修改订单
     *
     * @param owner   账号
     * @param orderId 订单ID
     * @param action  操作
     * @return 订单
     */
    OwnerOrder modify(String owner, Long orderId, OrderAction action);


    /**
     * 同步订单
     *
     * @param owner 账号
     * @return 同步结果
     */
    Boolean sync(String owner);


    /**
     * 更新订单策略
     *
     * @param owner      owner
     * @param orderIds   订单ID
     * @param strategyId 策略ID
     * @return 更新数量
     */
    Integer updateOrderStrategy(String owner, List<Long> orderIds, String strategyId);
}
