package me.dingtou.options.gateway.futu.executor.func.trade;

import com.futu.openapi.pb.TrdCommon;
import com.futu.openapi.pb.TrdModifyOrder;
import com.google.protobuf.GeneratedMessageV3;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.constant.Market;
import me.dingtou.options.gateway.futu.executor.TradeExecutor;
import me.dingtou.options.gateway.futu.executor.func.TradeFunctionCall;
import me.dingtou.options.model.OwnerOrder;

@Slf4j
public class FuncCancelOrder implements TradeFunctionCall<OwnerOrder> {

    private final OwnerOrder ownerOrder;

    public FuncCancelOrder(OwnerOrder ownerOrder) {
        this.ownerOrder = ownerOrder;
    }

    @Override
    public OwnerOrder unlockResult() {
        return this.ownerOrder;
    }

    @Override
    public void call(TradeExecutor<OwnerOrder> client) {
        Market market = Market.of(ownerOrder.getMarket());
        int trdMarket;
        if (market.equals(Market.HK)) {
            trdMarket = TrdCommon.TrdMarket.TrdMarket_HK_VALUE;
        } else if (market.equals(Market.US)) {
            trdMarket = TrdCommon.TrdMarket.TrdMarket_US_VALUE;
        } else {
            throw new IllegalArgumentException("不支持的交易市场");
        }

        TrdCommon.TrdHeader header = TrdCommon.TrdHeader.newBuilder()
                .setAccID(Long.parseLong(ownerOrder.getAccountId()))
                .setTrdEnv(TrdCommon.TrdEnv.TrdEnv_Real_VALUE)
                .setTrdMarket(trdMarket)
                .build();
        TrdModifyOrder.C2S c2s = TrdModifyOrder.C2S.newBuilder()
                .setPacketID(client.nextPacketID())
                .setHeader(header)
                .setOrderID(Long.parseLong(ownerOrder.getPlatformOrderId()))
                .setModifyOrderOp(TrdCommon.ModifyOrderOp.ModifyOrderOp_Cancel_VALUE)
                .build();
        TrdModifyOrder.Request req = TrdModifyOrder.Request.newBuilder().setC2S(c2s).build();
        int seqNo = client.modifyOrder(req);
        log.warn("Send TrdModifyOrder: {}", seqNo);
    }

    @Override
    public OwnerOrder result(GeneratedMessageV3 response) {
        //TrdModifyOrder.Response rsp = (TrdModifyOrder.Response) response;
        return this.ownerOrder;
    }
}
