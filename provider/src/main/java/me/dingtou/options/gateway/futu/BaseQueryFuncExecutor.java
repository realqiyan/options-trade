package me.dingtou.options.gateway.futu;

import com.futu.openapi.*;
import com.futu.openapi.pb.*;
import com.google.protobuf.GeneratedMessageV3;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * futu api
 *
 * @author yuanhongbo
 */
public class BaseQueryFuncExecutor<T extends GeneratedMessageV3, R> extends FTAPI_Conn_Qot implements FTSPI_Qot, FTSPI_Conn {

    public static final String FU_TU_API_IP = System.getProperty("fuTuApiIp", "10.0.12.160");
    public static final String FU_TU_API_PORT_CFG = System.getProperty("fuTuApiPort", "18888");
    public static final int FU_TU_API_PORT;
    public static final String FU_TU_API_PRIVATE_KEY;

    static {
        try {
            FU_TU_API_PORT = Integer.parseInt(FU_TU_API_PORT_CFG);

            URI uri = Objects.requireNonNull(BaseQueryFuncExecutor.class.getResource("/key/private.key")).toURI();
            byte[] buf = Files.readAllBytes(Paths.get(uri));
            FU_TU_API_PRIVATE_KEY = new String(buf, StandardCharsets.UTF_8);

            FTAPI.init();
        } catch (Exception e) {
            throw new RuntimeException("init BaseQueryFuncExecutor error", e);
        }
    }


    private final ReqContext currentReqContext = new ReqContext();
    protected FunctionCall<BaseQueryFuncExecutor<T, R>, R> call;

    public BaseQueryFuncExecutor(){

    }
    public BaseQueryFuncExecutor(FunctionCall<BaseQueryFuncExecutor<T, R>, R> call) {
        this.call = call;
    }


    /**
     * 初始化
     *
     * @return futu api
     */
    public static <T extends GeneratedMessageV3, R> R exec(FunctionCall<BaseQueryFuncExecutor<T, R>, R> call) {
        try (BaseQueryFuncExecutor<T, R> client = new BaseQueryFuncExecutor<T, R>(call)) {
            // BaseQueryFuncExecutor<T, R> client = new BaseQueryFuncExecutor<T, R>(call);
            client.setClientInfo("javaClient", 1);  //设置客户端信息
            client.setConnSpi(client);  //设置连接回调
            client.setQotSpi(client);//设置交易回调
            boolean isEnableEncrypt = false;
            if (StringUtils.isNotBlank(FU_TU_API_PRIVATE_KEY)) {
                isEnableEncrypt = true;
                client.setRSAPrivateKey(FU_TU_API_PRIVATE_KEY);
            }
            boolean connect = client.initConnect(FU_TU_API_IP, FU_TU_API_PORT, isEnableEncrypt);
            if (!connect) {
                throw new RuntimeException("initConnect fail");
            }
            try {
                synchronized (client.currentReqContext.syncEvent) {
                    client.currentReqContext.syncEvent.wait(3000);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException("call timeout");
            }
            return client.call.result(client.currentReqContext);
        }

    }


    @Override
    public void onInitConnect(FTAPI_Conn client, long errCode, String desc) {
        System.out.printf("Qot onInitConnect: ret=%b desc=%s connID=%d\n", errCode, desc, client.getConnectID());
        call.call((BaseQueryFuncExecutor) client);
    }

    @Override
    public void onDisconnect(FTAPI_Conn client, long errCode) {
        System.out.printf("Qot onDisconnect: ret=%b  connID=%d\n", errCode, client.getConnectID());
    }


    /**
     * 统一处理返回
     *
     * @param protoID  协议ID
     * @param serialNo 请求序列号
     * @param rsp      返回结果
     */
    void handleQotOnReply(int protoID, int serialNo, GeneratedMessageV3 rsp) {
        ReqContext reqContext = this.currentReqContext;
        synchronized (reqContext.syncEvent) {
            reqContext.protoID = protoID;
            reqContext.seqNo = serialNo;
            reqContext.done = true;
            reqContext.resp = rsp;
            reqContext.syncEvent.notifyAll();
        }
    }


    @Override
    public void onReply_Sub(FTAPI_Conn client, int nSerialNo, QotSub.Response rsp) {
        handleQotOnReply(ProtoID.QOT_SUB, nSerialNo, rsp);
    }


    @Override
    public void onReply_GetSubInfo(FTAPI_Conn client, int nSerialNo, QotGetSubInfo.Response rsp) {
        handleQotOnReply(ProtoID.QOT_GETSUBINFO, nSerialNo, rsp);
    }


    @Override
    public void onReply_GetBasicQot(FTAPI_Conn client, int nSerialNo, QotGetBasicQot.Response rsp) {
        handleQotOnReply(ProtoID.QOT_GETBASICQOT, nSerialNo, rsp);
    }

    @Override
    public void onReply_GetOptionChain(FTAPI_Conn client, int nSerialNo, QotGetOptionChain.Response rsp) {
        handleQotOnReply(ProtoID.QOT_GETOPTIONCHAIN, nSerialNo, rsp);
    }


    @Override
    public void onReply_GetOptionExpirationDate(FTAPI_Conn client, int nSerialNo, QotGetOptionExpirationDate.Response rsp) {
        handleQotOnReply(ProtoID.QOT_GETOPTIONEXPIRATIONDATE, nSerialNo, rsp);
    }

}