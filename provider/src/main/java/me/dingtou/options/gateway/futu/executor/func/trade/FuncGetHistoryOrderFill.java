package me.dingtou.options.gateway.futu.executor.func.trade;

import com.futu.openapi.pb.TrdCommon;
import com.futu.openapi.pb.TrdGetHistoryOrderFillList;
import com.google.protobuf.GeneratedMessageV3;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.constant.Market;
import me.dingtou.options.constant.OrderStatus;
import me.dingtou.options.constant.TradeFrom;
import me.dingtou.options.constant.TradeSide;
import me.dingtou.options.gateway.futu.executor.TradeExecutor;
import me.dingtou.options.gateway.futu.executor.func.TradeFunctionCall;
import me.dingtou.options.model.OwnerOrder;
import me.dingtou.options.model.OwnerStrategy;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class FuncGetHistoryOrderFill implements TradeFunctionCall<List<OwnerOrder>> {

    private final OwnerStrategy strategy;

    public FuncGetHistoryOrderFill(OwnerStrategy strategy) {
        this.strategy = strategy;
    }

    @Override
    public boolean needUnlock() {
        return false;
    }

    @Override
    public List<OwnerOrder> unlockResult() {
        return Collections.emptyList();
    }

    @Override
    public void call(TradeExecutor<List<OwnerOrder>> client) {
        Date now = new Date();
        int trdMarket;
        if (strategy.getMarket().equals(Market.HK.getCode())) {
            trdMarket = TrdCommon.TrdMarket.TrdMarket_HK_VALUE;
        } else if (strategy.getMarket().equals(Market.US.getCode())) {
            trdMarket = TrdCommon.TrdMarket.TrdMarket_US_VALUE;
        } else {
            throw new IllegalArgumentException("不支持的交易市场");
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        TrdCommon.TrdHeader header = TrdCommon.TrdHeader.newBuilder().setAccID(Long.parseLong(strategy.getAccountId())).setTrdEnv(TrdCommon.TrdEnv.TrdEnv_Real_VALUE).setTrdMarket(trdMarket).build();
        TrdCommon.TrdFilterConditions filter = TrdCommon.TrdFilterConditions.newBuilder().setBeginTime(dateFormat.format(this.strategy.getStartTime())).setEndTime(dateFormat.format(now)).build();
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

        return orderFillListList.stream().map(this::convertOwnerOrder).filter(Objects::nonNull).collect(Collectors.toList());


    }

    private OwnerOrder convertOwnerOrder(TrdCommon.OrderFill order) {
        try {
            //BABA 241220 83.00P
            String code = order.getCode();
            String regexStr = "^(" + this.strategy.getCode() + ')' + "([0-9]{6})" + "([CP])" + "([0-9]*)$";
            Pattern codePattern = Pattern.compile(regexStr);
            Matcher matcher = codePattern.matcher(code);
            if (!matcher.find()) {
                log.warn("订单匹配错误: {} -> {}", code, regexStr);
                return null;
            }
            String strikeTimeStr = matcher.group(2);

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            SimpleDateFormat strikeTimeFormat = new SimpleDateFormat("yyMMdd");
            OwnerOrder ownerOrder = new OwnerOrder();
            ownerOrder.setOwner(this.strategy.getOwner());
            ownerOrder.setPlatform(this.strategy.getPlatform());
            ownerOrder.setUnderlyingCode(this.strategy.getCode());
            ownerOrder.setMarket(this.strategy.getMarket());
            ownerOrder.setStrategyId(this.strategy.getStrategyId());
            ownerOrder.setAccountId(this.strategy.getAccountId());

            ownerOrder.setSide(TradeSide.of(order.getTrdSide()).getCode());
            ownerOrder.setPlatformOrderId(String.valueOf(order.getOrderID()));
            ownerOrder.setPlatformOrderIdEx(order.getOrderIDEx());
            ownerOrder.setPlatformFillId(String.valueOf(order.getFillID()));
            ownerOrder.setStrikeTime(strikeTimeFormat.parse(strikeTimeStr));
            ownerOrder.setTradeTime(dateFormat.parse(order.getCreateTime()));
            ownerOrder.setCreateTime(dateFormat.parse(order.getCreateTime()));
            ownerOrder.setUpdateTime(new Date((long) (order.getUpdateTimestamp() * 1000)));
            ownerOrder.setCode(order.getCode());
            ownerOrder.setPrice(BigDecimal.valueOf(order.getPrice()));
            ownerOrder.setQuantity((int) order.getQty());

            ownerOrder.setStatus(OrderStatus.FILLED_ALL.getCode());
            ownerOrder.setTradeFrom(TradeFrom.PULL_FILL.getCode());

            return ownerOrder;
        } catch (Exception e) {
            log.error("convertOwnerOrder error. order: {}", order, e);
            return null;
        }
    }
}
