package me.dingtou.options.service.impl;

import me.dingtou.options.constant.TradeSide;
import me.dingtou.options.manager.TradeManager;
import me.dingtou.options.model.Account;
import me.dingtou.options.model.Options;
import me.dingtou.options.model.Order;
import me.dingtou.options.service.OptionsTradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class OptionsTradeServiceImpl implements OptionsTradeService {

    @Autowired
    private TradeManager tradeManager;

    @Override
    public Order trade(Account account, TradeSide side, Long quantity, BigDecimal price, Options options) {
        return tradeManager.trade(account, side.getCode(), quantity, price, options);
    }
}
