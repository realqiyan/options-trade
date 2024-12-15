package me.dingtou.options.gateway.futu.executor.func;

import com.futu.openapi.pb.QotCommon;
import com.futu.openapi.pb.QotGetBasicQot;
import com.google.protobuf.GeneratedMessageV3;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.gateway.futu.executor.QueryExecutor;
import me.dingtou.options.model.OptionsRealtimeData;
import me.dingtou.options.model.Security;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
public class FuncGetOptionsRealtimeData implements FunctionCall<List<OptionsRealtimeData>> {

    private final List<Security> allSecurity;

    public FuncGetOptionsRealtimeData(List<Security> allSecurity) {
        this.allSecurity = allSecurity;
    }

    @Override
    public List<Security> getSubSecurityList() {
        return allSecurity;
    }

    @Override
    public void call(QueryExecutor<List<OptionsRealtimeData>> client) {
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
        int seqNo = ((QueryExecutor<List<OptionsRealtimeData>>) client).getBasicQot(req);
        log.warn("Send QotGetBasicQot: {}", seqNo);
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
        int newScale = 4;
        data.setDelta(delta.setScale(newScale, RoundingMode.HALF_UP));
        BigDecimal gamma = new BigDecimal(String.valueOf(optionExData.getGamma()));
        data.setGamma(gamma.setScale(newScale, RoundingMode.HALF_UP));
        BigDecimal theta = new BigDecimal(String.valueOf(optionExData.getTheta()));
        data.setTheta(theta.setScale(newScale, RoundingMode.HALF_UP));
        BigDecimal vega = new BigDecimal(String.valueOf(optionExData.getVega()));
        data.setVega(vega.setScale(newScale, RoundingMode.HALF_UP));
        BigDecimal rho = new BigDecimal(String.valueOf(optionExData.getRho()));
        data.setRho(rho.setScale(newScale, RoundingMode.HALF_UP));
        BigDecimal impliedVolatility = new BigDecimal(String.valueOf(optionExData.getImpliedVolatility()));
        data.setImpliedVolatility(impliedVolatility.setScale(newScale, RoundingMode.HALF_UP));
        BigDecimal premium = new BigDecimal(String.valueOf(optionExData.getPremium()));
        data.setPremium(premium.setScale(newScale, RoundingMode.HALF_UP));
        BigDecimal curPrice = new BigDecimal(String.valueOf(basicQot.getCurPrice()));
        data.setCurPrice(curPrice.setScale(newScale, RoundingMode.HALF_UP));
        return data;
    }
}
