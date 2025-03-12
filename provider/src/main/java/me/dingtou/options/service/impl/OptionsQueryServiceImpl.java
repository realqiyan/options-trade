package me.dingtou.options.service.impl;

import lombok.extern.slf4j.Slf4j;
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
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Slf4j
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
    public OwnerSummary queryOwnerSummary(String owner) {
        OwnerSummary ownerSummary = new OwnerSummary();

        BigDecimal allOptionsIncome = BigDecimal.ZERO;
        BigDecimal totalFee = BigDecimal.ZERO;
        BigDecimal unrealizedOptionsIncome = BigDecimal.ZERO;


        List<OwnerStrategy> ownerStrategies = ownerManager.queryOwnerStrategy(owner);
        List<StrategySummary> strategySummaries = new CopyOnWriteArrayList<>();
        // 批量拉取策略数据
        ownerStrategies.parallelStream().forEach(ownerStrategy -> {
            StrategySummary strategySummary = queryStrategySummary(owner, ownerStrategy.getStrategyId());
            strategySummaries.add(strategySummary);
        });


        // 统计
        List<OwnerOrder> unrealizedOrders = new ArrayList<>();
        for (StrategySummary strategySummary : strategySummaries) {
            allOptionsIncome = allOptionsIncome.add(strategySummary.getAllOptionsIncome());
            totalFee = totalFee.add(strategySummary.getTotalFee());
            unrealizedOptionsIncome = unrealizedOptionsIncome.add(strategySummary.getUnrealizedOptionsIncome());

            strategySummary.getStrategyOrders().stream().filter(order -> {
                if (null == order.getExt()) {
                    return false;
                }
                String isClose = order.getExt().get(OrderExt.IS_CLOSE.getCode());
                return Boolean.FALSE.equals(Boolean.valueOf(isClose));
            }).forEach(unrealizedOrders::add);
        }
        ownerSummary.setAllOptionsIncome(allOptionsIncome);
        ownerSummary.setTotalFee(totalFee);
        ownerSummary.setUnrealizedOptionsIncome(unrealizedOptionsIncome);
        ownerSummary.setStrategySummaries(strategySummaries);
        ownerSummary.setUnrealizedOrders(unrealizedOrders);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM");
        Map<String, List<OwnerOrder>> monthOrder = strategySummaries.stream()
                .flatMap(strategy -> strategy.getStrategyOrders().stream())
                .collect(Collectors.groupingBy(order -> simpleDateFormat.format(order.getTradeTime())));

        // 月度收益
        Map<String, BigDecimal> stockLotSizeMap = new HashMap<>();
        for (OwnerStrategy ownerStrategy : ownerStrategies) {
            stockLotSizeMap.put(ownerStrategy.getCode(), BigDecimal.valueOf(ownerStrategy.getLotSize()));
        }

        TreeMap<String, BigDecimal> monthlyIncome = new TreeMap<>();
        for (Map.Entry<String, List<OwnerOrder>> entry : monthOrder.entrySet()) {
            BigDecimal income = entry.getValue().stream()
                    .filter(order -> !order.getUnderlyingCode().equals(order.getCode()))
                    .map(order -> {
                        BigDecimal sign = new BigDecimal(TradeSide.of(order.getSide()).getSign());
                        BigDecimal quantity = new BigDecimal(order.getQuantity());
                        BigDecimal lotSize = stockLotSizeMap.getOrDefault(order.getUnderlyingCode(), BigDecimal.valueOf(100));
                        return order.getPrice()
                                .multiply(quantity)
                                .multiply(lotSize)
                                .multiply(sign)
                                .subtract(order.getOrderFee());
                    }).reduce(BigDecimal.ZERO, BigDecimal::add);
            monthlyIncome.put(entry.getKey(), income);
        }
        ownerSummary.setMonthlyIncome(monthlyIncome);

        // 获取账户信息
        OwnerAccount account = ownerManager.queryOwnerAccount(owner);
        ownerSummary.setAccountSize(account.getAccountSize());
        ownerSummary.setMarginRatio(account.getMarginRatio());

        // 计算PUT订单保证金占用和持有股票总成本
        BigDecimal putMarginOccupied = BigDecimal.ZERO;
        BigDecimal totalStockCost = BigDecimal.ZERO;
        for (StrategySummary strategySummary : strategySummaries) {
            putMarginOccupied = putMarginOccupied.add(strategySummary.getPutMarginOccupied());
            totalStockCost = totalStockCost.add(strategySummary.getTotalStockCost());
        }
        ownerSummary.setPutMarginOccupied(putMarginOccupied);
        ownerSummary.setTotalStockCost(totalStockCost);

        // 计算可用资金
        ownerSummary.setAvailableFunds(account.getAccountSize().subtract(putMarginOccupied).subtract(totalStockCost));
        ownerSummary.setTotalInvestment(putMarginOccupied.add(totalStockCost));
        return ownerSummary;
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

        OwnerAccount account = ownerManager.queryOwnerAccount(owner);
        // 订单费用
        BigDecimal totalFee = tradeManager.queryTotalOrderFee(account, ownerOrders);
        summary.setTotalFee(totalFee);


        // 股票现价
        Security security = Security.of(ownerStrategy.getCode(), account.getMarket());
        SecurityQuote securityQuote = securityQuoteGateway.quote(account, security);
        BigDecimal lastDone = securityQuote.getLastDone();
        summary.setCurrentStockPrice(lastDone);

        // 股票订单
        int holdStockNum = 0;
        BigDecimal totalStockCost = BigDecimal.ZERO;
        List<OwnerOrder> securityOrders = ownerOrders.stream().filter(order -> order.getCode().equals(ownerStrategy.getCode())).toList();
        for (OwnerOrder securityOrder : securityOrders) {
            TradeSide tradeSide = TradeSide.of(securityOrder.getSide());

            switch (tradeSide) {
                case BUY:
                case BUY_BACK:
                    holdStockNum += securityOrder.getQuantity();
                    totalStockCost = totalStockCost.add(securityOrder.getPrice().multiply(new BigDecimal(securityOrder.getQuantity())));
                    break;
                case SELL:
                case SELL_SHORT:
                    holdStockNum -= securityOrder.getQuantity();
                    totalStockCost = totalStockCost.subtract(securityOrder.getPrice().multiply(new BigDecimal(securityOrder.getQuantity())));
                    break;
                default:
                    break;
            }

        }
        // 股票持有数量
        summary.setHoldStockNum(holdStockNum);
        // 股票成本
        summary.setTotalStockCost(totalStockCost);
        BigDecimal averageStockCost = holdStockNum == 0 ? BigDecimal.ZERO : totalStockCost.divide(new BigDecimal(holdStockNum), 4, RoundingMode.HALF_UP);
        summary.setAverageStockCost(averageStockCost);


        // 期权总金额
        List<OwnerOrder> allOptionsOrders = ownerOrders.stream().filter(order -> OrderStatus.of(order.getStatus()).isTraded()).filter(order -> !order.getCode().equals(ownerStrategy.getCode())).toList();

        BigDecimal lotSize = new BigDecimal(ownerStrategy.getLotSize());
        // 所有期权利润
        BigDecimal allOptionsIncome = allOptionsOrders.stream().map(order -> {
            BigDecimal sign = new BigDecimal(TradeSide.of(order.getSide()).getSign());
            return order.getPrice().multiply(lotSize).multiply(BigDecimal.valueOf(order.getQuantity())).multiply(sign);
        }).reduce(BigDecimal.ZERO, BigDecimal::add);
        // 期权利润
        summary.setAllOptionsIncome(allOptionsIncome.subtract(totalFee));

        // 所有未平仓的期权利润
        BigDecimal unrealizedOptionsIncome = allOptionsOrders.stream().filter(order -> Boolean.FALSE.equals(Boolean.valueOf(order.getExt().get(OrderExt.IS_CLOSE.getCode())))).map(order -> {
            BigDecimal sign = new BigDecimal(TradeSide.of(order.getSide()).getSign());
            return order.getPrice().multiply(lotSize).multiply(BigDecimal.valueOf(order.getQuantity())).multiply(sign);
        }).reduce(BigDecimal.ZERO, BigDecimal::add);
        // 期权利润
        summary.setUnrealizedOptionsIncome(unrealizedOptionsIncome);

        // 计算PUT订单保证金占用
        BigDecimal putMarginOccupied = allOptionsOrders.stream()
                .filter(order -> Boolean.FALSE.equals(Boolean.valueOf(order.getExt().get(OrderExt.IS_CLOSE.getCode()))))
                .map(order -> {
                    String underlyingCode = order.getUnderlyingCode();
                    String s = order.getCode().split(underlyingCode)[1];
                    boolean isPut = s.contains("P");
                    BigDecimal result = new BigDecimal(0);
                    if (isPut) {
                        String[] split = s.split("P");
                        BigDecimal strikePrice = new BigDecimal(Long.parseLong(split[1]) / 1000);
                        result = strikePrice.multiply(lotSize).multiply(BigDecimal.valueOf(order.getQuantity()))
                                .multiply(account.getMarginRatio());
                    }
                    return result;

                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setPutMarginOccupied(putMarginOccupied);

        return summary;
    }

    @Override
    public SecurityOrderBook queryOrderBook(Security security) {
        return tradeManager.querySecurityOrderBook(security.getCode(), security.getMarket());
    }

    @Override
    public OptionsChain queryOptionsChain(String owner, Security security, OptionsStrikeDate optionsStrikeDate, OwnerStrategy strategy) {
        OwnerAccount ownerAccount = ownerManager.queryOwnerAccount(owner);
        OptionsChain optionsChain = optionsManager.queryOptionsChain(ownerAccount, security, optionsStrikeDate);

        // 计算策略数据
        calculateStrategyData(optionsStrikeDate, optionsChain, strategy);

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
     * @param strategy          策略(可选)
     */
    private void calculateStrategyData(OptionsStrikeDate optionsStrikeDate, OptionsChain optionsChain, OwnerStrategy strategy) {
        if (null == allOptionsStrategy || allOptionsStrategy.isEmpty()) {
            return;
        }
        for (OptionsStrategy optionsStrategy : allOptionsStrategy) {
            if (optionsStrategy.isSupport(strategy)) {
                StrategySummary strategySummary = null;
                if (null != strategy) {
                    strategySummary = queryStrategySummary(strategy.getOwner(), strategy.getStrategyId());
                }
                optionsStrategy.calculate(optionsStrikeDate, optionsChain, strategySummary);
                return;
            }
        }
    }


}
