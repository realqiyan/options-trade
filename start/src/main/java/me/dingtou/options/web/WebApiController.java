package me.dingtou.options.web;


import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.constant.OrderAction;
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
import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
public class WebApiController {

    @Autowired
    private OptionsQueryService optionsQueryService;

    @Autowired
    private OptionsTradeService optionsTradeService;


    @RequestMapping(value = "/options/owner/get", method = RequestMethod.GET)
    public Owner queryOwner() throws Exception {
        String owner = SessionUtils.getCurrentOwner();
        return optionsQueryService.queryOwner(owner);
    }

    @RequestMapping(value = "/options/strategy/get", method = RequestMethod.GET)
    public StrategySummary queryStrategySummary(@RequestParam(value = "strategyId", required = true) String strategyId) throws Exception {
        String owner = SessionUtils.getCurrentOwner();
        return optionsQueryService.queryStrategySummary(owner, strategyId);
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


    @RequestMapping(value = "/trade/submit", method = RequestMethod.POST)
    public OwnerOrder submit(@RequestParam(value = "owner", required = true) String owner,
                             @RequestParam(value = "side", required = true) Integer side,
                             @RequestParam(value = "strategyId", required = true) String strategyId,
                             @RequestParam(value = "quantity", required = true) Integer quantity,
                             @RequestParam(value = "price", required = true) String price,
                             @RequestParam(value = "options", required = true) String options) throws Exception {

        log.info("trade submit. owner:{}, side:{}, quantity:{}, price:{}, options:{}", owner, side, quantity, price, options);

        String loginOwner = SessionUtils.getCurrentOwner();
        if (!loginOwner.equals(owner)) {
            return null;
        }

        Options optionsObj = JSON.parseObject(options, Options.class);
        BigDecimal sellPrice = new BigDecimal(price);
        return optionsTradeService.submit(strategyId, TradeSide.of(side), quantity, sellPrice, optionsObj);
    }


    @RequestMapping(value = "/trade/close", method = RequestMethod.POST)
    public OwnerOrder close(@RequestParam(value = "owner", required = true) String owner,
                            @RequestParam(value = "price", required = true) String price,
                            @RequestParam(value = "order", required = true) String order) throws Exception {

        log.info("trade close. owner:{}, price:{}, order:{}", owner, price, order);

        String loginOwner = SessionUtils.getCurrentOwner();
        if (!loginOwner.equals(owner)) {
            return null;
        }
        OwnerOrder orderObj = JSON.parseObject(order, OwnerOrder.class);
        return optionsTradeService.close(orderObj, new BigDecimal(price));
    }


    @RequestMapping(value = "/trade/modify", method = RequestMethod.POST)
    public OwnerOrder modify(@RequestParam(value = "action", required = true) String action,
                             @RequestParam(value = "order", required = true) String order) throws Exception {
        String loginOwner = SessionUtils.getCurrentOwner();
        log.info("trade modify. owner:{}, action:{}, order:{}", loginOwner, action, order);

        OrderAction orderAction = OrderAction.of(action);
        OwnerOrder orderObj = JSON.parseObject(order, OwnerOrder.class);
        if (!loginOwner.equals(orderObj.getOwner())) {
            return null;
        }
        return optionsTradeService.modify(orderObj, orderAction);
    }

    @RequestMapping(value = "/trade/sync", method = RequestMethod.GET)
    public List<OwnerOrder> sync() throws Exception {
        String loginOwner = SessionUtils.getCurrentOwner();
        log.info("sync. owner:{}", loginOwner);
        return optionsTradeService.sync(loginOwner);
    }

}