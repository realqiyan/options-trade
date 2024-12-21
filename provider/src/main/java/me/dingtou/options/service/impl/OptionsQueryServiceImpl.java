package me.dingtou.options.service.impl;

import me.dingtou.options.constant.TradeSide;
import me.dingtou.options.manager.OptionsManager;
import me.dingtou.options.manager.OwnerManager;
import me.dingtou.options.manager.TradeManager;
import me.dingtou.options.model.*;
import me.dingtou.options.service.OptionsQueryService;
import me.dingtou.options.strategy.OptionsStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class OptionsQueryServiceImpl implements OptionsQueryService {

    @Autowired
    private List<OptionsStrategy> allOptionsStrategy;

    @Autowired
    private OptionsManager optionsManager;

    @Autowired
    private OwnerManager ownerManager;

    @Autowired
    private TradeManager tradeManager;


    @Override
    public Owner queryOwner(String owner) {
        return ownerManager.queryOwner(owner);
    }


    @Override
    public List<OptionsStrikeDate> queryOptionsExpDate(Security security) {
        return optionsManager.queryOptionsExpDate(security.getCode(), security.getMarket());
    }

    @Override
    public StrategySummary queryStrategySummary(String owner, String strategyId) {
        StrategySummary summary = new StrategySummary();
        List<OwnerStrategy> ownerStrategies = ownerManager.queryOwnerStrategy(owner);
        Optional<OwnerStrategy> strategyOptional = ownerStrategies.stream().filter(ownerStrategy -> ownerStrategy.getStrategyId().equals(strategyId)).findAny();
        if (strategyOptional.isEmpty()) {
            return summary;
        }
        // 策略
        OwnerStrategy ownerStrategy = strategyOptional.get();
        summary.setStrategy(ownerStrategy);

        // 订单列表
        List<OwnerOrder> ownerOrders = ownerManager.queryStrategyOrder(ownerStrategy);

        summary.setStrategyOrders(ownerOrders);

        // 订单费用
        BigDecimal totalFee = tradeManager.queryTotalOrderFee(ownerStrategy, ownerOrders);
        summary.setTotalFee(totalFee);

        // 订单总金额
        BigDecimal lotSize = new BigDecimal(ownerStrategy.getLotSize());
        List<BigDecimal> totalPriceList = ownerOrders.stream().map(order -> {
            BigDecimal sign = new BigDecimal(TradeSide.of(order.getSide()).getSign());
            return order.getPrice().multiply(lotSize).multiply(sign);
        }).toList();
        BigDecimal totalPrice = totalPriceList.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        // 订单利润
        summary.setTotalIncome(totalPrice.subtract(totalFee));
        return summary;
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
        if (null == allOptionsStrategy || allOptionsStrategy.isEmpty()) {
            return;
        }
        for (OptionsStrategy optionsStrategy : allOptionsStrategy) {
            optionsStrategy.calculate(optionsStrikeDate, optionsChain);
        }
    }


}
