package me.dingtou.options.web;


import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.constant.TradeSide;
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

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@RestController
public class OptionsController {

    @Autowired
    private OptionsQueryService optionsQueryService;

    @Autowired
    private OptionsTradeService optionsTradeService;


    @RequestMapping(value = "/options/owner/get", method = RequestMethod.GET)
    public Owner queryOwner() throws Exception {
        log.info("queryOwner");
        String owner = SessionUtils.getCurrentOwner();
        return optionsQueryService.queryOwner(owner);
    }

    @RequestMapping(value = "/options/strike/list", method = RequestMethod.GET)
    public List<OptionsExpDate> listOptionsExpDate(Security security) throws Exception {
        log.info("listOptionsExpDate. security:{}", security);
        if (null == security || StringUtils.isEmpty(security.getCode())) {
            return Collections.emptyList();
        }
        List<OptionsExpDate> optionsExpDates = optionsQueryService.queryOptionsExpDate(security);
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
        log.info("listOptionsChain. market:{}, code:{}, strikeTime:{}, strikeTimestamp:{}, optionExpiryDateDistance:{}", market, code, strikeTime, strikeTimestamp, optionExpiryDateDistance);
        Security security = new Security();
        security.setMarket(market);
        security.setCode(code);

        OptionsExpDate optionsExpDate = new OptionsExpDate();
        optionsExpDate.setStrikeTime(strikeTime);
        optionsExpDate.setStrikeTimestamp(strikeTimestamp);
        optionsExpDate.setOptionExpiryDateDistance(optionExpiryDateDistance);

        return optionsQueryService.queryOptionsChain(security, optionsExpDate);
    }


    @RequestMapping(value = "/options/sell", method = RequestMethod.POST)
    public Order sell(@RequestParam(value = "owner", required = true) String owner,
                      @RequestParam(value = "account", required = true) String account,
                      @RequestParam(value = "quantity", required = true) Long quantity,
                      @RequestParam(value = "price", required = true) String price,
                      @RequestParam(value = "options", required = true) String options) throws Exception {

        log.info("buy: owner={}, quantity={}, price={}, options={}", owner, quantity, price, options);
        String loginOwner = SessionUtils.getCurrentOwner();
        if (!loginOwner.equals(owner)) {
            return null;
        }
        Account accountObj = JSON.parseObject(account, Account.class);
        if (!loginOwner.equals(accountObj.getOwner())) {
            return null;
        }
        Options optionsObj = JSON.parseObject(options, Options.class);
        return optionsTradeService.trade(accountObj, TradeSide.SELL, quantity, new BigDecimal(price), optionsObj);
    }

}
