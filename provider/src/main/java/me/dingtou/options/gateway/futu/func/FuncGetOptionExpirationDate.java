package me.dingtou.options.gateway.futu.func;

import com.alibaba.fastjson.JSON;
import com.futu.openapi.pb.QotCommon;
import com.futu.openapi.pb.QotGetOptionExpirationDate;
import me.dingtou.options.gateway.futu.BaseFuncExecutor;
import me.dingtou.options.gateway.futu.FunctionCall;
import me.dingtou.options.gateway.futu.ReqContext;
import me.dingtou.options.model.OptionsExpDate;

import java.util.Collections;
import java.util.List;

/**
 * 获取期权到期日链
 *
 * @author qiyan
 */
public class FuncGetOptionExpirationDate implements FunctionCall<BaseFuncExecutor<QotGetOptionExpirationDate.Response, List<OptionsExpDate>>, List<OptionsExpDate>> {

    private final int market;
    private final String code;

    public FuncGetOptionExpirationDate(int market, String code) {
        this.market = market;
        this.code = code;
    }


    @Override
    public void call(BaseFuncExecutor<QotGetOptionExpirationDate.Response, List<OptionsExpDate>> client) {

        QotCommon.Security sec = QotCommon.Security.newBuilder()
                .setMarket(market)
                .setCode(code)
                .build();
        QotGetOptionExpirationDate.C2S c2s = QotGetOptionExpirationDate.C2S.newBuilder()
                .setOwner(sec)
                .build();
        QotGetOptionExpirationDate.Request req = QotGetOptionExpirationDate.Request.newBuilder().setC2S(c2s).build();
        int seqNo = client.getOptionExpirationDate(req);
        System.out.printf("Send QotGetOptionExpirationDate: %d\n", seqNo);
        if (seqNo == 0) {
            throw new RuntimeException("QotGetOptionExpirationDate error");
        }

    }

    @Override
    public List<OptionsExpDate> result(ReqContext reqContext) {

        QotGetOptionExpirationDate.Response resp = (QotGetOptionExpirationDate.Response) reqContext.resp;
        if (null == resp || 0 != resp.getRetType()) {
            return Collections.emptyList();
        }

        return convert(resp.getS2C().getDateListList());
    }

    private List<OptionsExpDate> convert(List<QotGetOptionExpirationDate.OptionExpirationDate> dateLis) {
        if (dateLis == null) {
            return Collections.emptyList();
        }
        String jsonString = JSON.toJSONString(dateLis);
        return JSON.parseArray(jsonString, OptionsExpDate.class);
    }

}
