package me.dingtou.options.manager;

import me.dingtou.options.gateway.OptionsChainGateway;
import me.dingtou.options.model.OptionsChain;
import me.dingtou.options.model.OptionsExpDate;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class OptionsManager {

    @Autowired
    private OptionsChainGateway optionsChainGateway;

    public List<OptionsExpDate> queryOptionsExpDate(String code, Integer market) {
        if (StringUtils.isBlank(code) || null == market) {
            return Collections.emptyList();
        }
        return optionsChainGateway.getOptionsExpDate(market, code);
    }

    public OptionsChain queryOptionsChain(String code, Integer market, String strikeTime) {
        if (StringUtils.isBlank(code) || null == market || null == strikeTime) {
            return null;
        }
        return optionsChainGateway.queryOptionsChain(market, code, strikeTime);
    }
}
