package me.dingtou.options.gateway.futu.executor;

import com.futu.openapi.FTAPI_Conn;
import com.futu.openapi.FTAPI_Conn_Qot;
import com.futu.openapi.FTSPI_Conn;
import com.futu.openapi.FTSPI_Qot;
import com.futu.openapi.pb.*;
import com.google.protobuf.GeneratedMessageV3;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.gateway.futu.executor.func.QueryFunctionCall;
import me.dingtou.options.model.Security;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import static me.dingtou.options.gateway.futu.executor.BaseConfig.*;

/**
 * futu api
 *
 * @author yuanhongbo
 */
@Slf4j
public class QueryExecutor extends FTAPI_Conn_Qot implements FTSPI_Qot, FTSPI_Conn {

    /**
     * 请求链接和上下文关联
     */
    private static final Map<Integer, QueryContext> CONTEXT = new ConcurrentHashMap<>();

    /**
     * 订阅的股票
     */
    private static final Map<String, Security> SUB_SECURITY = new ConcurrentHashMap<>();

    /**
     * 单例
     */
    private static final QueryExecutor INSTANCE = new QueryExecutor();

    /**
     * 是否连接
     */
    private boolean isConnected = false;

    private QueryExecutor() {
        // 设置客户端信息
        this.setClientInfo("javaClient", 1);
        // 设置连接回调
        this.setConnSpi(this);
        // 设置回调
        this.setQotSpi(this);
        // 初始化连接
        initConnect();
    }

    /**
     * 初始化连接
     * 
     * @return
     */
    private boolean initConnect() {
        // 清空订阅的股票
        SUB_SECURITY.clear();
        // 清空请求上下文
        CONTEXT.clear();

        boolean isEnableEncrypt = false;
        if (StringUtils.isNotBlank(FU_TU_API_PRIVATE_KEY)) {
            isEnableEncrypt = true;
            this.setRSAPrivateKey(FU_TU_API_PRIVATE_KEY);
        }
        boolean connect = this.initConnect(FU_TU_API_IP, FU_TU_API_PORT, isEnableEncrypt);
        if (!connect) {
            throw new RuntimeException("initConnect fail");
        }
        return connect;
    }

    /**
     * 是否连接
     * 
     * @return
     */
    public boolean isConnected() {
        return isConnected;
    }

    /**
     * 设置是否连接
     * 
     * @param connected
     */
    public void setConnected(boolean connected) {
        this.isConnected = connected;
        // 如果连接断开，重新连接
        if (!connected) {
            initConnect();
        }
    }

    /**
     * 获取单例
     * 
     * @return
     */
    public static QueryExecutor getInstance() {
        return INSTANCE;
    }

    /**
     * 获取订阅的股票
     * 
     * @return
     */
    public static Map<String, Security> getSubSecurity() {
        return SUB_SECURITY;
    }

    /**
     * 查询服务
     *
     * @return 最终结果
     */
    @SuppressWarnings("unchecked")
    public static <R> R query(QueryFunctionCall<R> call) {
        QueryExecutor client = getInstance();
        if (!client.isConnected()) {
            try {
                // 等待创建连接
                Thread.sleep(300);
            } catch (InterruptedException e) {
                log.error("QueryExecutor not connected", e);
            }
            if (!client.isConnected()) {
                throw new RuntimeException("QueryExecutor not connected");
            }
        }

        // 当前请求创建上下文
        QueryContext ctx = new QueryContext(call);

        try {
            List<Integer> subTypeList = call.getSubTypeList();
            List<Security> subSecurityList = getUnSubSecurityList(subTypeList, call.getSubSecurityList());
            if (subSecurityList.isEmpty()) {
                // 无需订阅的接口直接请求
                int seqNo = ctx.callback.call(client);
                // 记录上下文
                CONTEXT.put(seqNo, ctx);
            } else {
                // 需要订阅的接口
                QotSub.C2S.Builder builder = QotSub.C2S.newBuilder();
                for (Security security : subSecurityList) {
                    if (null == security) {
                        continue;
                    }
                    QotCommon.Security sec = QotCommon.Security.newBuilder()
                            .setMarket(security.getMarket())
                            .setCode(security.getCode())
                            .build();
                    builder.addSecurityList(sec);
                }

                for (Integer subType : subTypeList) {
                    builder.addSubTypeList(subType);
                }
                QotSub.C2S c2s = builder.setIsSubOrUnSub(true).build();
                QotSub.Request req = QotSub.Request.newBuilder().setC2S(c2s).build();
                int seqNo = client.sub(req);
                // 记录上下文
                CONTEXT.put(seqNo, ctx);
                log.warn("Send QotSub: {}", seqNo);
            }

            return (R) ctx.callback.result(ctx.future.get());
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取未订阅的股票
     * 
     * @param subTypeList
     * @param subSecurityList
     * @return
     */
    private static List<Security> getUnSubSecurityList(List<Integer> subTypeList, List<Security> subSecurityList) {
        if (subSecurityList == null || subSecurityList.isEmpty()) {
            return Collections.emptyList();
        }

        List<Security> unSubSecurityList = new ArrayList<>();
        for (Security security : subSecurityList) {
            List<String> subKeyList = new ArrayList<>();
            for (Integer subType : subTypeList) {
                String key = subType + "_" + security.toString();
                subKeyList.add(key);
            }
            for (String subKey : subKeyList) {
                if (!SUB_SECURITY.containsKey(subKey)) {
                    unSubSecurityList.add(security);
                    break;
                }
            }
        }

        return unSubSecurityList;
    }

    @Override
    public void onInitConnect(FTAPI_Conn client, long errCode, String desc) {
        log.warn("Qot onInitConnect: ret={} desc={} connID={}", errCode, desc, client.getConnectID());
        QueryExecutor conn = (QueryExecutor) client;
        conn.setConnected(true);
    }

    @Override
    public void onDisconnect(FTAPI_Conn client, long errCode) {
        log.warn("Qot onDisconnect: ret={}  connID={}", errCode, client.getConnectID());
        QueryExecutor conn = (QueryExecutor) client;
        conn.setConnected(false);
    }

    @Override
    public void onReply_Sub(FTAPI_Conn conn, int nSerialNo, QotSub.Response rsp) {
        log.warn("onReply_Sub: QotSub={} RetType={} RetMsg={}", nSerialNo, rsp.getRetType(), rsp.getRetMsg());

        QueryExecutor client = (QueryExecutor) conn;
        QueryContext ctx = CONTEXT.remove(nSerialNo);

        if (rsp.getRetType() == 0) {
            List<Security> subSecurityList = ctx.callback.getSubSecurityList();
            if (null != subSecurityList && !subSecurityList.isEmpty()) {
                List<Integer> subTypeList = ctx.callback.getSubTypeList();
                for (Security security : subSecurityList) {
                    for (Integer subType : subTypeList) {
                        SUB_SECURITY.put(subType + "_" + security.toString(), security);
                    }
                }
            }

        }

        if (ctx != null) {
            if (ctx.callback.isContinue()) {
                int newSerialNo = ctx.callback.call(client);
                CONTEXT.put(newSerialNo, ctx);
            } else {
                ctx.future.complete(rsp);
            }
        }
    }

    /**
     * 统一处理返回
     * 
     * @param nSerialNo
     * @param rsp       返回结果
     */
    void handleQotOnReply(int nSerialNo, GeneratedMessageV3 rsp) {
        QueryContext ctx = CONTEXT.remove(nSerialNo);
        if (ctx != null) {
            log.warn("handleQotOnReply: nSerialNo={}", nSerialNo);
            ctx.future.complete(rsp);
        }
    }

    @Override
    public void onReply_GetSubInfo(FTAPI_Conn client, int nSerialNo, QotGetSubInfo.Response rsp) {
        log.warn("onReply_GetSubInfo: nSerialNo={} RetType={} RetMsg={}", nSerialNo, rsp.getRetType(), rsp.getRetMsg());
        handleQotOnReply(nSerialNo, rsp);
    }

    @Override
    public void onReply_GetBasicQot(FTAPI_Conn client, int nSerialNo, QotGetBasicQot.Response rsp) {
        log.warn("onReply_GetBasicQot: nSerialNo={} RetType={} RetMsg={}", nSerialNo, rsp.getRetType(), rsp.getRetMsg());
        handleQotOnReply(nSerialNo, rsp);
    }

    @Override
    public void onReply_GetOptionChain(FTAPI_Conn client, int nSerialNo, QotGetOptionChain.Response rsp) {
        log.warn("onReply_GetOptionChain: nSerialNo={} RetType={} RetMsg={}", nSerialNo, rsp.getRetType(), rsp.getRetMsg());
        handleQotOnReply(nSerialNo, rsp);
    }

    @Override
    public void onReply_GetOptionExpirationDate(FTAPI_Conn client, int nSerialNo,
            QotGetOptionExpirationDate.Response rsp) {
        log.warn("onReply_GetOptionExpirationDate: nSerialNo={} RetType={} RetMsg={}", nSerialNo, rsp.getRetType(), rsp.getRetMsg());
        handleQotOnReply(nSerialNo, rsp);
    }

    @Override
    public void onReply_GetOrderBook(FTAPI_Conn client, int nSerialNo, QotGetOrderBook.Response rsp) {
        log.warn("onReply_GetOrderBook: nSerialNo={} RetType={} RetMsg={}", nSerialNo, rsp.getRetType(), rsp.getRetMsg());
        handleQotOnReply(nSerialNo, rsp);
    }

    @Getter
    private static class QueryContext {
        private final CompletableFuture<GeneratedMessageV3> future = new CompletableFuture<>();
        private final QueryFunctionCall<?> callback;

        public QueryContext(QueryFunctionCall<?> callback) {
            this.callback = callback;
        }

    }

}