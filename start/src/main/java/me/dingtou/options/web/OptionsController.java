package me.dingtou.options.web;


import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.model.*;
import me.dingtou.options.service.OptionsQueryService;
import me.dingtou.options.service.OptionsTradeService;
import me.dingtou.options.web.util.SessionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
public class OptionsController {

    @Autowired
    private OptionsQueryService optionsQueryService;

    @Autowired
    private OptionsTradeService optionsTradeService;


    @RequestMapping(value = "/options/owner/get", method = RequestMethod.GET)
    public Owner queryOwner() throws Exception {
        String owner = SessionUtils.getCurrentOwner();
        return optionsQueryService.queryOwner(owner);
    }

    @RequestMapping(value = "/options/order/list", method = RequestMethod.GET)
    public List<OwnerOrder> queryOwnerOrder(@RequestParam(value = "strategyId", required = true) String strategyId) throws Exception {
        String owner = SessionUtils.getCurrentOwner();
        return optionsQueryService.queryStrategyOrder(owner, strategyId);
    }

    @RequestMapping(value = "/options/strike/list", method = RequestMethod.GET)
    public List<OptionsStrikeDate> listOptionsExpDate(Security security) throws Exception {
        log.info("listOptionsExpDate. security:{}", security);
        if (null == security || StringUtils.isEmpty(security.getCode())) {
            return Collections.emptyList();
        }
        List<OptionsStrikeDate> optionsStrikeDates = optionsQueryService.queryOptionsExpDate(security);
        if (null == optionsStrikeDates || optionsStrikeDates.isEmpty()) {
            return Collections.emptyList();
        }
        return optionsStrikeDates;
    }

    @RequestMapping(value = "/options/chain/get", method = RequestMethod.GET)
    public OptionsChain listOptionsChain(@RequestParam(value = "market", required = true) Integer market,
                                         @RequestParam(value = "code", required = true) String code,
                                         @RequestParam(value = "strikeTime", required = true) String strikeTime,
                                         @RequestParam(value = "strikeTimestamp", required = true) Long strikeTimestamp,
                                         @RequestParam(value = "optionExpiryDateDistance", required = true) Integer optionExpiryDateDistance) throws Exception {
        log.info("listOptionsChain. market:{}, code:{}, strikeTime:{}, strikeTimestamp:{}, optionExpiryDateDistance:{}", market, code, strikeTime, strikeTimestamp, optionExpiryDateDistance);
        Security security = new Security();
        security.setMarket(market);
        security.setCode(code);

        OptionsStrikeDate optionsStrikeDate = new OptionsStrikeDate();
        optionsStrikeDate.setStrikeTime(strikeTime);
        optionsStrikeDate.setStrikeTimestamp(strikeTimestamp);
        optionsStrikeDate.setOptionExpiryDateDistance(optionExpiryDateDistance);

        return optionsQueryService.queryOptionsChain(security, optionsStrikeDate);
    }

    @RequestMapping(value = "/options/orderbook/get", method = RequestMethod.GET)
    public SecurityOrderBook listOrderBook(@RequestParam(value = "market", required = true) Integer market,
                                           @RequestParam(value = "code", required = true) String code) throws Exception {
        log.info("listOrderBook. market:{}, code:{}", market, code);
        Security security = new Security();
        security.setMarket(market);
        security.setCode(code);
        return optionsQueryService.queryOrderBook(security);
    }

}
