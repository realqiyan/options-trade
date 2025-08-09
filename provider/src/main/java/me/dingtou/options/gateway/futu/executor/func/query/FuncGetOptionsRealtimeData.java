package me.dingtou.options.gateway.futu.executor.func.query;

import com.futu.openapi.pb.QotCommon;
import com.futu.openapi.pb.QotGetBasicQot;
import com.google.protobuf.GeneratedMessageV3;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.gateway.futu.executor.QueryExecutor;
import me.dingtou.options.gateway.futu.executor.func.QueryFunctionCall;
import me.dingtou.options.model.OptionsRealtimeData;
import me.dingtou.options.model.Security;
import me.dingtou.options.util.NumberUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
public class FuncGetOptionsRealtimeData implements QueryFunctionCall<List<OptionsRealtimeData>> {

    private final List<Security> allSecurity;

    public FuncGetOptionsRealtimeData(List<Security> allSecurity) {
        this.allSecurity = allSecurity;
    }

    @Override
    public List<Security> getSubSecurityList() {
        return allSecurity;
    }

    @Override
    public List<Integer> getSubTypeList() {
        List<Integer> subTypeList = new ArrayList<>();
        subTypeList.add(QotCommon.SubType.SubType_Basic_VALUE);
        // subTypeList.add(QotCommon.SubType.SubType_OrderBook_VALUE);
        return subTypeList;
    }

    @Override
    public int call(QueryExecutor client) {
        QotGetBasicQot.C2S.Builder builder = QotGetBasicQot.C2S.newBuilder();
        for (Security security : allSecurity) {
            if (null == security) {
                continue;
            }
            QotCommon.Security sec = QotCommon.Security.newBuilder().setMarket(security.getMarket()).setCode(security.getCode()).build();
            builder.addSecurityList(sec);
        }
        QotGetBasicQot.C2S c2s = builder.build();
        QotGetBasicQot.Request req = QotGetBasicQot.Request.newBuilder().setC2S(c2s).build();
        int seqNo = ((QueryExecutor) client).getBasicQot(req);
        log.debug("Send QotGetBasicQot: {}", seqNo);
        if (seqNo == 0) {
            throw new RuntimeException("QotGetBasicQot error");
        }
        return seqNo;
    }

    @Override
    public List<OptionsRealtimeData> result(GeneratedMessageV3 response) {
        QotGetBasicQot.Response resp = (QotGetBasicQot.Response) response;
        if (null == resp || 0 != resp.getRetType()) {
            return Collections.emptyList();
        }
        List<OptionsRealtimeData> result = new ArrayList<>();

        List<QotCommon.BasicQot> basicQotList = resp.getS2C().getBasicQotListList();
        for (QotCommon.BasicQot basicQot : basicQotList) {
            QotCommon.OptionBasicQotExData optionExData = basicQot.getOptionExData();
            OptionsRealtimeData data = convertOptionsRealtimeData(basicQot, optionExData);
            result.add(data);
        }
        return result;
    }


    private static OptionsRealtimeData convertOptionsRealtimeData(QotCommon.BasicQot basicQot, QotCommon.OptionBasicQotExData optionExData) {
        OptionsRealtimeData data = new OptionsRealtimeData();
        Security security = new Security();
        security.setMarket(basicQot.getSecurity().getMarket());
        security.setCode(basicQot.getSecurity().getCode());
        data.setSecurity(security);
        BigDecimal delta = new BigDecimal(String.valueOf(optionExData.getDelta()));
        data.setDelta(NumberUtils.scale(delta));
        BigDecimal gamma = new BigDecimal(String.valueOf(optionExData.getGamma()));
        data.setGamma(NumberUtils.scale(gamma));
        BigDecimal theta = new BigDecimal(String.valueOf(optionExData.getTheta()));
        data.setTheta(NumberUtils.scale(theta));
        BigDecimal vega = new BigDecimal(String.valueOf(optionExData.getVega()));
        data.setVega(NumberUtils.scale(vega));
        BigDecimal rho = new BigDecimal(String.valueOf(optionExData.getRho()));
        data.setRho(NumberUtils.scale(rho));
        BigDecimal impliedVolatility = new BigDecimal(String.valueOf(optionExData.getImpliedVolatility()));
        data.setImpliedVolatility(NumberUtils.scale(impliedVolatility));
        BigDecimal premium = new BigDecimal(String.valueOf(optionExData.getPremium()));
        data.setPremium(NumberUtils.scale(premium));
        BigDecimal curPrice = new BigDecimal(String.valueOf(basicQot.getCurPrice()));
        data.setCurPrice(NumberUtils.scale(curPrice));
        data.setOpenInterest(optionExData.getOpenInterest());
        data.setVolume(basicQot.getVolume());
        return data;
    }
}
