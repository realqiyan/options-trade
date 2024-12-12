package me.dingtou.options.gateway.futu.func;

import com.alibaba.fastjson.JSON;
import com.futu.openapi.pb.QotCommon;
import com.futu.openapi.pb.QotSub;
import me.dingtou.options.gateway.futu.BaseQueryFuncExecutor;
import me.dingtou.options.gateway.futu.FunctionCall;
import me.dingtou.options.gateway.futu.ReqContext;

/**
 * 订阅
 *
 * @author qiyan
 */
public class FuncSub implements FunctionCall<BaseQueryFuncExecutor<QotSub.Response, String>, String> {

    private final int market;
    private final String code;
    private final boolean subOrUnSub;

    public FuncSub(int market, String code, boolean subOrUnSub) {
        this.market = market;
        this.code = code;
        this.subOrUnSub = subOrUnSub;
    }

    @Override
    public void call(BaseQueryFuncExecutor<QotSub.Response, String> client) {
        QotCommon.Security sec = QotCommon.Security.newBuilder()
                .setMarket(market)
                .setCode(code)
                .build();
        QotSub.C2S c2s = QotSub.C2S.newBuilder()
                .addSecurityList(sec)
                .addSubTypeList(QotCommon.SubType.SubType_Basic_VALUE)
                .setIsSubOrUnSub(subOrUnSub)
                .build();
        QotSub.Request req = QotSub.Request.newBuilder().setC2S(c2s).build();
        int seqNo = client.sub(req);
        System.out.printf("Send QotSub: %d\n", seqNo);
    }

    @Override
    public String result(ReqContext reqContext) {

        QotSub.Response resp = (QotSub.Response) reqContext.resp;
        if (null == resp) {
            return null;
        }
        return JSON.toJSONString(resp);
    }


}
