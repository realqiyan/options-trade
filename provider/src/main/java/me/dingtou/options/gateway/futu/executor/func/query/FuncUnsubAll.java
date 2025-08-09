package me.dingtou.options.gateway.futu.executor.func.query;

import com.futu.openapi.pb.QotCommon;
import com.futu.openapi.pb.QotSub;
import com.google.protobuf.GeneratedMessageV3;

import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.gateway.futu.executor.QueryExecutor;
import me.dingtou.options.gateway.futu.executor.func.QueryFunctionCall;

@Slf4j
public class FuncUnsubAll implements QueryFunctionCall<Boolean> {

    private boolean isContinue = true;

    @Override
    public int call(QueryExecutor client) {
        isContinue = false;
        QotSub.C2S c2s = QotSub.C2S.newBuilder()
                .addSubTypeList(QotCommon.SubType.SubType_Basic_VALUE)
                .addSubTypeList(QotCommon.SubType.SubType_OrderBook_VALUE)
                .setIsUnsubAll(true)
                .setIsSubOrUnSub(false)
                .build();
        QotSub.Request req = QotSub.Request.newBuilder().setC2S(c2s).build();
        int seqNo = client.sub(req);
        log.debug("Send QotSub: {}", seqNo);
        return seqNo;
    }

    @Override
    public Boolean result(GeneratedMessageV3 response) {
        QotSub.Response resp = (QotSub.Response) response;
        log.debug("FuncUnsubAll ErrCode:{} RetMsg:{}", resp.getErrCode(), resp.getRetMsg());
        return resp.getErrCode() == 0;
    }

    @Override
    public boolean isContinue() {
        return isContinue;
    }

}
