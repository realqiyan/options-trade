package me.dingtou.options.gateway.futu;

import me.dingtou.options.gateway.OptionsChainGateway;
import me.dingtou.options.gateway.futu.func.FuncGetOptionChain;
import me.dingtou.options.gateway.futu.func.FuncGetOptionExpirationDate;
import me.dingtou.options.model.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Component
public class OptionsChainGatewayImpl implements OptionsChainGateway {


    @Override
    public List<OptionsExpDate> getOptionsExpDate(Security security) {
        return BaseFuncExecutor.exec(new FuncGetOptionExpirationDate(security.getMarket(), security.getCode()));
    }

    @Override
    public OptionsChain queryOptionsChain(Security security, String strikeTime, BigDecimal lastDone) {
        BigDecimal minStrikePrice = BigDecimal.ZERO;
        BigDecimal maxStrikePrice = BigDecimal.valueOf(Long.MAX_VALUE);
        if (null != lastDone) {
            minStrikePrice = lastDone.multiply(BigDecimal.valueOf(0.8));
            maxStrikePrice = lastDone.multiply(BigDecimal.valueOf(1.5));
        }
        OptionsChain optionsChain = BaseFuncExecutor.exec(new FuncGetOptionChain(security.getMarket(), security.getCode(), strikeTime));
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
        List<OptionsRealtimeData> optionsBasicInfo = FillBasicInfoExecutor.fill(allSecurity);
        if (optionsBasicInfo.isEmpty()) {
            optionsBasicInfo = FillBasicInfoExecutor.fill(allSecurity);
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
