package me.dingtou.options.gateway.futu;

import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.constant.Market;
import me.dingtou.options.gateway.OptionsTradeGateway;
import me.dingtou.options.gateway.futu.executor.OrderPushExecutor;
import me.dingtou.options.gateway.futu.executor.TradeExecutor;
import me.dingtou.options.gateway.futu.executor.func.trade.*;
import me.dingtou.options.model.Owner;
import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.model.OwnerFlowSummary;
import me.dingtou.options.model.OwnerOrder;
import me.dingtou.options.model.OwnerPosition;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
@Slf4j
public class OptionsTradeGatewayImpl implements OptionsTradeGateway {

    private static final int BATCH_SIZE = 300;

    @Override
    public OwnerOrder trade(OwnerAccount account, OwnerOrder order) {
        return TradeExecutor.submit(new FuncPlaceOrder(account, order));
    }

    @Override
    public OwnerOrder cancel(OwnerAccount account, OwnerOrder order) {
        return TradeExecutor.submit(new FuncCancelOrder(account, order));
    }

    @Override
    public Boolean delete(OwnerAccount account, OwnerOrder order) {
        return TradeExecutor.submit(new FuncDeleteOrder(account, order));
    }

    @Override
    public Map<String, BigDecimal> totalFee(OwnerAccount account, List<OwnerOrder> orders) {
        if (null == orders || orders.isEmpty()) {
            return Collections.emptyMap();
        }
        
        Map<String, BigDecimal> result = new java.util.HashMap<>();
        int totalOrders = orders.size();
        
        for (int i = 0; i < totalOrders; i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, totalOrders);
            List<OwnerOrder> batch = orders.subList(i, end);
            Map<String, BigDecimal> batchResult = TradeExecutor.submit(new FuncGetOrderFee(account, batch));
            result.putAll(batchResult);
        }
        
        return result;
    }

    @Override
    public List<OwnerOrder> pullOrder(Owner owner) {
        return TradeExecutor.submit(new FuncGetHistoryOrder(owner));
    }

    @Override
    public List<OwnerOrder> pullOrderFill(Owner owner) {
        return TradeExecutor.submit(new FuncGetHistoryOrderFill(owner));
    }

    @Override
    public void subscribeOrderPush(List<Owner> allOwner, Function<OwnerOrder, Void> callback) {
        OrderPushExecutor.submit(allOwner, callback);
    }

    @Override
    public List<OwnerPosition> getPosition(OwnerAccount account) {
        return TradeExecutor.submit(new FuncGetPosition(account));
    }

    @Override
    public List<OwnerFlowSummary> getFlowSummary(OwnerAccount account, Market market, String clearingDate) {
        return TradeExecutor.submit(new FuncGetFlowSummary(account, market, clearingDate));
    }
}
