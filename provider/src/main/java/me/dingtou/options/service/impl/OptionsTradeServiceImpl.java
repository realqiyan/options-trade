package me.dingtou.options.service.impl;

import me.dingtou.options.constant.OrderAction;
import me.dingtou.options.constant.TradeSide;
import me.dingtou.options.manager.OwnerManager;
import me.dingtou.options.manager.TradeManager;
import me.dingtou.options.model.Options;
import me.dingtou.options.model.OwnerOrder;
import me.dingtou.options.model.OwnerStrategy;
import me.dingtou.options.service.OptionsTradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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
        return tradeManager.trade(ownerStrategy, side, quantity, price, options);
    }

    @Override
    public OwnerOrder close(OwnerOrder order, BigDecimal price) {
        OwnerOrder ownerOrder = ownerManager.queryOwnerOrder(order.getOwner(), order.getPlatform(), order.getPlatformOrderId(), order.getPlatformFillId());
        if (null == ownerOrder) {
            throw new IllegalArgumentException("订单不存在 platformOrderId:" + order.getPlatformOrderId());
        }
        return tradeManager.close(ownerOrder, price);
    }

    @Override
    public OwnerOrder modify(OwnerOrder order, OrderAction action) {
        if (null == order || null == order.getPlatform()) {
            return null;
        }
        OwnerOrder oldOrder = ownerManager.queryOwnerOrder(order.getOwner(), order.getPlatform(), order.getPlatformOrderId(), order.getPlatformFillId());
        if (OrderAction.CANCEL.equals(action)) {
            return tradeManager.cancel(oldOrder);
        }
        throw new IllegalArgumentException("不支持的操作");
    }

    @Override
    public List<OwnerOrder> sync(String owner) {
        List<OwnerOrder> orders = new ArrayList<>();
        List<OwnerStrategy> ownerStrategies = ownerManager.queryOwnerStrategy(owner);
        for (OwnerStrategy ownerStrategy : ownerStrategies) {
            orders.addAll(tradeManager.syncOrder(ownerStrategy));
        }
        return orders;
    }

}
