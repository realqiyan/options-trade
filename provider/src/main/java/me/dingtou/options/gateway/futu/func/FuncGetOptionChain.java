package me.dingtou.options.gateway.futu.func;

import com.alibaba.fastjson.JSON;
import com.futu.openapi.ProtoID;
import com.futu.openapi.pb.QotCommon;
import com.futu.openapi.pb.QotGetOptionChain;
import com.futu.openapi.pb.QotGetOptionExpirationDate;
import com.google.protobuf.Descriptors;
import me.dingtou.options.gateway.futu.BaseFuncExecutor;
import me.dingtou.options.gateway.futu.FunctionCall;
import me.dingtou.options.gateway.futu.ReqContext;
import me.dingtou.options.model.OptionsChain;
import me.dingtou.options.model.OptionsExpDate;

import java.util.Collections;
import java.util.List;

/**
 * 获取期权链
 *
 * @author qiyan
 */
public class FuncGetOptionChain implements FunctionCall<BaseFuncExecutor<QotGetOptionChain.Response, OptionsChain>, OptionsChain> {

    private final int market;
    private final String code;
    private final String strikeTime;

    public FuncGetOptionChain(int market, String code, String strikeTime) {
        this.market = market;
        this.code = code;
        this.strikeTime = strikeTime;
    }


    @Override
    public void call(BaseFuncExecutor<QotGetOptionChain.Response, OptionsChain> client) {

        QotCommon.Security sec = QotCommon.Security.newBuilder()
                .setMarket(market)
                .setCode(code)
                .build();


        QotGetOptionChain.DataFilter dataFilter = QotGetOptionChain.DataFilter.newBuilder()
                .build();
        
        QotGetOptionChain.C2S c2s = QotGetOptionChain.C2S.newBuilder()
                .setOwner(sec)
                .setBeginTime(strikeTime)
                .setEndTime(strikeTime)
                .setDataFilter(dataFilter)
                .build();

        QotGetOptionChain.Request req = QotGetOptionChain.Request.newBuilder().setC2S(c2s).build();
        int seqNo = client.getOptionChain(req);
        System.out.printf("Send QotGetOptionChain: %d\n", seqNo);
        if (seqNo == 0) {
            throw new RuntimeException("QotGetOptionChain error");
        }

//        client.currentReqContext.seqNo = seqNo;
//        client.currentReqContext.protoID = ProtoID.QOT_GETOPTIONCHAIN;
    }

    @Override
    public OptionsChain result(ReqContext reqContext) {

        QotGetOptionChain.Response resp = (QotGetOptionChain.Response) reqContext.resp;
        if (null == resp || null == resp.getS2C() || null == resp.getS2C().getOptionChainList()) {
            return null;
        }
        QotGetOptionChain.OptionChain optionChain = resp.getS2C().getOptionChainList().iterator().next();

        return convert(optionChain);
    }

    private OptionsChain convert(QotGetOptionChain.OptionChain resp) {
        if (resp == null) {
            return null;
        }
        String jsonString = JSON.toJSONString(resp);
        return JSON.parseObject(jsonString, OptionsChain.class);
    }

}
