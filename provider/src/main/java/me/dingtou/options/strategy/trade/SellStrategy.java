package me.dingtou.options.strategy.trade;

import me.dingtou.options.model.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class SellStrategy extends BaseTradeStrategy {

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

        // AI分析提示词
        VixIndicator vixIndicator = optionsChain.getVixIndicator();
        StockIndicator stockIndicator = optionsChain.getStockIndicator();
        SecurityQuote securityQuote = stockIndicator.getSecurityQuote();


        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        BigDecimal securityPrice = securityQuote.getLastDone();
        StringBuilder prompt = new StringBuilder();
        prompt.append("我准备卖出").append(securityQuote.getSecurity().toString())
                .append("距离到期日").append(optionsChain.dte()).append("天的期权");
        if (null != summary) {
            prompt.append("，策略ID:").append(summary.getStrategy().getStrategyId());
            if (null != summary.getHoldStockNum()) {
                prompt.append("，当前持有股票数量：").append(summary.getHoldStockNum());
                // 展示持有股票成本价
                if (summary.getHoldStockCost() != null && summary.getHoldStockCost().compareTo(BigDecimal.ZERO) > 0) {
                    prompt.append("，持有股票成本价：").append(summary.getHoldStockCost());
                }
            }
        }
        prompt.append("当前股票价格是").append(securityPrice)
                .append(null != vixIndicator && null != vixIndicator.getCurrentVix()
                        ? "，当前VIX指数是" + vixIndicator.getCurrentVix().getValue()
                        : "")
                .append("，当前日期是").append(sdf.format(new Date()))
                .append("，接下来我将使用markdown格式给你提供一些信息，你需要根据信息给我交易建议。\n\n");

        return prompt;
    }

    @Override
    public boolean isSupport(OwnerStrategy strategy) {
        return null == strategy || "default".equalsIgnoreCase(strategy.getStrategyCode());
    }
}
