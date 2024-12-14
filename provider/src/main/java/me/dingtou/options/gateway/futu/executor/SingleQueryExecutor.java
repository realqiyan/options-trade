package me.dingtou.options.gateway.futu.executor;

import com.futu.openapi.*;
import com.futu.openapi.pb.*;
import com.google.protobuf.GeneratedMessageV3;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.gateway.futu.executor.func.FunctionCall;
import org.apache.commons.lang3.StringUtils;

import static me.dingtou.options.gateway.futu.executor.BaseConfig.*;

/**
 * futu api
 *
 * @author yuanhongbo
 */
@Slf4j
public class SingleQueryExecutor<T extends GeneratedMessageV3, R> extends FTAPI_Conn_Qot implements FTSPI_Qot, FTSPI_Conn {


    private final ReqContext currentReqContext = new ReqContext();
    protected FunctionCall<SingleQueryExecutor<T, R>, R> call;

    public SingleQueryExecutor(FunctionCall<SingleQueryExecutor<T, R>, R> call) {
        this.call = call;
    }


    /**
     * 初始化
     *
     * @return futu api
     */
    public static <T extends GeneratedMessageV3, R> R query(FunctionCall<SingleQueryExecutor<T, R>, R> call) {
        try (SingleQueryExecutor<T, R> client = new SingleQueryExecutor<T, R>(call)) {
            // SingleQueryExecutor<T, R> client = new SingleQueryExecutor<T, R>(call);
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
        log.warn("Qot onInitConnect: ret={} desc={} connID={}", errCode, desc, client.getConnectID());
        call.call((SingleQueryExecutor) client);
    }

    @Override
    public void onDisconnect(FTAPI_Conn client, long errCode) {
        log.warn("Qot onDisconnect: ret={}  connID={}", errCode, client.getConnectID());
    }


    /**
     * 统一处理返回
     *
     * @param rsp 返回结果
     */
    void handleQotOnReply(GeneratedMessageV3 rsp) {
        ReqContext reqContext = this.currentReqContext;
        synchronized (reqContext.syncEvent) {
            reqContext.resp = rsp;
            reqContext.syncEvent.notifyAll();
        }
    }


    @Override
    public void onReply_Sub(FTAPI_Conn client, int nSerialNo, QotSub.Response rsp) {
        handleQotOnReply(rsp);
    }


    @Override
    public void onReply_GetSubInfo(FTAPI_Conn client, int nSerialNo, QotGetSubInfo.Response rsp) {
        handleQotOnReply(rsp);
    }


    @Override
    public void onReply_GetBasicQot(FTAPI_Conn client, int nSerialNo, QotGetBasicQot.Response rsp) {
        handleQotOnReply(rsp);
    }

    @Override
    public void onReply_GetOptionChain(FTAPI_Conn client, int nSerialNo, QotGetOptionChain.Response rsp) {
        handleQotOnReply(rsp);
    }


    @Override
    public void onReply_GetOptionExpirationDate(FTAPI_Conn client, int nSerialNo, QotGetOptionExpirationDate.Response rsp) {
        handleQotOnReply(rsp);
    }

}