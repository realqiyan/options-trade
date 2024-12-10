package me.dingtou.options.manager;

import me.dingtou.options.gateway.OptionsChainGateway;
import me.dingtou.options.gateway.SecurityGateway;
import me.dingtou.options.model.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;

@Component
public class OptionsManager {

    @Autowired
    private OptionsChainGateway optionsChainGateway;

    @Autowired
    private SecurityGateway securityGateway;

    public List<OptionsExpDate> queryOptionsExpDate(String code, Integer market) {
        if (StringUtils.isBlank(code) || null == market) {
            return Collections.emptyList();
        }
        return optionsChainGateway.getOptionsExpDate(market, code);
    }

    public OptionsChain queryOptionsChain(String code, Integer market, OptionsExpDate optionsExpDate) {
        if (StringUtils.isBlank(code) || null == market || null == optionsExpDate || null == optionsExpDate.getStrikeTime()) {
            return null;
        }

        OptionsChain optionsChain = optionsChainGateway.queryOptionsChain(market, code, optionsExpDate.getStrikeTime());
        SecurityQuote securityQuote = securityGateway.quote(Security.of(code, market));
        optionsChain.setSecurityQuote(securityQuote);

        return optionsChain;
    }


}
