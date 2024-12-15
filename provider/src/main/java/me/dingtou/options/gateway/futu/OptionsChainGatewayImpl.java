package me.dingtou.options.gateway.futu;

import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.gateway.OptionsChainGateway;
import me.dingtou.options.gateway.futu.executor.QueryExecutor;
import me.dingtou.options.gateway.futu.executor.func.FuncGetOptionsRealtimeData;
import me.dingtou.options.gateway.futu.executor.func.FuncGetOptionChain;
import me.dingtou.options.gateway.futu.executor.func.FuncGetOptionExpirationDate;
import me.dingtou.options.model.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Component
@Slf4j
public class OptionsChainGatewayImpl implements OptionsChainGateway {


    @Override
    public List<OptionsStrikeDate> getOptionsExpDate(Security security) {
        return QueryExecutor.query(new FuncGetOptionExpirationDate(security.getMarket(), security.getCode()));
    }

    @Override
    public OptionsChain queryOptionsChain(Security security, String strikeTime, BigDecimal lastDone) {
        BigDecimal minStrikePrice = BigDecimal.ZERO;
        BigDecimal maxStrikePrice = BigDecimal.valueOf(Long.MAX_VALUE);
        if (null != lastDone) {
            minStrikePrice = lastDone.multiply(BigDecimal.valueOf(0.8));
            maxStrikePrice = lastDone.multiply(BigDecimal.valueOf(1.5));
        }
        OptionsChain optionsChain = QueryExecutor.query(new FuncGetOptionChain(security.getMarket(), security.getCode(), strikeTime));
        Set<Security> allSecurity = new HashSet<>();

        ListIterator<OptionsTuple> optionsTupleListIterator = optionsChain.getOptionList().listIterator();
        while (optionsTupleListIterator.hasNext()) {

            // 处理call
            OptionsTuple optionsTuple = optionsTupleListIterator.next();
            Options call = optionsTuple.getCall();
            if (null != call) {
                Security callSecurity = call.getBasic().getSecurity();
                BigDecimal callStrikePrice = call.getOptionExData().getStrikePrice();
                if (callStrikePrice.compareTo(minStrikePrice) >= 0 && callStrikePrice.compareTo(maxStrikePrice) <= 0) {
                    if (null != callSecurity) {
                        allSecurity.add(callSecurity);
                    }
                } else {
                    optionsTuple.setCall(null);
                }
            }

            // 处理put
            Options put = optionsTuple.getPut();
            if (null != put) {
                Security putSecurity = put.getBasic().getSecurity();
                BigDecimal putStrikePrice = put.getOptionExData().getStrikePrice();
                if (putStrikePrice.compareTo(minStrikePrice) >= 0 && putStrikePrice.compareTo(maxStrikePrice) <= 0) {
                    if (null != putSecurity) {
                        allSecurity.add(putSecurity);
                    }
                } else {
                    optionsTuple.setPut(null);
                }
            }

            if (null == optionsTuple.getCall() && null == optionsTuple.getPut()) {
                optionsTupleListIterator.remove();
            }
        }
        List<Security> securityList = new ArrayList<>(allSecurity);
        List<OptionsRealtimeData> optionsBasicInfo = QueryExecutor.query(new FuncGetOptionsRealtimeData(securityList));
        if (optionsBasicInfo.isEmpty()) {
            log.warn("query options basic info failed, retry");
            optionsBasicInfo = QueryExecutor.query(new FuncGetOptionsRealtimeData(securityList));
        }
        mergeRealtimeData(optionsChain, optionsBasicInfo);
        return optionsChain;
    }

    private void mergeRealtimeData(OptionsChain optionsChain, List<OptionsRealtimeData> optionsBasicInfoList) {

        Map<Security, OptionsRealtimeData> realtimeDataMap = new HashMap<>();
        for (OptionsRealtimeData optionsBasicInfo : optionsBasicInfoList) {
            realtimeDataMap.put(optionsBasicInfo.getSecurity(), optionsBasicInfo);
        }
        for (OptionsTuple optionsTuple : optionsChain.getOptionList()) {
            Options call = optionsTuple.getCall();
            if (null != call) {
                Security security = call.getBasic().getSecurity();
                OptionsRealtimeData optionsRealtimeData = realtimeDataMap.get(security);
                call.setRealtimeData(optionsRealtimeData);
            }

            Options put = optionsTuple.getPut();
            if (null != put) {
                Security security = put.getBasic().getSecurity();
                OptionsRealtimeData optionsRealtimeData = realtimeDataMap.get(security);
                put.setRealtimeData(optionsRealtimeData);
            }
        }
    }


}
