package me.dingtou.options.gateway.futu.func;

import com.alibaba.fastjson.JSON;
import com.futu.openapi.ProtoID;
import com.futu.openapi.pb.QotGetSubInfo;
import me.dingtou.options.gateway.futu.BaseFuncExecutor;
import me.dingtou.options.gateway.futu.FunctionCall;
import me.dingtou.options.gateway.futu.ReqContext;

/**
 * 获取订阅信息
 *
 * @author qiyan
 */
public class FuncGetSubInfo implements FunctionCall<BaseFuncExecutor<QotGetSubInfo.Response, String>, String> {


    @Override
    public void call(BaseFuncExecutor<QotGetSubInfo.Response, String> client) {

        QotGetSubInfo.C2S c2s = QotGetSubInfo.C2S.newBuilder()
                .build();
        QotGetSubInfo.Request req = QotGetSubInfo.Request.newBuilder().setC2S(c2s).build();
        int seqNo = client.getSubInfo(req);
        System.out.printf("Send QotGetSubInfo: %d\n", seqNo);
//        client.currentReqContext.seqNo = seqNo;
//        client.currentReqContext.protoID = ProtoID.QOT_GETOPTIONCHAIN;
    }

    @Override
    public String result(ReqContext reqContext) {

        QotGetSubInfo.Response resp = (QotGetSubInfo.Response) reqContext.resp;
        if (null == resp) {
            return null;
        }
        return JSON.toJSONString(resp.getS2C());
    }


}
