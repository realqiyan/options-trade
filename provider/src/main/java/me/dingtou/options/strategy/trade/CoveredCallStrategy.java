package me.dingtou.options.strategy.trade;

import me.dingtou.options.model.*;
import me.dingtou.options.util.TemplateRenderer;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class CoveredCallStrategy extends BaseTradeStrategy {

    @Override
    void processData(OwnerAccount account,
            OptionsChain optionsChain,
            StrategySummary summary) {
        return;
    }

    @Override
    StringBuilder processPrompt(OwnerAccount account, OptionsChain optionsChain, StrategySummary summary) {
        // 反转delta和theta
        optionsChain.getOptionsList().forEach(options -> {
            OptionsRealtimeData realtimeData = options.getRealtimeData();
            if (null != realtimeData) {
                realtimeData.setDelta(realtimeData.getDelta().multiply(BigDecimal.valueOf(-1)));
                realtimeData.setTheta(realtimeData.getTheta().multiply(BigDecimal.valueOf(-1)));
            } else {
                options.setRealtimeData(new OptionsRealtimeData());
            }
        });

        // 准备模板数据
        Map<String, Object> data = new HashMap<>();
        data.put("securityQuote", optionsChain.getStockIndicator().getSecurityQuote());
        data.put("optionsChain", optionsChain);
        data.put("summary", summary);
        data.put("securityPrice", optionsChain.getStockIndicator().getSecurityQuote().getLastDone());
        data.put("vixIndicator", optionsChain.getVixIndicator());
        data.put("currentDate", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        
        String promptStr = TemplateRenderer.render("trade_cc_strategy.ftl", data);
        return new StringBuilder(promptStr);
    }

    @Override
    public boolean isSupport(OwnerStrategy strategy) {
        return null == strategy || "cc_strategy".equalsIgnoreCase(strategy.getStrategyCode());
    }

}
