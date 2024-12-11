package me.dingtou.options.gateway.futu.func;

import com.alibaba.fastjson.JSON;
import com.futu.openapi.pb.QotCommon;
import com.futu.openapi.pb.QotGetBasicQot;
import me.dingtou.options.gateway.futu.BaseFuncExecutor;
import me.dingtou.options.gateway.futu.FunctionCall;
import me.dingtou.options.gateway.futu.ReqContext;

/**
 * 获取基础行情
 *
 * @author qiyan
 */
public class FuncGetBasicQot implements FunctionCall<BaseFuncExecutor<QotGetBasicQot.Response, String>, String> {

    private final int market;
    private final String code;

    public FuncGetBasicQot(int market, String code) {
        this.market = market;
        this.code = code;
    }

    @Override
    public void call(BaseFuncExecutor<QotGetBasicQot.Response, String> client) {
        QotCommon.Security sec = QotCommon.Security.newBuilder()
                .setMarket(market)
                .setCode(code)
                .build();
        QotGetBasicQot.C2S c2s = QotGetBasicQot.C2S.newBuilder()
                .addSecurityList(sec)
                .build();
        QotGetBasicQot.Request req = QotGetBasicQot.Request.newBuilder().setC2S(c2s).build();
        int seqNo = client.getBasicQot(req);
        System.out.printf("Send QotGetBasicQot: %d\n", seqNo);
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
