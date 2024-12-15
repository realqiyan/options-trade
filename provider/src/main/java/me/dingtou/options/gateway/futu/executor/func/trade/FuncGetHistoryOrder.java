package me.dingtou.options.gateway.futu.executor.func.trade;

import com.google.protobuf.GeneratedMessageV3;
import me.dingtou.options.gateway.futu.executor.TradeExecutor;
import me.dingtou.options.gateway.futu.executor.func.TradeFunctionCall;
import me.dingtou.options.model.OwnerOrder;

import java.util.List;

public class FuncGetHistoryOrder implements TradeFunctionCall<List<OwnerOrder>> {

    @Override
    public boolean needUnlock() {
        return false;
    }

    @Override
    public void call(TradeExecutor<List<OwnerOrder>> client) {

    }

    @Override
    public List<OwnerOrder> result(GeneratedMessageV3 response) {
        return null;
    }
}
