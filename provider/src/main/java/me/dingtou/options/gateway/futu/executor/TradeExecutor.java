package me.dingtou.options.gateway.futu.executor;

import com.futu.openapi.FTAPI_Conn;
import com.futu.openapi.FTAPI_Conn_Trd;
import com.futu.openapi.FTSPI_Conn;
import com.futu.openapi.FTSPI_Trd;
import com.futu.openapi.pb.*;
import com.google.protobuf.GeneratedMessageV3;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.gateway.futu.executor.func.TradeFunctionCall;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static me.dingtou.options.gateway.futu.executor.BaseConfig.*;

/**
 * futu api
 *
 * @author yuanhongbo
 */
@Slf4j
public class TradeExecutor<R> extends FTAPI_Conn_Trd implements FTSPI_Trd, FTSPI_Conn {

    private final CompletableFuture<GeneratedMessageV3> future = new CompletableFuture<>();

    private final TradeFunctionCall<R> call;

    public TradeExecutor(TradeFunctionCall<R> call) {
        this.call = call;
    }

    /**
     * 初始化
     *
     * @return futu api
     */
    public static <R> R submit(TradeFunctionCall<R> call) {
        try (TradeExecutor<R> client = new TradeExecutor<>(call)) {
            client.setClientInfo("javaClient", 1); // 设置客户端信息
            client.setConnSpi(client); // 设置连接回调
            client.setTrdSpi(client);// 设置交易回调
            boolean isEnableEncrypt = false;
            if (StringUtils.isNotBlank(FU_TU_API_PRIVATE_KEY)) {
                isEnableEncrypt = true;
                client.setRSAPrivateKey(FU_TU_API_PRIVATE_KEY);
            }
            boolean connect = client.initConnect(FU_TU_API_IP, FU_TU_API_PORT, isEnableEncrypt);
            if (!connect) {
                throw new RuntimeException("initConnect fail");
            }
            return client.call.result(client.future.get(30, TimeUnit.SECONDS));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onInitConnect(FTAPI_Conn client, long errCode, String desc) {
        log.warn("Qot onInitConnect: ret={} desc={} connID={}", errCode, desc, client.getConnectID());
        if (StringUtils.isBlank(FU_TU_TRADE_PWD_MD5) && call.needUnlock()) {
            log.warn("TradeExecutor needUnlock but FU_TU_TRADE_PWD_MD5 is empty");
            return;
        }
        if (call.needUnlock()) {
            TrdUnlockTrade.C2S c2s = TrdUnlockTrade.C2S.newBuilder()
                    .setPwdMD5(FU_TU_TRADE_PWD_MD5)
                    .setUnlock(true)
                    .setSecurityFirm(TrdCommon.SecurityFirm.SecurityFirm_FutuSecurities_VALUE)
                    .build();
            TrdUnlockTrade.Request req = TrdUnlockTrade.Request.newBuilder().setC2S(c2s).build();
            TradeExecutor<?> conn = (TradeExecutor<?>) client;
            int seqNo = conn.unlockTrade(req);
            log.warn("Send unlockTrade: {}", seqNo);
        } else {
            if (client instanceof TradeExecutor<?>) {
                call.call((TradeExecutor<R>) client);
            }
        }
    }

    /**
     * 统一处理返回
     *
     * @param rsp 返回结果
     */
    void handleQotOnReply(GeneratedMessageV3 rsp) {
        this.future.complete(rsp);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onReply_UnlockTrade(FTAPI_Conn client, int nSerialNo, TrdUnlockTrade.Response rsp) {
        if (rsp.getRetType() != Common.RetType.RetType_Succeed_VALUE) {
            log.warn("TrdUnlockTrade failed: {}", rsp.getRetMsg());
            return;
        }

        if (client instanceof TradeExecutor<?>) {
            call.call((TradeExecutor<R>) client);
        }

    }

    @Override
    public void onReply_PlaceOrder(FTAPI_Conn client, int nSerialNo, TrdPlaceOrder.Response rsp) {
        handleQotOnReply(rsp);
    }

    @Override
    public void onReply_ModifyOrder(FTAPI_Conn client, int nSerialNo, TrdModifyOrder.Response rsp) {
        handleQotOnReply(rsp);
    }

    @Override
    public void onReply_GetHistoryOrderList(FTAPI_Conn client, int nSerialNo, TrdGetHistoryOrderList.Response rsp) {
        handleQotOnReply(rsp);
    }

    @Override
    public void onReply_GetHistoryOrderFillList(FTAPI_Conn client, int nSerialNo,
            TrdGetHistoryOrderFillList.Response rsp) {
        handleQotOnReply(rsp);
    }

    @Override
    public void onReply_GetOrderFee(FTAPI_Conn client, int nSerialNo, TrdGetOrderFee.Response rsp) {
        handleQotOnReply(rsp);
    }

    @Override
    public void onReply_GetPositionList(FTAPI_Conn client, int nSerialNo, TrdGetPositionList.Response rsp) {
        handleQotOnReply(rsp);
    }
}