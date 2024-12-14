package me.dingtou.options.gateway.futu.executor.func;

import com.alibaba.fastjson.JSON;
import com.futu.openapi.pb.QotGetSubInfo;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.gateway.futu.executor.SingleQueryExecutor;
import me.dingtou.options.gateway.futu.executor.ReqContext;

/**
 * 获取订阅信息
 *
 * @author qiyan
 */
@Slf4j
public class FuncGetSubInfo implements FunctionCall<SingleQueryExecutor<QotGetSubInfo.Response, String>, String> {


    @Override
    public void call(SingleQueryExecutor<QotGetSubInfo.Response, String> client) {

        QotGetSubInfo.C2S c2s = QotGetSubInfo.C2S.newBuilder()
                .build();
        QotGetSubInfo.Request req = QotGetSubInfo.Request.newBuilder().setC2S(c2s).build();
        int seqNo = client.getSubInfo(req);
        log.warn("Send QotGetSubInfo: {}", seqNo);
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
