package me.dingtou.options.web;


import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.model.OptionsExpDate;
import me.dingtou.options.model.Security;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
public class TradeController {


    @RequestMapping(value = "/trade/buy", method = RequestMethod.POST)
    public List<OptionsExpDate> buy(Security security) throws Exception {

        return null;
    }

}
