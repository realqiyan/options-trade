package me.dingtou.options.manager;

import me.dingtou.options.gateway.OptionsChainGateway;
import me.dingtou.options.gateway.SecurityGateway;
import me.dingtou.options.model.OptionsChain;
import me.dingtou.options.model.OptionsExpDate;
import me.dingtou.options.model.Security;
import me.dingtou.options.model.SecurityQuote;
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
    private SecurityGateway securityGateway;

    public List<OptionsExpDate> queryOptionsExpDate(String code, Integer market) {
        if (StringUtils.isBlank(code) || null == market) {
            return Collections.emptyList();
        }
        return optionsChainGateway.getOptionsExpDate(Security.of(code, market));
    }

    public OptionsChain queryOptionsChain(String code, Integer market, OptionsExpDate optionsExpDate) {
        if (StringUtils.isBlank(code) || null == market || null == optionsExpDate || null == optionsExpDate.getStrikeTime()) {
            return null;
        }
        Security security = Security.of(code, market);
        SecurityQuote securityQuote = securityGateway.quote(security);
        OptionsChain optionsChain = optionsChainGateway.queryOptionsChain(security, optionsExpDate.getStrikeTime(), securityQuote.getLastDone());
        optionsChain.setSecurityQuote(securityQuote);

        return optionsChain;
    }


}
