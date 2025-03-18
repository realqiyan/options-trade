package me.dingtou.options.gateway.futu.executor.func.trade;

import com.futu.openapi.pb.TrdCommon;
import com.futu.openapi.pb.TrdGetHistoryOrderFillList;
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
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class FuncGetHistoryOrderFill implements TradeFunctionCall<List<OwnerOrder>> {

    private final Owner owner;

    public FuncGetHistoryOrderFill(Owner owner) {
        this.owner = owner;
    }

    @Override
    public boolean needUnlock() {
        return false;
    }


    @Override
    public void call(TradeExecutor<List<OwnerOrder>> client) {
        Date now = new Date();
        OwnerAccount account = owner.getAccount();
        int trdMarket;
        if (account.getMarket().equals(Market.HK.getCode())) {
            trdMarket = TrdCommon.TrdMarket.TrdMarket_HK_VALUE;
        } else if (account.getMarket().equals(Market.US.getCode())) {
            trdMarket = TrdCommon.TrdMarket.TrdMarket_US_VALUE;
        } else {
            throw new IllegalArgumentException("不支持的交易市场");
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        TrdCommon.TrdHeader header = TrdCommon.TrdHeader.newBuilder().setAccID(Long.parseLong(account.getAccountId())).setTrdEnv(TrdCommon.TrdEnv.TrdEnv_Real_VALUE).setTrdMarket(trdMarket).build();
        TrdCommon.TrdFilterConditions filter = TrdCommon.TrdFilterConditions.newBuilder().setBeginTime(dateFormat.format(account.getCreateTime())).setEndTime(dateFormat.format(now)).build();
        TrdGetHistoryOrderFillList.C2S c2s = TrdGetHistoryOrderFillList.C2S.newBuilder().setHeader(header).setFilterConditions(filter).build();
        TrdGetHistoryOrderFillList.Request req = TrdGetHistoryOrderFillList.Request.newBuilder().setC2S(c2s).build();
        int seqNo = client.getHistoryOrderFillList(req);
        log.warn("Send TrdGetHistoryOrderFillList: {}", seqNo);
    }

    @Override
    public List<OwnerOrder> result(GeneratedMessageV3 response) {
        TrdGetHistoryOrderFillList.Response rsp = (TrdGetHistoryOrderFillList.Response) response;
        List<TrdCommon.OrderFill> orderFillListList = rsp.getS2C().getOrderFillListList();

        if (orderFillListList.isEmpty()) {
            return Collections.emptyList();
        }

        return orderFillListList.stream().map(orderFill -> OrderUtils.convertOwnerOrder(orderFill, owner)).filter(Objects::nonNull).collect(Collectors.toList());


    }

}
