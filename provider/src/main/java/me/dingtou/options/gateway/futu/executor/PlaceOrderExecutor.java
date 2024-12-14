package me.dingtou.options.gateway.futu.executor;

import com.futu.openapi.FTAPI_Conn;
import com.futu.openapi.FTAPI_Conn_Trd;
import com.futu.openapi.FTSPI_Conn;
import com.futu.openapi.FTSPI_Trd;
import com.futu.openapi.pb.TrdCommon;
import com.futu.openapi.pb.TrdPlaceOrder;
import com.futu.openapi.pb.TrdUnlockTrade;
import com.google.protobuf.GeneratedMessageV3;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.constant.Market;
import me.dingtou.options.model.OwnerOrder;
import org.apache.commons.lang3.StringUtils;

import static me.dingtou.options.gateway.futu.executor.BaseConfig.*;

/**
 * futu api
 *
 * @author yuanhongbo
 */
@Slf4j
public class PlaceOrderExecutor extends FTAPI_Conn_Trd implements FTSPI_Trd, FTSPI_Conn {


    private final Object syncEvent = new Object();
    private final OwnerOrder ownerOrder;

    private GeneratedMessageV3 resp;

    public PlaceOrderExecutor(OwnerOrder ownerOrder) {
        this.ownerOrder = ownerOrder;
    }


    /**
     * 初始化
     *
     * @return futu api
     */
    public static String placeOrder(OwnerOrder ownerOrder) {
        try (PlaceOrderExecutor client = new PlaceOrderExecutor(ownerOrder)) {
            client.setClientInfo("javaClient", 1);  //设置客户端信息
            client.setConnSpi(client);  //设置连接回调
            client.setTrdSpi(client);//设置交易回调
            boolean isEnableEncrypt = false;
            if (StringUtils.isNotBlank(FU_TU_API_PRIVATE_KEY)) {
                isEnableEncrypt = true;
                client.setRSAPrivateKey(FU_TU_API_PRIVATE_KEY);
            }
            client.initConnect(FU_TU_API_IP, FU_TU_API_PORT, isEnableEncrypt);

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
            // ownerOrder.setPlatformOrderId(String.valueOf(orderID));
            return String.valueOf(orderID);
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
        Market market = Market.of(ownerOrder.getMarket());
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
                .setAccID(Long.parseLong(ownerOrder.getAccountId()))
                .setTrdEnv(TrdCommon.TrdEnv.TrdEnv_Real_VALUE)
                .setTrdMarket(trdMarket)
                .build();
        TrdPlaceOrder.C2S c2s = TrdPlaceOrder.C2S.newBuilder()
                .setPacketID(conn.nextPacketID())
                .setHeader(header)
                .setTrdSide(ownerOrder.getSide())
                .setOrderType(TrdCommon.OrderType.OrderType_Normal_VALUE)
                .setSecMarket(secMarket)
                .setCode(ownerOrder.getCode())
                .setQty(ownerOrder.getQuantity())
                .setPrice(ownerOrder.getPrice().doubleValue())
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