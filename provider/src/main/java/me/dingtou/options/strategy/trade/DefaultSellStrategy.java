package me.dingtou.options.strategy.trade;

import me.dingtou.options.constant.OptionsStrategy;
import me.dingtou.options.model.*;
import me.dingtou.options.util.TemplateRenderer;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class DefaultSellStrategy extends BaseTradeStrategy {

    @Override
    void processData(OwnerAccount account,
            OptionsChain optionsChain,
            StrategySummary summary) {
        return;
    }

    @Override
    StringBuilder processPrompt(OwnerAccount account, OptionsChain optionsChain, StrategySummary summary) {

        // 准备模板数据
        Map<String, Object> data = new HashMap<>();
        data.put("securityQuote", optionsChain.getStockIndicator().getSecurityQuote());
        data.put("optionsChain", optionsChain);
        data.put("summary", summary);
        data.put("securityPrice", optionsChain.getStockIndicator().getSecurityQuote().getLastDone());
        data.put("vixIndicator", optionsChain.getVixIndicator());
        data.put("currentDate", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        
        String promptStr = TemplateRenderer.render("trade_default.ftl", data);
        return new StringBuilder(promptStr);
    }

    @Override
    public boolean isSupport(OwnerStrategy strategy) {
        String code = OptionsStrategy.DEFAULT.getCode();
        return null == strategy || code.equalsIgnoreCase(strategy.getStrategyCode());
    }
}
