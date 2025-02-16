package me.dingtou.options.service.impl;

import me.dingtou.options.constant.OrderExt;
import me.dingtou.options.constant.OrderStatus;
import me.dingtou.options.constant.TradeSide;
import me.dingtou.options.gateway.SecurityQuoteGateway;
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

    @Autowired
    private SecurityQuoteGateway securityQuoteGateway;


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
        Optional<OwnerStrategy> strategyOptional = ownerStrategies.stream()
                .filter(ownerStrategy -> ownerStrategy.getStrategyId().equals(strategyId))
                .findAny();
        if (strategyOptional.isEmpty()) {
            return summary;
        }
        // 策略
        OwnerStrategy ownerStrategy = strategyOptional.get();
        summary.setStrategy(ownerStrategy);

        // 订单列表
        List<OwnerOrder> ownerOrders = ownerManager.queryStrategyOrder(ownerStrategy);
        summary.setStrategyOrders(ownerOrders);

        OwnerAccount account = ownerManager.queryOwnerAccount(owner);
        // 订单费用
        BigDecimal totalFee = tradeManager.queryTotalOrderFee(account, ownerOrders);
        summary.setTotalFee(totalFee);

        // 股票持有数量
        int holdSecurityNum = ownerOrders.stream()
                .filter(order -> order.getCode().equals(ownerStrategy.getCode()))
                .mapToInt(order -> -1 * TradeSide.of(order.getSide()).getSign() * order.getQuantity())
                .sum();
        summary.setHoldSecurityNum(holdSecurityNum);

        // 股票现价
        Security security = Security.of(ownerStrategy.getCode(), account.getMarket());
        SecurityQuote securityQuote = securityQuoteGateway.quote(security);
        BigDecimal lastDone = securityQuote.getLastDone();

        // 股票总金额
        List<BigDecimal> totalSecurityPriceList = ownerOrders.stream()
                .filter(order -> order.getCode().equals(ownerStrategy.getCode()))
                .map(order -> {
                    BigDecimal sign = new BigDecimal(TradeSide.of(order.getSide()).getSign());
                    return order.getPrice().multiply(new BigDecimal(order.getQuantity())).multiply(sign);
                }).toList();
        BigDecimal totalSecurityPrice = totalSecurityPriceList.stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        // 股票利润
        BigDecimal securityIncome = lastDone.multiply(BigDecimal.valueOf(holdSecurityNum)).add(totalSecurityPrice);
        summary.setSecurityIncome(securityIncome);


        // 期权总金额
        List<OwnerOrder> allOptionsOrders = ownerOrders.stream()
                .filter(order -> OrderStatus.of(order.getStatus()).isTraded())
                .filter(order -> !order.getCode().equals(ownerStrategy.getCode()))
                .toList();

        BigDecimal lotSize = new BigDecimal(ownerStrategy.getLotSize());
        // 所有期权利润
        BigDecimal allOptionsIncome = allOptionsOrders.stream()
                .map(order -> {
                    BigDecimal sign = new BigDecimal(TradeSide.of(order.getSide()).getSign());
                    return order.getPrice().multiply(lotSize).multiply(sign);
                }).reduce(BigDecimal.ZERO, BigDecimal::add);
        // 期权利润
        summary.setAllOptionsIncome(allOptionsIncome.subtract(totalFee));

        // 所有未平仓的期权利润
        BigDecimal unrealizedOptionsIncome = allOptionsOrders.stream()
                .filter(order -> Boolean.FALSE.equals(Boolean.valueOf(order.getExt().get(OrderExt.IS_CLOSE.getCode()))))
                .map(order -> {
                    BigDecimal sign = new BigDecimal(TradeSide.of(order.getSide()).getSign());
                    return order.getPrice().multiply(lotSize).multiply(sign);
                }).reduce(BigDecimal.ZERO, BigDecimal::add);
        // 期权利润
        summary.setUnrealizedOptionsIncome(unrealizedOptionsIncome);


        return summary;
    }

    @Override
    public SecurityOrderBook queryOrderBook(Security security) {
        return tradeManager.querySecurityOrderBook(security.getCode(), security.getMarket());
    }

    @Override
    public OptionsChain queryOptionsChain(Security security, OptionsStrikeDate optionsStrikeDate) {
        OptionsChain optionsChain = optionsManager.queryOptionsChain(security.getCode(), security.getMarket(), optionsStrikeDate);

        // 计算策略数据
        calculateStrategyData(optionsStrikeDate, optionsChain);

        return optionsChain;
    }

    @Override
    public List<OwnerOrder> queryDraftOrder(String owner) {
        return tradeManager.queryDraftOrder(owner);
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
