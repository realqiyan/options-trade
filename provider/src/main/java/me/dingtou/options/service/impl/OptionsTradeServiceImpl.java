package me.dingtou.options.service.impl;

import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.constant.OrderAction;
import me.dingtou.options.constant.TradeSide;
import me.dingtou.options.manager.OwnerManager;
import me.dingtou.options.manager.TradeManager;
import me.dingtou.options.model.*;
import me.dingtou.options.service.OptionsTradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
public class OptionsTradeServiceImpl implements OptionsTradeService {

    @Autowired
    private TradeManager tradeManager;

    @Autowired
    private OwnerManager ownerManager;

    @Override
    public OwnerOrder submit(String strategyId, TradeSide side, Integer quantity, BigDecimal price, Options options) {
        OwnerStrategy ownerStrategy = tradeManager.queryStrategy(strategyId);
        if (null == ownerStrategy) {
            throw new IllegalArgumentException("策略不存在 strategyId:" + strategyId);
        }
        OwnerAccount account = ownerManager.queryOwnerAccount(ownerStrategy.getOwner());
        return tradeManager.trade(account, ownerStrategy, side, quantity, price, options);
    }

    @Override
    public OwnerOrder close(String owner, Long orderId, BigDecimal price) {
        OwnerOrder ownerOrder = ownerManager.queryOwnerOrder(owner, orderId);
        if (null == ownerOrder) {
            throw new IllegalArgumentException("订单不存在 orderId:" + orderId);
        }
        OwnerAccount account = ownerManager.queryOwnerAccount(owner);
        return tradeManager.close(account, ownerOrder, price);
    }

    @Override
    public OwnerOrder modify(String owner, Long orderId, OrderAction action) {
        OwnerOrder order = ownerManager.queryOwnerOrder(owner, orderId);
        if (null == order) {
            return null;
        }
        OwnerAccount account = ownerManager.queryOwnerAccount(order.getOwner());
        switch (action) {
            case CANCEL:
                return tradeManager.cancel(account, order);
            case DELETE:
                Boolean delete = tradeManager.delete(account, order);
                log.warn("delete orderId:{} result:{}", orderId, delete);
                return order;
            default:
                throw new IllegalArgumentException("不支持的操作");
        }
    }

    @Override
    public Boolean sync(String owner) {
        Owner ownerObj = ownerManager.queryOwner(owner);
        return tradeManager.syncOrder(ownerObj);
    }

    @Override
    public Integer updateOrderStrategy(String owner, List<Long> orderIds, String strategyId) {
        OwnerAccount ownerAccount = ownerManager.queryOwnerAccount(owner);
        if (null == ownerAccount) {
            return 0;
        }
        OwnerStrategy ownerStrategy = tradeManager.queryStrategy(strategyId);
        if (null == ownerStrategy || !owner.equals(ownerStrategy.getOwner())) {
            return 0;
        }
        return tradeManager.updateOrderStrategy(ownerAccount, orderIds, ownerStrategy);
    }

}
