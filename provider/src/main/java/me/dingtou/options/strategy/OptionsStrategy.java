package me.dingtou.options.strategy;

import me.dingtou.options.model.OptionsChain;
import me.dingtou.options.model.OptionsStrikeDate;

/**
 * 期权策略
 *
 * @author qiyan
 */
public interface OptionsStrategy {
    /**
     * 计算&补全
     *
     * @param optionsStrikeDate 期权到期日
     * @param optionsChain      期权链
     */
    void calculate(OptionsStrikeDate optionsStrikeDate, OptionsChain optionsChain);
}
