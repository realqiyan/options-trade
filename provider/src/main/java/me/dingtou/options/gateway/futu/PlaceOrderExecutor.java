package me.dingtou.options.gateway.futu;

import com.futu.openapi.*;
import com.futu.openapi.pb.*;
import com.google.protobuf.GeneratedMessageV3;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.constant.Market;
import me.dingtou.options.model.Order;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * futu api
 *
 * @author yuanhongbo
 */
@Slf4j
public class PlaceOrderExecutor extends FTAPI_Conn_Trd implements FTSPI_Trd, FTSPI_Conn {

    private static final String PWD_MD5;
    public static final String TRADE_MARKET = "futu";

    static {
        try {
            URI uri = Objects.requireNonNull(BaseQueryFuncExecutor.class.getResource("/key/trade.key")).toURI();
            byte[] buf = Files.readAllBytes(Paths.get(uri));
            PWD_MD5 = new String(buf, StandardCharsets.UTF_8);

            FTAPI.init();
        } catch (Exception e) {
            throw new RuntimeException("init BaseQueryFuncExecutor error", e);
        }
    }

    private final Object syncEvent = new Object();
    private final Order order;

    private GeneratedMessageV3 resp;

    public PlaceOrderExecutor(Order order) {
        this.order = order;
    }


    /**
     * 初始化
     *
     * @return futu api
     */
    public static Order placeOrder(Order order) {
        try (PlaceOrderExecutor client = new PlaceOrderExecutor(order)) {
            client.setClientInfo("javaClient", 1);  //设置客户端信息
            client.setConnSpi(client);  //设置连接回调
            client.setTrdSpi(client);//设置交易回调
            boolean isEnableEncrypt = false;
            if (StringUtils.isNotBlank(BaseQueryFuncExecutor.FU_TU_API_PRIVATE_KEY)) {
                isEnableEncrypt = true;
                client.setRSAPrivateKey(BaseQueryFuncExecutor.FU_TU_API_PRIVATE_KEY);
            }
            client.initConnect(BaseQueryFuncExecutor.FU_TU_API_IP, BaseQueryFuncExecutor.FU_TU_API_PORT, isEnableEncrypt);

            try {
                synchronized (client.syncEvent) {
                    client.syncEvent.wait(12000);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException("call timeout");
            }


            TrdPlaceOrder.Response resp = (TrdPlaceOrder.Response) client.resp;

            if (null == resp || 0 != resp.getRetType()) {
                throw new RuntimeException(null != resp ? resp.getRetMsg() : "placeOrder error");
            }

            long orderID = resp.getS2C().getOrderID();
            order.setOrderId(String.valueOf(orderID));
            order.setTradeMarket(TRADE_MARKET);

            return order;
        }
    }


    @Override
    public void onInitConnect(FTAPI_Conn client, long errCode, String desc) {
        log.warn("Qot onInitConnect: ret={} desc={} connID={}", errCode, desc, client.getConnectID());
        TrdUnlockTrade.C2S c2s = TrdUnlockTrade.C2S.newBuilder()
                .setPwdMD5(PWD_MD5)
                .setUnlock(true)
                .setSecurityFirm(TrdCommon.SecurityFirm.SecurityFirm_FutuSecurities_VALUE)
                .build();
        TrdUnlockTrade.Request req = TrdUnlockTrade.Request.newBuilder().setC2S(c2s).build();
        PlaceOrderExecutor conn = (PlaceOrderExecutor) client;
        int seqNo = conn.unlockTrade(req);
        log.warn("Send unlockTrade: {}", seqNo);
    }

    @Override
    public void onReply_UnlockTrade(FTAPI_Conn client, int nSerialNo, TrdUnlockTrade.Response rsp) {
        if (rsp.getRetType() != 0) {
            log.warn("TrdUnlockTrade failed: {}", rsp.getRetMsg());
            return;
        }
        PlaceOrderExecutor conn = (PlaceOrderExecutor) client;
        Market market = Market.of(order.getSecurity().getMarket());
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
                .setAccID(Long.parseLong(order.getAccount().getAccountId()))
                .setTrdEnv(TrdCommon.TrdEnv.TrdEnv_Real_VALUE)
                .setTrdMarket(trdMarket)
                .build();
        TrdPlaceOrder.C2S c2s = TrdPlaceOrder.C2S.newBuilder()
                .setPacketID(conn.nextPacketID())
                .setHeader(header)
                .setTrdSide(order.getSide())
                .setOrderType(TrdCommon.OrderType.OrderType_Normal_VALUE)
                .setSecMarket(secMarket)
                .setCode(order.getSecurity().getCode())
                .setQty(order.getQuantity())
                .setPrice(order.getPrice().doubleValue())
                .build();
        TrdPlaceOrder.Request req = TrdPlaceOrder.Request.newBuilder().setC2S(c2s).build();
        int seqNo = conn.placeOrder(req);
        log.warn("Send TrdPlaceOrder: {}", seqNo);
    }

    @Override
    public void onReply_PlaceOrder(FTAPI_Conn client, int nSerialNo, TrdPlaceOrder.Response rsp) {
        if (rsp.getRetType() != 0) {
            log.warn("TrdPlaceOrder failed: {}", rsp.getRetMsg());
            return;
        }
        PlaceOrderExecutor conn = (PlaceOrderExecutor) client;
        conn.resp = rsp;
        synchronized (conn.syncEvent) {
            conn.syncEvent.notifyAll();
        }
    }


}