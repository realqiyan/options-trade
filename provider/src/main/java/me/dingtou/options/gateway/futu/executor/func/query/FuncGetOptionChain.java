package me.dingtou.options.gateway.futu.executor.func.query;

import com.alibaba.fastjson.JSON;
import com.futu.openapi.pb.QotCommon;
import com.futu.openapi.pb.QotGetOptionChain;
import com.google.protobuf.GeneratedMessageV3;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.gateway.futu.executor.QueryExecutor;
import me.dingtou.options.gateway.futu.executor.func.QueryFunctionCall;
import me.dingtou.options.model.Options;
import me.dingtou.options.model.OptionsChain;
import me.dingtou.options.model.OptionsTuple;
import me.dingtou.options.model.Security;

import java.util.Collections;
import java.util.List;

/**
 * 获取期权链
 *
 * @author qiyan
 */
@Slf4j
public class FuncGetOptionChain implements QueryFunctionCall<OptionsChain> {

    private final int market;
    private final String code;
    private final String strikeTime;

    public FuncGetOptionChain(int market, String code, String strikeTime) {
        this.market = market;
        this.code = code;
        this.strikeTime = strikeTime;
    }


    @Override
    public List<Security> getSubSecurityList() {
        return Collections.emptyList();
    }

    @Override
    public void call(QueryExecutor<OptionsChain> client) {

        QotCommon.Security sec = QotCommon.Security.newBuilder()
                .setMarket(market)
                .setCode(code)
                .build();


        QotGetOptionChain.DataFilter.Builder builder = QotGetOptionChain.DataFilter.newBuilder();
        // builder.setDeltaMax(0.800).setDeltaMin(-0.800);
        QotGetOptionChain.DataFilter dataFilter = builder.build();

        QotGetOptionChain.C2S c2s = QotGetOptionChain.C2S.newBuilder()
                .setOwner(sec)
                .setCondition(QotGetOptionChain.OptionCondType.OptionCondType_Outside_VALUE)
                .setBeginTime(strikeTime)
                .setEndTime(strikeTime)
                .setDataFilter(dataFilter)
                .build();

        QotGetOptionChain.Request req = QotGetOptionChain.Request.newBuilder().setC2S(c2s).build();
        int seqNo = client.getOptionChain(req);
        log.warn("Send QotGetOptionChain: {}", seqNo);
        if (seqNo == 0) {
            throw new RuntimeException("QotGetOptionChain error");
        }
    }

    @Override
    public OptionsChain result(GeneratedMessageV3 response) {

        QotGetOptionChain.Response resp = (QotGetOptionChain.Response) response;
        if (null == resp || 0 != resp.getRetType()) {
            throw new RuntimeException(resp != null ? resp.getRetMsg() : "QotGetOptionChain error");
        }
        QotGetOptionChain.OptionChain optionChain = resp.getS2C().getOptionChainList().iterator().next();

        return convert(optionChain);
    }

    private OptionsChain convert(QotGetOptionChain.OptionChain resp) {
        if (resp == null) {
            return null;
        }
        String jsonString = JSON.toJSONString(resp);
        OptionsChain optionsChain = JSON.parseObject(jsonString, OptionsChain.class);
        for (OptionsTuple optionsTuple : optionsChain.getOptionList()) {
            Options call = optionsTuple.getCall();
            if (null == call || null == call.getBasic() || "0".equals(call.getBasic().getId())) {
                optionsTuple.setCall(null);
            }

            Options put = optionsTuple.getPut();
            if (null == put || null == put.getBasic() || "0".equals(put.getBasic().getId())) {
                optionsTuple.setPut(null);
            }
        }
        return optionsChain;
    }

}
