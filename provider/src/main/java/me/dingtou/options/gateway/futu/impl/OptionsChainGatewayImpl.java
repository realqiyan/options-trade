package me.dingtou.options.gateway.futu.impl;

import me.dingtou.options.gateway.OptionsChainGateway;
import me.dingtou.options.gateway.futu.BaseFuncExecutor;
import me.dingtou.options.gateway.futu.FillBasicInfoExecutor;
import me.dingtou.options.gateway.futu.func.*;
import me.dingtou.options.model.*;
import org.springframework.stereotype.Component;


import java.util.*;

@Component
public class OptionsChainGatewayImpl implements OptionsChainGateway {


    @Override
    public List<OptionsExpDate> getOptionsExpDate(int market, String code) {
        return BaseFuncExecutor.exec(new FuncGetOptionExpirationDate(market, code));
    }

    @Override
    public OptionsChain queryOptionsChain(Integer market, String code, String strikeTime) {
        OptionsChain optionsChain = BaseFuncExecutor.exec(new FuncGetOptionChain(market, code, strikeTime));
        Set<Security> allSecurity = new HashSet<>();
        for (OptionsTuple optionsTuple : optionsChain.getOptionList()) {
            Security callSecurity = optionsTuple.getCall().getBasic().getSecurity();
            Security putSecurity = optionsTuple.getPut().getBasic().getSecurity();
            allSecurity.add(callSecurity);
            allSecurity.add(putSecurity);
        }
        List<OptionsRealtimeData> optionsBasicInfo = FillBasicInfoExecutor.fill(allSecurity);
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
            Security security = call.getBasic().getSecurity();
            OptionsRealtimeData optionsRealtimeData = realtimeDataMap.get(security);
            call.setRealtimeData(optionsRealtimeData);

            Options put = optionsTuple.getPut();
            security = put.getBasic().getSecurity();
            optionsRealtimeData = realtimeDataMap.get(security);
            put.setRealtimeData(optionsRealtimeData);
        }
    }


}
