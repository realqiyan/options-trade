package me.dingtou.options.gateway.futu.executor;

import com.futu.openapi.*;
import com.futu.openapi.pb.*;
import com.google.protobuf.GeneratedMessageV3;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.gateway.futu.executor.func.FunctionCall;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.CompletableFuture;

import static me.dingtou.options.gateway.futu.executor.BaseConfig.*;

/**
 * futu api
 *
 * @author yuanhongbo
 */
@Slf4j
public class SingleQueryExecutor<R> extends FTAPI_Conn_Qot implements FTSPI_Qot, FTSPI_Conn {


    private final CompletableFuture<GeneratedMessageV3> future = new CompletableFuture<>();

    private final FunctionCall<R> call;

    public SingleQueryExecutor(FunctionCall<R> call) {
        this.call = call;
    }


    /**
     * 初始化
     *
     * @return futu api
     */
    public static <R> R query(FunctionCall<R> call) {
        try (SingleQueryExecutor<R> client = new SingleQueryExecutor<>(call)) {
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
            return client.call.result(client.future.get());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }


    @SuppressWarnings("unchecked")
    @Override
    public void onInitConnect(FTAPI_Conn client, long errCode, String desc) {
        log.warn("Qot onInitConnect: ret={} desc={} connID={}", errCode, desc, client.getConnectID());
        if (errCode == 0 && client instanceof SingleQueryExecutor<?>) {
            call.call((SingleQueryExecutor<R>) client);
        } else {
            throw new RuntimeException("onInitConnect fail");
        }
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
        this.future.complete(rsp);
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