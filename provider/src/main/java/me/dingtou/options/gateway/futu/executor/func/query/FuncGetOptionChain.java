package me.dingtou.options.gateway.futu.executor.func.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.alibaba.fastjson.JSON;
import com.futu.openapi.pb.QotCommon;
import com.futu.openapi.pb.QotGetOptionChain;
import com.futu.openapi.pb.QotGetOptionChain.C2S.Builder;
import com.google.protobuf.GeneratedMessageV3;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.gateway.futu.executor.QueryExecutor;
import me.dingtou.options.gateway.futu.executor.func.QueryFunctionCall;
import me.dingtou.options.model.Options;
import me.dingtou.options.model.OptionsTuple;

/**
 * 获取期权链
 *
 * @author qiyan
 */
@Slf4j
public class FuncGetOptionChain implements QueryFunctionCall<List<Options>> {

    private final int market;
    private final String code;
    private final String strikeTime;
    private final boolean isAll;

    public FuncGetOptionChain(int market, String code, String strikeTime) {
        this(market, code, strikeTime, false);
    }

    public FuncGetOptionChain(int market, String code, String strikeTime, boolean isAll) {
        this.market = market;
        this.code = code;
        this.strikeTime = strikeTime;
        this.isAll = isAll;
    }

    @Override
    public int call(QueryExecutor client) {

        QotCommon.Security sec = QotCommon.Security.newBuilder()
                .setMarket(market)
                .setCode(code)
                .build();

        QotGetOptionChain.DataFilter.Builder builder = QotGetOptionChain.DataFilter.newBuilder();
        // builder.setDeltaMax(0.800).setDeltaMin(-0.800);
        QotGetOptionChain.DataFilter dataFilter = builder.build();

        Builder chainBuilder = QotGetOptionChain.C2S.newBuilder();
        if (!isAll) {
            chainBuilder.setDataFilter(dataFilter)
                    .setCondition(QotGetOptionChain.OptionCondType.OptionCondType_Outside_VALUE);
        }
        QotGetOptionChain.C2S c2s = chainBuilder
                .setOwner(sec)
                .setBeginTime(strikeTime)
                .setEndTime(strikeTime)
                .build();

        QotGetOptionChain.Request req = QotGetOptionChain.Request.newBuilder().setC2S(c2s).build();
        int seqNo = client.getOptionChain(req);
        log.warn("Send QotGetOptionChain: {}", seqNo);
        if (seqNo == 0) {
            throw new RuntimeException("QotGetOptionChain error");
        }
        return seqNo;
    }

    @Override
    public List<Options> result(GeneratedMessageV3 response) {

        QotGetOptionChain.Response resp = (QotGetOptionChain.Response) response;
        if (null == resp || 0 != resp.getRetType()) {
            throw new RuntimeException(resp != null ? resp.getRetMsg() : "QotGetOptionChain error");
        }
        QotGetOptionChain.OptionChain optionChain = resp.getS2C().getOptionChainList().iterator().next();

        return convert(optionChain);
    }

    private List<Options> convert(QotGetOptionChain.OptionChain resp) {
        if (resp == null) {
            return Collections.emptyList();
        }
        List<Options> optionsList = new ArrayList<>();
        String jsonString = JSON.toJSONString(resp.getOptionList());
        List<OptionsTuple> optionsTuples = JSON.parseArray(jsonString, OptionsTuple.class);
        for (OptionsTuple optionsTuple : optionsTuples) {
            Options call = optionsTuple.getCall();
            if (null != call && null != call.getBasic() && !"0".equals(call.getBasic().getId())) {
                optionsList.add(call);
            }

            Options put = optionsTuple.getPut();
            if (null != put && null != put.getBasic() && !"0".equals(put.getBasic().getId())) {
                optionsList.add(put);
            }
        }
        return optionsList;
    }

}
