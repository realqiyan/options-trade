package me.dingtou.options.service.impl;

import me.dingtou.options.manager.OptionsManager;
import me.dingtou.options.manager.OwnerManager;
import me.dingtou.options.model.*;
import me.dingtou.options.service.OptionsQueryService;
import me.dingtou.options.strategy.OptionsStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class OptionsQueryServiceImpl implements OptionsQueryService {

    @Autowired
    private OptionsManager optionsManager;

    @Autowired
    private OwnerManager ownerManager;

    @Autowired
    private List<OptionsStrategy> optionsStrategyList;

    @Override
    public Owner queryOwner(String owner) {
        return ownerManager.queryOwner(owner);
    }


    @Override
    public List<OptionsStrikeDate> queryOptionsExpDate(Security security) {
        return optionsManager.queryOptionsExpDate(security.getCode(), security.getMarket());
    }

    @Override
    public List<OwnerOrder> queryStrategyOrder(String owner, String strategyId) {
        List<OwnerStrategy> ownerStrategies = ownerManager.queryOwnerStrategy(owner);
        Optional<OwnerStrategy> strategyOptional = ownerStrategies.stream().filter(ownerStrategy -> ownerStrategy.getStrategyId().equals(strategyId)).findAny();
        if (strategyOptional.isEmpty()) {
            return Collections.emptyList();
        }

        OwnerStrategy ownerStrategy = strategyOptional.get();
        return ownerManager.queryOwnerOrder(ownerStrategy.getOwner(), ownerStrategy.getStrategyId());

    }

    @Override
    public SecurityOrderBook queryOrderBook(Security security) {
        return optionsManager.querySecurityOrderBook(security.getCode(), security.getMarket());
    }

    @Override
    public OptionsChain queryOptionsChain(Security security, OptionsStrikeDate optionsStrikeDate) {
        OptionsChain optionsChain = optionsManager.queryOptionsChain(security.getCode(), security.getMarket(), optionsStrikeDate);

        // 计算策略数据
        calculateStrategyData(optionsStrikeDate, optionsChain);

        return optionsChain;
    }


    /**
     * 计算策略数据
     *
     * @param optionsStrikeDate 期权到期日
     * @param optionsChain      期权链
     */
    private void calculateStrategyData(OptionsStrikeDate optionsStrikeDate, OptionsChain optionsChain) {
        if (null == optionsStrategyList || optionsStrategyList.isEmpty()) {
            return;
        }
        for (OptionsStrategy optionsStrategy : optionsStrategyList) {
            optionsStrategy.calculate(optionsStrikeDate, optionsChain);
        }
    }


}
