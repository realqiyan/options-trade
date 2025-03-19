package me.dingtou.options.gateway.futu.executor;

import java.util.List;
import java.util.Stack;

import org.apache.commons.lang3.StringUtils;

import com.futu.openapi.FTAPI_Conn;
import com.futu.openapi.FTAPI_Conn_Qot;
import com.futu.openapi.FTSPI_Conn;
import com.futu.openapi.FTSPI_Qot;

import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.model.Security;

@Slf4j
public class OptionsPushExecutor extends FTAPI_Conn_Qot implements FTSPI_Qot, FTSPI_Conn {

    private static OptionsPushExecutor CLIENT = new OptionsPushExecutor();

    // size 500 的一个栈
    private final Stack<Security> allOptions = new Stack<>();

    private OptionsPushExecutor() {
        this.setClientInfo("javaClient", 1); // 设置客户端信息
        this.setConnSpi(this); // 设置连接回调
        this.setQotSpi(this);// 设置回调
        boolean isEnableEncrypt = false;
        if (StringUtils.isNotBlank(BaseConfig.FU_TU_API_PRIVATE_KEY)) {
            isEnableEncrypt = true;
            this.setRSAPrivateKey(BaseConfig.FU_TU_API_PRIVATE_KEY);
        }
        boolean connect = this.initConnect(BaseConfig.FU_TU_API_IP, BaseConfig.FU_TU_API_PORT, isEnableEncrypt);
        if (!connect) {
            throw new RuntimeException("initConnect fail");
        }
    }

    /**
     * 监听
     * 
     * @param securityList
     */
    public static void listen(List<Security> securityList) {
        CLIENT.allOptions.addAll(securityList);
    }

    @Override
    public void onInitConnect(FTAPI_Conn client, long errCode, String desc) {
        log.warn("Qot onInitConnect: ret={} desc={} connID={}", errCode, desc, client.getConnectID());
    }

}