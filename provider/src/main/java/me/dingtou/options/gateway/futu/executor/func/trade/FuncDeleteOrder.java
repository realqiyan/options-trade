package me.dingtou.options.gateway.futu.executor.func.trade;

import com.futu.openapi.pb.TrdCommon;
import com.futu.openapi.pb.TrdModifyOrder;
import com.google.protobuf.GeneratedMessageV3;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.constant.Market;
import me.dingtou.options.gateway.futu.executor.TradeExecutor;
import me.dingtou.options.gateway.futu.executor.func.TradeFunctionCall;
import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.model.OwnerOrder;

@Slf4j
public class FuncDeleteOrder implements TradeFunctionCall<Boolean> {

    private final OwnerAccount ownerAccount;
    private final OwnerOrder ownerOrder;

    public FuncDeleteOrder(OwnerAccount ownerAccount,OwnerOrder ownerOrder) {
        this.ownerAccount = ownerAccount;
        this.ownerOrder = ownerOrder;
    }

    @Override
    public void call(TradeExecutor<Boolean> client) {
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
                .setAccID(Long.parseLong(ownerAccount.getAccountId()))
                .setTrdEnv(TrdCommon.TrdEnv.TrdEnv_Real_VALUE)
                .setTrdMarket(trdMarket)
                .build();
        TrdModifyOrder.C2S c2s = TrdModifyOrder.C2S.newBuilder()
                .setPacketID(client.nextPacketID())
                .setHeader(header)
                .setOrderID(Long.parseLong(ownerOrder.getPlatformOrderId()))
                .setModifyOrderOp(TrdCommon.ModifyOrderOp.ModifyOrderOp_Delete_VALUE)
                .build();
        TrdModifyOrder.Request req = TrdModifyOrder.Request.newBuilder().setC2S(c2s).build();
        int seqNo = client.modifyOrder(req);
        log.warn("Send TrdModifyOrder: {}", seqNo);
    }

    @Override
    public Boolean result(GeneratedMessageV3 response) {
        TrdModifyOrder.Response rsp = (TrdModifyOrder.Response) response;
        return rsp.getRetType() == 0
                || rsp.getRetMsg().contains("当前状态为DELETED")
                || rsp.getRetMsg().contains("此订单号不存在");
    }
}
