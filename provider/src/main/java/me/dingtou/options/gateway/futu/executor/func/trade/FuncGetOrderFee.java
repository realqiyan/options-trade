package me.dingtou.options.gateway.futu.executor.func.trade;

import com.futu.openapi.pb.TrdCommon;
import com.futu.openapi.pb.TrdGetOrderFee;
import com.google.protobuf.GeneratedMessageV3;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.constant.Market;
import me.dingtou.options.gateway.futu.executor.TradeExecutor;
import me.dingtou.options.gateway.futu.executor.func.TradeFunctionCall;
import me.dingtou.options.model.OwnerOrder;
import me.dingtou.options.model.OwnerStrategy;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
public class FuncGetOrderFee implements TradeFunctionCall<BigDecimal> {

    private final OwnerStrategy strategy;
    private final List<OwnerOrder> orders;

    public FuncGetOrderFee(OwnerStrategy strategy, List<OwnerOrder> orders) {
        this.strategy = strategy;
        this.orders = orders;
    }

    @Override
    public boolean needUnlock() {
        return false;
    }

    @Override
    public void call(TradeExecutor<BigDecimal> client) {

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
        List<String> allOrderId = orders.stream().map(OwnerOrder::getPlatformOrderIdEx).toList();
        TrdGetOrderFee.C2S c2s = TrdGetOrderFee.C2S.newBuilder()
                .setHeader(header)
                .addAllOrderIdExList(allOrderId)
                .build();
        TrdGetOrderFee.Request req = TrdGetOrderFee.Request.newBuilder().setC2S(c2s).build();
        int seqNo = client.getOrderFee(req);
        log.warn("Send TrdGetOrderFee : {}", seqNo);
    }

    @Override
    public BigDecimal result(GeneratedMessageV3 response) {
        TrdGetOrderFee.Response rsp = (TrdGetOrderFee.Response) response;
        List<TrdCommon.OrderFee> orderFeeListList = rsp.getS2C().getOrderFeeListList();
        List<TrdCommon.OrderFeeItem> feeItems = orderFeeListList.stream().flatMap(o -> o.getFeeListList().stream()).toList();
        List<BigDecimal> feeList = feeItems.stream()
                .map(orderFee -> new BigDecimal(String.valueOf(orderFee.getValue())))
                .toList();
        return feeList.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
