package me.dingtou.options.gateway.futu.executor.func;

import com.alibaba.fastjson.JSON;
import com.futu.openapi.pb.QotCommon;
import com.futu.openapi.pb.QotGetBasicQot;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.gateway.futu.executor.SingleQueryExecutor;
import me.dingtou.options.gateway.futu.executor.ReqContext;

/**
 * 获取基础行情
 *
 * @author qiyan
 */
@Slf4j
public class FuncGetBasicQot implements FunctionCall<SingleQueryExecutor<QotGetBasicQot.Response, String>, String> {

    private final int market;
    private final String code;

    public FuncGetBasicQot(int market, String code) {
        this.market = market;
        this.code = code;
    }

    @Override
    public void call(SingleQueryExecutor<QotGetBasicQot.Response, String> client) {
        QotCommon.Security sec = QotCommon.Security.newBuilder()
                .setMarket(market)
                .setCode(code)
                .build();
        QotGetBasicQot.C2S c2s = QotGetBasicQot.C2S.newBuilder()
                .addSecurityList(sec)
                .build();
        QotGetBasicQot.Request req = QotGetBasicQot.Request.newBuilder().setC2S(c2s).build();
        int seqNo = client.getBasicQot(req);
        log.warn("Send QotGetBasicQot: {}", seqNo);
    }

    @Override
    public String result(ReqContext reqContext) {

        QotGetBasicQot.Response resp = (QotGetBasicQot.Response) reqContext.resp;
        if (null == resp) {
            return null;
        }
        return JSON.toJSONString(resp);
    }


}
