package me.dingtou.options.gateway.futu.executor.func.trade;

import com.futu.openapi.pb.TrdCommon;
import com.futu.openapi.pb.TrdGetPositionList;
import com.google.protobuf.GeneratedMessageV3;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.constant.Market;
import me.dingtou.options.gateway.futu.executor.TradeExecutor;
import me.dingtou.options.gateway.futu.executor.func.TradeFunctionCall;
import me.dingtou.options.gateway.futu.util.PositionUtils;
import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.model.OwnerPosition;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class FuncGetPosition implements TradeFunctionCall<List<OwnerPosition>> {

    private final OwnerAccount account;

    public FuncGetPosition(OwnerAccount account) {
        this.account = account;
    }

    @Override
    public boolean needUnlock() {
        return false;
    }

    @Override
    public void call(TradeExecutor<List<OwnerPosition>> client) {
        int trdMarket;
        if (account.getMarket().equals(Market.HK.getCode())) {
            trdMarket = TrdCommon.TrdMarket.TrdMarket_HK_VALUE;
        } else if (account.getMarket().equals(Market.US.getCode())) {
            trdMarket = TrdCommon.TrdMarket.TrdMarket_US_VALUE;
        } else {
            throw new IllegalArgumentException("不支持的交易市场");
        }

        TrdCommon.TrdHeader header = TrdCommon.TrdHeader.newBuilder()
                .setAccID(Long.parseLong(account.getAccountId()))
                .setTrdEnv(TrdCommon.TrdEnv.TrdEnv_Real_VALUE)
                .setTrdMarket(trdMarket)
                .build();

        TrdGetPositionList.C2S c2s = TrdGetPositionList.C2S.newBuilder()
                .setHeader(header)
                .build();
        TrdGetPositionList.Request req = TrdGetPositionList.Request.newBuilder().setC2S(c2s).build();
        int seqNo = client.getPositionList(req);
        log.warn("Send TrdGetPositionList: {}", seqNo);
    }

    @Override
    public List<OwnerPosition> result(GeneratedMessageV3 response) {
        TrdGetPositionList.Response rsp = (TrdGetPositionList.Response) response;
        return rsp.getS2C().getPositionListList().stream()
                .map(position -> PositionUtils.convertOwnerPosition(position, account.getOwner()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
