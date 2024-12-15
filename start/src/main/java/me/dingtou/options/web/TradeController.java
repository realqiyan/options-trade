package me.dingtou.options.web;


import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.constant.OrderAction;
import me.dingtou.options.constant.TradeSide;
import me.dingtou.options.model.Options;
import me.dingtou.options.model.OwnerOrder;
import me.dingtou.options.service.OptionsQueryService;
import me.dingtou.options.service.OptionsTradeService;
import me.dingtou.options.web.util.SessionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
public class TradeController {

    @Autowired
    private OptionsQueryService optionsQueryService;

    @Autowired
    private OptionsTradeService optionsTradeService;


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
        return optionsTradeService.submit(strategyId, TradeSide.of(side), quantity, new BigDecimal(price), optionsObj);
    }


    @RequestMapping(value = "/trade/close", method = RequestMethod.POST)
    public OwnerOrder close(@RequestParam(value = "owner", required = true) String owner,
                            @RequestParam(value = "side", required = true) Integer side,
                            @RequestParam(value = "strategyId", required = true) String strategyId,
                            @RequestParam(value = "quantity", required = true) Integer quantity,
                            @RequestParam(value = "price", required = true) String price,
                            @RequestParam(value = "order", required = true) String order) throws Exception {

        log.info("trade close. owner:{}, side:{}, quantity:{}, price:{}, order:{}", owner, side, quantity, price, order);

        String loginOwner = SessionUtils.getCurrentOwner();
        if (!loginOwner.equals(owner)) {
            return null;
        }
        OwnerOrder orderObj = JSON.parseObject(order, OwnerOrder.class);
        return optionsTradeService.close(strategyId, TradeSide.of(side), quantity, new BigDecimal(price), orderObj);
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
