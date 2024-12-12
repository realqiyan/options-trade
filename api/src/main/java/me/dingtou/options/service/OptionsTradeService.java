package me.dingtou.options.service;

import me.dingtou.options.constant.TradeSide;
import me.dingtou.options.model.Account;
import me.dingtou.options.model.Options;
import me.dingtou.options.model.Order;

import java.math.BigDecimal;

/**
 * 期权交易服务
 *
 * @author qiyan
 */
public interface OptionsTradeService {

    /**
     * 交易
     *
     * @param account  账号
     * @param side     1:买，2:卖
     * @param quantity 交易数量
     * @param price    交易价格
     * @param options  期权
     * @return 订单
     */
    Order trade(Account account, TradeSide side, Long quantity, BigDecimal price, Options options);
}
