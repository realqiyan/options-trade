package me.dingtou.options.gateway.futu.executor.func;

import com.alibaba.fastjson.JSON;
import com.futu.openapi.pb.QotCommon;
import com.futu.openapi.pb.QotGetOptionExpirationDate;
import com.google.protobuf.GeneratedMessageV3;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.gateway.futu.executor.QueryExecutor;
import me.dingtou.options.model.OptionsStrikeDate;
import me.dingtou.options.model.Security;

import java.util.Collections;
import java.util.List;

/**
 * 获取期权到期日链
 *
 * @author qiyan
 */
@Slf4j
public class FuncGetOptionExpirationDate implements FunctionCall<List<OptionsStrikeDate>> {

    private final int market;
    private final String code;

    public FuncGetOptionExpirationDate(int market, String code) {
        this.market = market;
        this.code = code;
    }


    @Override
    public List<Security> getSubSecurityList() {
        return Collections.emptyList();
    }

    @Override
    public void call(QueryExecutor<List<OptionsStrikeDate>> client) {

        QotCommon.Security sec = QotCommon.Security.newBuilder()
                .setMarket(market)
                .setCode(code)
                .build();
        QotGetOptionExpirationDate.C2S c2s = QotGetOptionExpirationDate.C2S.newBuilder()
                .setOwner(sec)
                .build();
        QotGetOptionExpirationDate.Request req = QotGetOptionExpirationDate.Request.newBuilder().setC2S(c2s).build();
        int seqNo = client.getOptionExpirationDate(req);
        log.warn("Send QotGetOptionExpirationDate: {}", seqNo);
        if (seqNo == 0) {
            throw new RuntimeException("QotGetOptionExpirationDate error");
        }

    }

    @Override
    public List<OptionsStrikeDate> result(GeneratedMessageV3 response) {

        QotGetOptionExpirationDate.Response resp = (QotGetOptionExpirationDate.Response) response;
        if (null == resp || 0 != resp.getRetType()) {
            return Collections.emptyList();
        }

        return convert(resp.getS2C().getDateListList());
    }

    private List<OptionsStrikeDate> convert(List<QotGetOptionExpirationDate.OptionExpirationDate> dateLis) {
        if (dateLis == null) {
            return Collections.emptyList();
        }
        String jsonString = JSON.toJSONString(dateLis);
        return JSON.parseArray(jsonString, OptionsStrikeDate.class);
    }

}
