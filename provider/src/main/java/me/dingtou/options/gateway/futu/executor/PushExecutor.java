package me.dingtou.options.gateway.futu.executor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;

import com.futu.openapi.FTAPI_Conn;
import com.futu.openapi.FTAPI_Conn_Trd;
import com.futu.openapi.FTSPI_Conn;
import com.futu.openapi.FTSPI_Trd;
import com.futu.openapi.pb.TrdSubAccPush;
import com.futu.openapi.pb.TrdSubAccPush.C2S.Builder;
import com.futu.openapi.pb.TrdUpdateOrder;
import com.futu.openapi.pb.TrdCommon.Order;
import com.futu.openapi.pb.TrdCommon.TrdHeader;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.gateway.futu.util.OrderUtils;
import me.dingtou.options.model.Owner;
import me.dingtou.options.model.OwnerOrder;

@Slf4j
public class PushExecutor extends FTAPI_Conn_Trd implements FTSPI_Trd, FTSPI_Conn {

    private final List<Owner> allOwner;
    private final Function<OwnerOrder, Void> callback;
    private final Map<String, Owner> accountOwnerMap;

    public PushExecutor(List<Owner> allOwner, Function<OwnerOrder, Void> callback) {
        if (allOwner == null || allOwner.isEmpty()) {
            throw new IllegalArgumentException("allOwner is null or empty");
        }
        this.allOwner = allOwner;
        this.callback = callback;

        this.accountOwnerMap = new HashMap<>();
        for (Owner owner : allOwner) {
            accountOwnerMap.put(owner.getAccount().getAccountId(), owner);
        }

    }

    /**
     * 初始化
     *
     * @return futu api
     */
    public static void submit(List<Owner> allOwner, Function<OwnerOrder, Void> callback) {
        try (PushExecutor client = new PushExecutor(allOwner, callback)) {
            client.setClientInfo("javaClient", 1); // 设置客户端信息
            client.setConnSpi(client); // 设置连接回调
            client.setTrdSpi(client);// 设置交易回调
            boolean isEnableEncrypt = false;
            if (StringUtils.isNotBlank(BaseConfig.FU_TU_API_PRIVATE_KEY)) {
                isEnableEncrypt = true;
                client.setRSAPrivateKey(BaseConfig.FU_TU_API_PRIVATE_KEY);
            }
            boolean connect = client.initConnect(BaseConfig.FU_TU_API_IP, BaseConfig.FU_TU_API_PORT, isEnableEncrypt);
            if (!connect) {
                throw new RuntimeException("initConnect fail");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onInitConnect(FTAPI_Conn client, long errCode, String desc) {
        log.warn("Qot onInitConnect: ret={} desc={} connID={}", errCode, desc, client.getConnectID());
        PushExecutor conn = (PushExecutor) client;
        Builder builder = TrdSubAccPush.C2S.newBuilder();
        for (Owner owner : allOwner) {
            builder.addAccIDList(Long.parseLong(owner.getAccount().getAccountId()));
        }
        TrdSubAccPush.C2S c2s = builder.build();
        TrdSubAccPush.Request req = TrdSubAccPush.Request.newBuilder().setC2S(c2s).build();
        int seqNo = conn.subAccPush(req);
        log.warn("Send TrdSubAccPush: {}", seqNo);
    }

    @Override
    public void onReply_SubAccPush(FTAPI_Conn client, int nSerialNo, TrdSubAccPush.Response rsp) {
        log.warn("onReply_SubAccPush: {}", rsp);
    }

    @Override
    public void onPush_UpdateOrder(FTAPI_Conn client, TrdUpdateOrder.Response rsp) {
        log.warn("onPush_UpdateOrder: {}", rsp.getErrCode());
        if (rsp.getErrCode() == 0) {
            TrdHeader header = rsp.getS2C().getHeader();
            Order order = rsp.getS2C().getOrder();
            long accID = header.getAccID();
            Owner owner = accountOwnerMap.get(String.valueOf(accID));
            if (owner == null) {
                log.warn("owner is null, accID: {}", accID);
                return;
            }
            callback.apply(OrderUtils.convertOwnerOrder(order, owner));
        }
    }
}