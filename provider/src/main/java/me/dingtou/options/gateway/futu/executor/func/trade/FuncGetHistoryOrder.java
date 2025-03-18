package me.dingtou.options.gateway.futu.executor.func.trade;

import com.futu.openapi.pb.TrdCommon;
import com.futu.openapi.pb.TrdGetHistoryOrderList;
import com.google.protobuf.GeneratedMessageV3;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.constant.Market;
import me.dingtou.options.gateway.futu.executor.TradeExecutor;
import me.dingtou.options.gateway.futu.executor.func.TradeFunctionCall;
import me.dingtou.options.gateway.futu.util.OrderUtils;
import me.dingtou.options.model.Owner;
import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.model.OwnerOrder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class FuncGetHistoryOrder implements TradeFunctionCall<List<OwnerOrder>> {

    private final Owner owner;

    public FuncGetHistoryOrder(Owner owner) {
        this.owner = owner;
    }

    @Override
    public boolean needUnlock() {
        return false;
    }

    @Override
    public void call(TradeExecutor<List<OwnerOrder>> client) {
        OwnerAccount account = owner.getAccount();
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

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date now = new Date();
        TrdCommon.TrdFilterConditions.Builder builder = TrdCommon.TrdFilterConditions.newBuilder();

        TrdCommon.TrdFilterConditions filter = builder
                .setBeginTime(dateFormat.format(account.getCreateTime()))
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
        return orderListList.stream().map(order -> OrderUtils.convertOwnerOrder(order, owner)).filter(Objects::nonNull).collect(Collectors.toList());

    }

    
}
