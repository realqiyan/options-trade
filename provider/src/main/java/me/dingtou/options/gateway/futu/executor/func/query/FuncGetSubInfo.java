package me.dingtou.options.gateway.futu.executor.func.query;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.futu.openapi.pb.QotGetSubInfo;
import com.futu.openapi.pb.QotCommon;
import com.futu.openapi.pb.QotCommon.ConnSubInfo;
import com.google.protobuf.GeneratedMessageV3;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.gateway.futu.executor.QueryExecutor;
import me.dingtou.options.gateway.futu.executor.func.QueryFunctionCall;
import me.dingtou.options.model.Security;

@Slf4j
public class FuncGetSubInfo implements QueryFunctionCall<FuncGetSubInfo.SubInfo> {

    @Override
    public int call(QueryExecutor client) {
        QotGetSubInfo.C2S c2s = QotGetSubInfo.C2S.newBuilder().build();
        QotGetSubInfo.Request req = QotGetSubInfo.Request.newBuilder().setC2S(c2s).build();
        int seqNo = client.getSubInfo(req);
        log.debug("Send QotGetSubInfo: {}", seqNo);
        return seqNo;
    }

    @Override
    public FuncGetSubInfo.SubInfo result(GeneratedMessageV3 response) {
        Map<String, Security> target = new HashMap<>();

        QotGetSubInfo.Response resp = (QotGetSubInfo.Response) response;

        QotGetSubInfo.S2C s2c = resp.getS2C();
        log.debug("FuncGetSubInfo -> RemainQuota: {} TotalUsedQuota: {}", s2c.getRemainQuota(), s2c.getTotalUsedQuota());

        List<ConnSubInfo> connSubInfoList = s2c.getConnSubInfoListList();
        for (ConnSubInfo connSubInfo : connSubInfoList) {
            List<QotCommon.SubInfo> subInfoList = connSubInfo.getSubInfoListList();
            for (QotCommon.SubInfo subInfo : subInfoList) {
                List<QotCommon.Security> securityList = subInfo.getSecurityListList();
                for (QotCommon.Security security : securityList) {
                    Security securityObj = Security.of(security.getCode(), security.getMarket());
                    target.put(subInfo.getSubType() + "_" + securityObj.toString(), securityObj);
                }
            }
        }
        return new SubInfo(s2c.getRemainQuota(), s2c.getTotalUsedQuota(), target);
    }

    @Getter
    public static class SubInfo {
        // 剩余配额 
        private final int remainQuota;
        // 已使用配额
        private final int totalUsedQuota;
        // 订阅信息
        private final Map<String, Security> subSecurityMap;

        public SubInfo(int remainQuota, int totalUsedQuota, Map<String, Security> subSecurityMap) {
            this.remainQuota = remainQuota;
            this.totalUsedQuota = totalUsedQuota;
            this.subSecurityMap = subSecurityMap;
        }
    }
}
