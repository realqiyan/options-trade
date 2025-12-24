package me.dingtou.options.gateway.futu.executor.func.trade;

import com.futu.openapi.pb.TrdCommon;
import com.futu.openapi.pb.TrdFlowSummary;
import com.google.protobuf.GeneratedMessageV3;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.constant.Market;
import me.dingtou.options.gateway.futu.executor.TradeExecutor;
import me.dingtou.options.gateway.futu.executor.func.TradeFunctionCall;
import me.dingtou.options.gateway.futu.util.RateLimiter;
import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.model.OwnerFlowSummary;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class FuncGetFlowSummary implements TradeFunctionCall<List<OwnerFlowSummary>> {

    private static final RateLimiter RATE_LIMITER = new RateLimiter("GetFlowSummary", 30000, 20);

    private final OwnerAccount account;
    private final String clearingDate;

    public FuncGetFlowSummary(OwnerAccount account, String clearingDate) {
        this.account = account;
        this.clearingDate = clearingDate;
    }

    @Override
    public boolean needUnlock() {
        return false;
    }

    @Override
    public void call(TradeExecutor<List<OwnerFlowSummary>> client) {
        RATE_LIMITER.acquire();

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

        TrdFlowSummary.C2S c2s = TrdFlowSummary.C2S.newBuilder()
                .setHeader(header)
                .setClearingDate(clearingDate)
                .build();
        TrdFlowSummary.Request req = TrdFlowSummary.Request.newBuilder().setC2S(c2s).build();

        int seqNo = client.getFlowSummary(req);
        log.warn("Send TrdFlowSummary: {}", seqNo);
    }

    @Override
    public List<OwnerFlowSummary> result(GeneratedMessageV3 response) {
        TrdFlowSummary.Response rsp = (TrdFlowSummary.Response) response;
        return rsp.getS2C()
                .getFlowSummaryInfoListList()
                .stream()
                .map(this::convertOwnerFlowSummary)
                .collect(Collectors.toList());
    }

    private OwnerFlowSummary convertOwnerFlowSummary(TrdFlowSummary.FlowSummaryInfo cashFlow) {
        try {
            OwnerFlowSummary summary = new OwnerFlowSummary();
            summary.setOwner(account.getOwner());
            summary.setPlatform(account.getPlatform());
            summary.setCashflowId(cashFlow.getCashFlowID());
            summary.setClearingDate(parseDate(cashFlow.getClearingDate()));
        if (cashFlow.hasSettlementDate()) {
            summary.setSettlementDate(parseDate(cashFlow.getSettlementDate()));
        }
        summary.setCurrency(String.valueOf(cashFlow.getCurrency()));
        summary.setCashflowType(cashFlow.getCashFlowType());
        summary.setCashflowDirection(String.valueOf(cashFlow.getCashFlowDirection()));
        summary.setCashflowAmount(BigDecimal.valueOf(cashFlow.getCashFlowAmount()));
            summary.setCashflowRemark(cashFlow.getCashFlowRemark());
            summary.setCreateTime(new Date());
            summary.setUpdateTime(new Date());
            return summary;
        } catch (Exception e) {
            log.error("convertOwnerFlowSummary error. cashFlow: {}", cashFlow, e);
            return null;
        }
    }

    private Date parseDate(String dateStr) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return dateFormat.parse(dateStr);
    }

}
