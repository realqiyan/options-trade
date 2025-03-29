package me.dingtou.options.strategy;

import me.dingtou.options.model.OptionsChain;
import me.dingtou.options.model.OptionsStrikeDate;
import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.model.OwnerStrategy;
import me.dingtou.options.model.StrategySummary;

/**
 * 期权策略
 *
 * @author qiyan
 */
public interface OptionsTradeStrategy {

    /**
     * 是否支持该策略
     *
     * @param strategy 策略
     * @return true/false
     */
    boolean isSupport(OwnerStrategy strategy);

    /**
     * 计算&补全
     *
     * @param account           账户
     * @param optionsStrikeDate 期权到期日
     * @param optionsChain      期权链
     * @param strategySummary   策略信息（可选）
     */
    void calculate(OwnerAccount account,
            OptionsStrikeDate optionsStrikeDate,
            OptionsChain optionsChain,
            StrategySummary strategySummary);
}
