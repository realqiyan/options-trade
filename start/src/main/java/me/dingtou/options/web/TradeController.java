package me.dingtou.options.web;


import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.model.Order;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class TradeController {


    @RequestMapping(value = "/trade/buy", method = RequestMethod.POST)
    public Order buy(Order order) throws Exception {
        return order;
    }

}
