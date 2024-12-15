package me.dingtou.options.gateway.futu.executor.func.trade;

import com.futu.openapi.pb.TrdCommon;
import com.futu.openapi.pb.TrdGetHistoryOrderList;
import com.google.protobuf.GeneratedMessageV3;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.constant.Market;
import me.dingtou.options.constant.OrderStatus;
import me.dingtou.options.gateway.futu.executor.TradeExecutor;
import me.dingtou.options.gateway.futu.executor.func.TradeFunctionCall;
import me.dingtou.options.model.OwnerOrder;
import me.dingtou.options.model.OwnerStrategy;
import org.apache.commons.lang3.time.DateUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class FuncGetHistoryOrder implements TradeFunctionCall<List<OwnerOrder>> {

    private final OwnerStrategy strategy;
    private final List<OwnerOrder> orderList;

    public FuncGetHistoryOrder(OwnerStrategy strategy, List<OwnerOrder> orderList) {
        this.strategy = strategy;
        this.orderList = orderList;
    }

    @Override
    public boolean needUnlock() {
        return false;
    }

    @Override
    public void call(TradeExecutor<List<OwnerOrder>> client) {

        int trdMarket;
        if (strategy.getMarket().equals(Market.HK.getCode())) {
            trdMarket = TrdCommon.TrdMarket.TrdMarket_HK_VALUE;
        } else if (strategy.getMarket().equals(Market.US.getCode())) {
            trdMarket = TrdCommon.TrdMarket.TrdMarket_US_VALUE;
        } else {
            throw new IllegalArgumentException("不支持的交易市场");
        }

        TrdCommon.TrdHeader header = TrdCommon.TrdHeader.newBuilder()
                .setAccID(Long.parseLong(strategy.getAccountId()))
                .setTrdEnv(TrdCommon.TrdEnv.TrdEnv_Real_VALUE)
                .setTrdMarket(trdMarket)
                .build();
        List<Long> orderIds = this.orderList.stream()
                .map(ownerOrder -> Long.parseLong(ownerOrder.getPlatformOrderId()))
                .collect(Collectors.toList());

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date now = new Date();
        Date lastYear = DateUtils.addYears(now, -1);
        TrdCommon.TrdFilterConditions filter = TrdCommon.TrdFilterConditions.newBuilder()
                .addAllIdList(orderIds)
                .setBeginTime(dateFormat.format(lastYear))
                .setEndTime(dateFormat.format(now))
                .build();
        TrdGetHistoryOrderList.C2S c2s = TrdGetHistoryOrderList.C2S.newBuilder()
                .setHeader(header)
                .setFilterConditions(filter)
                .build();
        TrdGetHistoryOrderList.Request req = TrdGetHistoryOrderList.Request.newBuilder().setC2S(c2s).build();
        int seqNo = client.getHistoryOrderList(req);
        log.warn("Send TrdGetHistoryOrderList: {}", seqNo);
    }

    @Override
    public List<OwnerOrder> result(GeneratedMessageV3 response) {
        TrdGetHistoryOrderList.Response rsp = (TrdGetHistoryOrderList.Response) response;
        List<TrdCommon.Order> orderListList = rsp.getS2C().getOrderListList();
        Map<Long, TrdCommon.Order> orderMap = orderListList.stream().collect(Collectors.toMap(TrdCommon.Order::getOrderID, order -> order));
        for (OwnerOrder order : this.orderList) {
            TrdCommon.Order trdOrder = orderMap.get(Long.parseLong(order.getPlatformOrderId()));
            if (trdOrder != null) {
                order.setStatus(OrderStatus.of(trdOrder.getOrderStatus()).getCode());
            }
        }
        return this.orderList;
    }
}
