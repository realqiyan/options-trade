package me.dingtou.options.gateway.futu.executor.func.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.alibaba.fastjson2.JSON;
import com.futu.openapi.pb.QotCommon;
import com.futu.openapi.pb.QotGetOptionChain;
import com.futu.openapi.pb.QotGetOptionChain.C2S.Builder;
import com.futu.openapi.pb.QotGetOptionChain.OptionItem;
import com.google.protobuf.GeneratedMessageV3;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.constant.OptionsFilterType;
import me.dingtou.options.gateway.futu.executor.QueryExecutor;
import me.dingtou.options.gateway.futu.executor.func.QueryFunctionCall;
import me.dingtou.options.gateway.futu.util.RateLimiter;
import me.dingtou.options.model.Options;
import me.dingtou.options.model.OptionsTuple;

@Slf4j
public class FuncGetOptionChain implements QueryFunctionCall<List<Options>> {

    private static final RateLimiter RATE_LIMITER = new RateLimiter("GetOptionChain", 30000, 9);

    private final int market;
    private final String code;
    private final String strikeTime;
    private final OptionsFilterType filterType;

    public FuncGetOptionChain(int market, String code, String strikeTime) {
        this(market, code, strikeTime, OptionsFilterType.ALL);
    }

    public FuncGetOptionChain(int market, String code, String strikeTime, OptionsFilterType filterType) {
        this.market = market;
        this.code = code;
        this.strikeTime = strikeTime;
        this.filterType = filterType;
    }

    @Override
    public int call(QueryExecutor client) {
        RATE_LIMITER.acquire();

        QotCommon.Security sec = QotCommon.Security.newBuilder()
                .setMarket(market)
                .setCode(code)
                .build();

        QotGetOptionChain.DataFilter.Builder builder = QotGetOptionChain.DataFilter.newBuilder();
        // builder.setDeltaMax(0.800).setDeltaMin(-0.800);
        QotGetOptionChain.DataFilter dataFilter = builder.build();

        Builder chainBuilder = QotGetOptionChain.C2S.newBuilder();
        if (OptionsFilterType.OTM_ALL.equals(filterType)) {
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
        log.debug("Send QotGetOptionChain: {}", seqNo);
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
        if (resp.getS2C().getOptionChainList().isEmpty()) {
            return Collections.emptyList();
        }
        QotGetOptionChain.OptionChain optionChain = resp.getS2C().getOptionChainList().iterator().next();

        return convert(optionChain);
    }

    private List<Options> convert(QotGetOptionChain.OptionChain resp) {
        if (resp == null || null == resp.getOptionList()) {
            return Collections.emptyList();
        }
        List<Options> optionsList = new ArrayList<>();

        for (QotGetOptionChain.OptionItem optionItem : resp.getOptionList()) {
            OptionsTuple optionsTuple = convert(optionItem);

            Options call = optionsTuple.getCall();
            Options put = optionsTuple.getPut();

            // 根据过滤类型添加相应的期权
            switch (filterType) {
                case ALL:
                case OTM_ALL:
                    if (null != call && null != call.getBasic() && !"0".equals(call.getBasic().getId())) {
                        optionsList.add(call);
                    }
                    if (null != put && null != put.getBasic() && !"0".equals(put.getBasic().getId())) {
                        optionsList.add(put);
                    }
                    break;
                case CALL:
                    if (null != call && null != call.getBasic() && !"0".equals(call.getBasic().getId())) {
                        optionsList.add(call);
                    }
                    break;
                case PUT:
                    if (null != put && null != put.getBasic() && !"0".equals(put.getBasic().getId())) {
                        optionsList.add(put);
                    }
                    break;
            }
        }
        return optionsList;
    }

    /**
     * 对象转换
     * 
     * @param optionItem API数据结构
     * @return OptionsTuple
     */
    private OptionsTuple convert(OptionItem optionItem) {
        String jsonString = JSON.toJSONString(optionItem);
        OptionsTuple convertValue = JSON.parseObject(jsonString, OptionsTuple.class);
        return convertValue;
    }

}
