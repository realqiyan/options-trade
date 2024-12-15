package me.dingtou.options.manager;

import me.dingtou.options.gateway.OptionsChainGateway;
import me.dingtou.options.gateway.SecurityOrderBookGateway;
import me.dingtou.options.gateway.SecurityQuoteGateway;
import me.dingtou.options.model.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class OptionsManager {

    @Autowired
    private OptionsChainGateway optionsChainGateway;

    @Autowired
    private SecurityQuoteGateway securityQuoteGateway;

    @Autowired
    private SecurityOrderBookGateway securityOrderBookGateway;

    public List<OptionsStrikeDate> queryOptionsExpDate(String code, Integer market) {
        if (StringUtils.isBlank(code) || null == market) {
            return Collections.emptyList();
        }
        return optionsChainGateway.getOptionsExpDate(Security.of(code, market));
    }

    public OptionsChain queryOptionsChain(String code, Integer market, OptionsStrikeDate optionsStrikeDate) {
        if (StringUtils.isBlank(code) || null == market || null == optionsStrikeDate || null == optionsStrikeDate.getStrikeTime()) {
            return null;
        }
        Security security = Security.of(code, market);
        SecurityQuote securityQuote = securityQuoteGateway.quote(security);
        OptionsChain optionsChain = optionsChainGateway.queryOptionsChain(security, optionsStrikeDate.getStrikeTime(), securityQuote.getLastDone());
        optionsChain.setSecurityQuote(securityQuote);

        return optionsChain;
    }


    public SecurityOrderBook querySecurityOrderBook(String code, Integer market) {
        if (StringUtils.isBlank(code) || null == market) {
            return null;
        }
        Security security = Security.of(code, market);
        return securityOrderBookGateway.getOrderBook(security);
    }


}
