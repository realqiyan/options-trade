package me.dingtou.options.help;

import com.alibaba.fastjson2.JSON;
import com.futu.openapi.*;
import com.futu.openapi.pb.TrdCommon;
import com.futu.openapi.pb.TrdGetAccList;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.config.ConfigUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 获取交易账户列表
 */
@Slf4j
public class TrdGetAccListHelper implements FTSPI_Trd, FTSPI_Conn {
    FTAPI_Conn_Trd trd = new FTAPI_Conn_Trd();

    public TrdGetAccListHelper() {
        trd.setClientInfo("javaclient", 1);  //设置客户端信息
        trd.setConnSpi(this);  //设置连接回调
        trd.setTrdSpi(this);   //设置交易回调

        try {
            String rasPrivateKey = ConfigUtils.getConfigDir() + "futu_rsa_private.key";
            Path rasPrivateKeyPath = Paths.get(rasPrivateKey);
            if (!Files.exists(rasPrivateKeyPath)) {
                throw new RuntimeException(rasPrivateKey + " not exists");
            }
            byte[] buf = Files.readAllBytes(rasPrivateKeyPath);
            String key = new String(buf, StandardCharsets.UTF_8);

            trd.setRSAPrivateKey(key);
        } catch (Exception e) {
            log.error("setRSAPrivateKey error.", e);
        }

    }

    public void start() {
        trd.initConnect("10.0.12.160", (short) 18888, true);
    }

    @Override
    public void onInitConnect(FTAPI_Conn client, long errCode, String desc) {
        log.info("Trd onInitConnect: ret={} desc={} connID={}", errCode, desc, client.getConnectID());
        if (errCode != 0)
            return;

        TrdGetAccList.C2S c2s = TrdGetAccList.C2S.newBuilder().setUserID(900053)
                .setTrdCategory(TrdCommon.TrdCategory.TrdCategory_Security_VALUE)
                .setNeedGeneralSecAccount(true)
                .build();
        TrdGetAccList.Request req = TrdGetAccList.Request.newBuilder().setC2S(c2s).build();
        int seqNo = trd.getAccList(req);
        log.info("Send TrdGetAccList: {}", seqNo);
    }

    @Override
    public void onDisconnect(FTAPI_Conn client, long errCode) {
        log.info("Trd onDisConnect: {}", errCode);
    }

    @Override
    public void onReply_GetAccList(FTAPI_Conn client, int nSerialNo, TrdGetAccList.Response rsp) {
        if (rsp.getRetType() != 0) {
            log.warn("TrdGetAccList failed: {}", rsp.getRetMsg());
        } else {
            String json = JSON.toJSONString(rsp);
            log.info("Receive TrdGetAccList: {}", json);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        FTAPI.init();
        TrdGetAccListHelper trd = new TrdGetAccListHelper();
        trd.start();

        Thread.sleep(1000 * 6);

    }
}