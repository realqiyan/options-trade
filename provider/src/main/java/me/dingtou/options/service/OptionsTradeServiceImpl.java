package me.dingtou.options.service;

import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.constant.OrderAction;
import me.dingtou.options.constant.OrderStatus;
import me.dingtou.options.constant.TradeSide;
import me.dingtou.options.job.JobClient;
import me.dingtou.options.job.JobContext;
import me.dingtou.options.job.background.CannelOrderJob;
import me.dingtou.options.job.background.CannelOrderJob.CannelOrderJobArgs;
import me.dingtou.options.manager.OwnerManager;
import me.dingtou.options.manager.TradeManager;
import me.dingtou.options.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class OptionsTradeServiceImpl implements OptionsTradeService {

    private final Map<String, Object> syncOrderLock = new ConcurrentHashMap<>();

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
    public OwnerOrder close(String owner, Long orderId, BigDecimal price, Date cannelTime) {
        OwnerOrder ownerOrder = ownerManager.queryOwnerOrder(owner, orderId);
        if (null == ownerOrder) {
            throw new IllegalArgumentException("订单不存在 orderId:" + orderId);
        }
        OwnerAccount account = ownerManager.queryOwnerAccount(owner);
        OwnerOrder closeOrder = tradeManager.close(account, ownerOrder, price);
        if (null != closeOrder && null != cannelTime) {
            CannelOrderJobArgs args = new CannelOrderJobArgs();
            args.setOwner(owner);
            args.setOrderId(closeOrder.getId());
            args.setCannelTime(cannelTime);
            JobClient.submit(new CannelOrderJob(), JobContext.of(args), cannelTime.toInstant());
        }
        return closeOrder;
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
        // 同步订单时，需要加锁，防止多个线程同时同步订单，导致订单数据不一致
        Object lock = syncOrderLock.putIfAbsent(owner, new Object());
        if (null == lock) {
            lock = syncOrderLock.get(owner);
        }
        synchronized (lock) {
            Owner ownerObj = ownerManager.queryOwner(owner);
            return tradeManager.syncOrder(ownerObj);
        }
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

    @Override
    public boolean updateOrderStatus(String owner, Long orderId, OrderStatus status) {
        OwnerAccount account = ownerManager.queryOwnerAccount(owner);
        if (null == account) {
            return false;
        }
        return tradeManager.updateOrderStatus(account, orderId, status);
    }

    @Override
    public boolean updateOrderIncome(String owner, Long orderId, BigDecimal manualIncome) {
        OwnerAccount account = ownerManager.queryOwnerAccount(owner);
        if (null == account) {
            return false;
        }
        return tradeManager.updateOrderIncome(owner, orderId, manualIncome);
    }
}
