package me.dingtou.options.strategy;

import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.model.OwnerOrder;
import me.dingtou.options.model.OwnerStrategy;

/**
 * 订单交易策略
 *
 * @author qiyan
 */
public interface OrderTradeStrategy {

    /**
     * 是否支持该策略
     *
     * @param strategy 策略
     * @return true/false
     */
    boolean isSupport(OwnerStrategy strategy);

    /**
     * 计算订单处理
     * 
     * @param account 账户
     * @param order   订单
     */
    void calculate(OwnerAccount account, OwnerOrder order);
}
