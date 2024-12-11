package me.dingtou.options.web;


import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.model.OptionsChain;
import me.dingtou.options.model.OptionsExpDate;
import me.dingtou.options.model.Security;
import me.dingtou.options.service.OptionsService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@Slf4j
@RestController
public class OptionsController {

    @Autowired
    private OptionsService optionsService;


    @RequestMapping(value = "/options/strike/list", method = RequestMethod.GET)
    public List<OptionsExpDate> listOptionsExpDate(Security security) throws Exception {
        if (null == security || StringUtils.isEmpty(security.getCode())) {
            return Collections.emptyList();
        }
        List<OptionsExpDate> optionsExpDates = optionsService.queryOptionsExpDate(security);
        if (null == optionsExpDates || optionsExpDates.isEmpty()) {
            return Collections.emptyList();
        }
        return optionsExpDates;
    }

    @RequestMapping(value = "/options/chain/get", method = RequestMethod.GET)
    public OptionsChain listOptionsChain(@RequestParam(value = "market", required = true) Integer market,
                                         @RequestParam(value = "code", required = true) String code,
                                         @RequestParam(value = "strikeTime", required = true) String strikeTime,
                                         @RequestParam(value = "strikeTimestamp", required = true) Long strikeTimestamp,
                                         @RequestParam(value = "optionExpiryDateDistance", required = true) Integer optionExpiryDateDistance) throws Exception {
        Security security = new Security();
        security.setMarket(market);
        security.setCode(code);

        OptionsExpDate optionsExpDate = new OptionsExpDate();
        optionsExpDate.setStrikeTime(strikeTime);
        optionsExpDate.setStrikeTimestamp(strikeTimestamp);
        optionsExpDate.setOptionExpiryDateDistance(optionExpiryDateDistance);

        return optionsService.queryOptionsChain(security, optionsExpDate);
    }

}
