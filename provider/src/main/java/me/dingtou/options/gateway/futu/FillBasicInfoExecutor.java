package me.dingtou.options.gateway.futu;

import com.futu.openapi.*;
import com.futu.openapi.pb.*;
import com.google.protobuf.GeneratedMessageV3;
import me.dingtou.options.model.OptionsRealtimeData;
import me.dingtou.options.model.Security;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * futu api
 *
 * @author yuanhongbo
 */
public class FillBasicInfoExecutor extends BaseFuncExecutor<QotGetBasicQot.Response, List<OptionsRealtimeData>> {

    private final Object syncEvent = new Object();

    private GeneratedMessageV3 resp;

    private final Set<Security> allSecurity;

    public FillBasicInfoExecutor(Set<Security> allSecurity) {
        this.allSecurity = allSecurity;
    }


    /**
     * 初始化
     *
     * @return futu api
     */
    public static List<OptionsRealtimeData> fill(Set<Security> allSecurity) {
        try (FillBasicInfoExecutor client = new FillBasicInfoExecutor(allSecurity)) {
            client.setClientInfo("javaClient", 1);  //设置客户端信息
            client.setConnSpi(client);  //设置连接回调
            client.setQotSpi(client);//设置交易回调
            client.initConnect(BaseFuncExecutor.FU_TU_API_IP, BaseFuncExecutor.FU_TU_API_PORT, false);

            try {
                synchronized (client.syncEvent) {
                    client.syncEvent.wait(3000);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException("call timeout");
            }

            System.out.print("Reply: syncEvent:notifyAll\n");

            QotGetBasicQot.Response resp = (QotGetBasicQot.Response) client.resp;

            if (null == resp || null == resp.getS2C() || null == resp.getS2C().getBasicQotListList()) {
                return Collections.emptyList();
            }
            List<OptionsRealtimeData> result = new ArrayList<>();

            List<QotCommon.BasicQot> basicQotList = resp.getS2C().getBasicQotListList();
            for (QotCommon.BasicQot basicQot : basicQotList) {
                QotCommon.OptionBasicQotExData optionExData = basicQot.getOptionExData();
                OptionsRealtimeData data = convertOptionsRealtimeData(basicQot, optionExData);
                result.add(data);
            }
            return result;
        }
    }

    private static OptionsRealtimeData convertOptionsRealtimeData(QotCommon.BasicQot basicQot, QotCommon.OptionBasicQotExData optionExData) {
        OptionsRealtimeData data = new OptionsRealtimeData();
        Security security = new Security();
        security.setMarket(basicQot.getSecurity().getMarket());
        security.setCode(basicQot.getSecurity().getCode());
        data.setSecurity(security);
        data.setDelta(optionExData.getDelta());
        data.setGamma(optionExData.getGamma());
        data.setTheta(optionExData.getTheta());
        data.setVega(optionExData.getVega());
        data.setRho(optionExData.getRho());
        data.setImpliedVolatility(optionExData.getImpliedVolatility());
        data.setPremium(optionExData.getPremium());
        data.setCurPrice(basicQot.getCurPrice());
        return data;
    }


    @Override
    public void onInitConnect(FTAPI_Conn client, long errCode, String desc) {
        System.out.printf("Qot onInitConnect: ret=%b desc=%s connID=%d\n", errCode, desc, client.getConnectID());

        QotSub.C2S.Builder builder = QotSub.C2S.newBuilder();
        for (Security security : allSecurity) {
            if (null == security) {
                continue;
            }
            QotCommon.Security sec = QotCommon.Security.newBuilder().setMarket(security.getMarket()).setCode(security.getCode()).build();
            builder.addSecurityList(sec);
        }
        QotSub.C2S c2s = builder.addSubTypeList(QotCommon.SubType.SubType_Basic_VALUE).setIsSubOrUnSub(true).build();
        QotSub.Request req = QotSub.Request.newBuilder().setC2S(c2s).build();
        FillBasicInfoExecutor conn = (FillBasicInfoExecutor) client;
        int seqNo = conn.sub(req);
        System.out.printf("Send QotSub: %d\n", seqNo);
    }


    @Override
    public void onReply_Sub(FTAPI_Conn client, int nSerialNo, QotSub.Response rsp) {
        System.out.printf("Reply: QotSub: %d RetType: %d\n", nSerialNo, rsp.getRetType());

        if (rsp.getRetType() != Common.RetType.RetType_Succeed_VALUE) {
            return;
        }

        QotGetBasicQot.C2S.Builder builder = QotGetBasicQot.C2S.newBuilder();
        for (Security security : allSecurity) {
            if (null == security) {
                continue;
            }
            QotCommon.Security sec = QotCommon.Security.newBuilder().setMarket(security.getMarket()).setCode(security.getCode()).build();
            builder.addSecurityList(sec);
        }
        QotGetBasicQot.C2S c2s = builder.build();
        QotGetBasicQot.Request req = QotGetBasicQot.Request.newBuilder().setC2S(c2s).build();
        FillBasicInfoExecutor conn = (FillBasicInfoExecutor) client;
        int seqNo = conn.getBasicQot(req);
        System.out.printf("Send QotGetBasicQot: %d\n", seqNo);
    }

    @Override
    public void onReply_GetBasicQot(FTAPI_Conn client, int nSerialNo, QotGetBasicQot.Response rsp) {
        System.out.printf("Reply: GetBasicQot: %d RetType: %d\n", nSerialNo, rsp.getRetType());
        FillBasicInfoExecutor conn = (FillBasicInfoExecutor) client;
        conn.resp = rsp;
        synchronized (conn.syncEvent) {
            conn.syncEvent.notifyAll();
        }
    }


}