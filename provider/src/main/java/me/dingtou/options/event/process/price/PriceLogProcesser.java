package me.dingtou.options.event.process.price;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.constant.PushDataType;
import me.dingtou.options.event.AppEvent;
import me.dingtou.options.event.EventProcesser;
// import me.dingtou.options.model.SecurityQuote;
@Slf4j
@Component
public class PriceLogProcesser implements EventProcesser {

    @Override
    public PushDataType supportType() {
        return PushDataType.STOCK_PRICE;
    }

    @Override
    public void process(AppEvent event) {
        //SecurityQuote securityQuote = (SecurityQuote) event.getSource();
        //log.debug("PriceEventProcesser process: {}", securityQuote);
    }

}
