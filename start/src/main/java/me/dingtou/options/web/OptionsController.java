package me.dingtou.options.web;


import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.model.OptionsChain;
import me.dingtou.options.model.OptionsExpDate;
import me.dingtou.options.model.UnderlyingAsset;
import me.dingtou.options.service.OptionsReadService;
import me.dingtou.options.web.util.SessionUtils;
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
    private OptionsReadService optionsReadService;


    @RequestMapping(value = "/options/strike/list", method = RequestMethod.GET)
    public List<OptionsExpDate> listOptionsExpDate(UnderlyingAsset underlyingAsset) throws Exception {
        if (null == underlyingAsset || StringUtils.isEmpty(underlyingAsset.getCode())) {
            return Collections.emptyList();
        }
        String owner = SessionUtils.getCurrentOwner();
        underlyingAsset.setOwner(owner);
        List<OptionsExpDate> optionsExpDates = optionsReadService.queryOptionsExpDate(underlyingAsset);
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
        String owner = SessionUtils.getCurrentOwner();
        UnderlyingAsset underlyingAsset = new UnderlyingAsset();
        underlyingAsset.setOwner(owner);
        underlyingAsset.setMarket(market);
        underlyingAsset.setCode(code);

        OptionsExpDate optionsExpDate = new OptionsExpDate();
        optionsExpDate.setStrikeTime(strikeTime);
        optionsExpDate.setStrikeTimestamp(strikeTimestamp);
        optionsExpDate.setOptionExpiryDateDistance(optionExpiryDateDistance);

        return optionsReadService.queryOptionsChain(underlyingAsset, optionsExpDate);
    }

}
