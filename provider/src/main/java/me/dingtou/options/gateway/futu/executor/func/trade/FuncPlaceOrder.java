package me.dingtou.options.gateway.futu.executor.func.trade;

import com.futu.openapi.pb.TrdCommon;
import com.futu.openapi.pb.TrdPlaceOrder;
import com.google.protobuf.GeneratedMessageV3;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.constant.Market;
import me.dingtou.options.gateway.futu.executor.TradeExecutor;
import me.dingtou.options.gateway.futu.executor.func.TradeFunctionCall;
import me.dingtou.options.model.OwnerOrder;

import java.util.UUID;

@Slf4j
public class FuncPlaceOrder implements TradeFunctionCall<String> {

    private final OwnerOrder ownerOrder;

    public FuncPlaceOrder(OwnerOrder ownerOrder) {
        this.ownerOrder = ownerOrder;
    }

    @Override
    public String unlockResult() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    @Override
    public void call(TradeExecutor<String> client) {
        Market market = Market.of(ownerOrder.getMarket());
        int trdMarket;
        int secMarket;
        if (market.equals(Market.HK)) {
            trdMarket = TrdCommon.TrdMarket.TrdMarket_HK_VALUE;
            secMarket = TrdCommon.TrdSecMarket.TrdSecMarket_HK_VALUE;
        } else if (market.equals(Market.US)) {
            trdMarket = TrdCommon.TrdMarket.TrdMarket_US_VALUE;
            secMarket = TrdCommon.TrdSecMarket.TrdSecMarket_US_VALUE;
        } else {
            throw new IllegalArgumentException("不支持的交易市场");
        }

        TrdCommon.TrdHeader header = TrdCommon.TrdHeader.newBuilder()
                .setAccID(Long.parseLong(ownerOrder.getAccountId()))
                .setTrdEnv(TrdCommon.TrdEnv.TrdEnv_Real_VALUE)
                .setTrdMarket(trdMarket)
                .build();
        TrdPlaceOrder.C2S c2s = TrdPlaceOrder.C2S.newBuilder()
                .setPacketID(client.nextPacketID())
                .setHeader(header)
                .setTrdSide(ownerOrder.getSide())
                .setOrderType(TrdCommon.OrderType.OrderType_Normal_VALUE)
                .setSecMarket(secMarket)
                .setCode(ownerOrder.getCode())
                .setQty(ownerOrder.getQuantity())
                .setPrice(ownerOrder.getPrice().doubleValue())
                .setTimeInForce(TrdCommon.TimeInForce.TimeInForce_GTC_VALUE)
                .build();
        TrdPlaceOrder.Request req = TrdPlaceOrder.Request.newBuilder().setC2S(c2s).build();
        int seqNo = client.placeOrder(req);
        log.warn("Send TrdPlaceOrder: {}", seqNo);
    }

    @Override
    public String result(GeneratedMessageV3 response) {
        TrdPlaceOrder.Response resp = (TrdPlaceOrder.Response) response;
        long orderID = resp.getS2C().getOrderID();
        return String.valueOf(orderID);
    }
}
