package me.dingtou.options.strategy.trade;

import me.dingtou.options.model.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

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

        // AI分析提示词
        VixIndicator vixIndicator = optionsChain.getVixIndicator();
        StockIndicator stockIndicator = optionsChain.getStockIndicator();
        SecurityQuote securityQuote = stockIndicator.getSecurityQuote();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        BigDecimal securityPrice = securityQuote.getLastDone();
        StringBuilder prompt = new StringBuilder();
        prompt.append("我打算长期持有").append(securityQuote.getSecurity().toString())
                .append("，同时准备通过做CoveredCall对冲组合风险，如果可以顺便赚取部分权利金，整体策略持仓组合每股Delta目标0.25到0.75之间，备选的期权距离到期日")
                .append(optionsChain.dte()).append("天");
        if (null != summary) {
            prompt.append("，策略ID:").append(summary.getStrategy().getStrategyId());
            prompt.append("，当前持有股数:").append(summary.getHoldStockNum());
            prompt.append("，当前策略整体Delta:").append(summary.getStrategyDelta());
            prompt.append("，策略持仓组合每股Delta:").append(summary.getAvgDelta());
        }
        prompt.append("，当前股票价格是").append(securityPrice)
                .append(null != vixIndicator && null != vixIndicator.getCurrentVix()
                        ? "，当前VIX指数是" + vixIndicator.getCurrentVix().getValue()
                        : "")
                .append("，当前日期是").append(sdf.format(new Date()))
                .append("，接下来我将使用markdown格式给你提供一些信息，你需要根据信息给我交易建议。\n\n");

        return prompt;
    }

    @Override
    public boolean isSupport(OwnerStrategy strategy) {
        return null == strategy || "cc_strategy".equalsIgnoreCase(strategy.getStrategyCode());
    }

}
