package me.dingtou.options.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import me.dingtou.options.dao.OwnerStrategyDAO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import me.dingtou.options.constant.AccountExt;
import me.dingtou.options.constant.OrderStatus;
import me.dingtou.options.constant.TradeSide;
import me.dingtou.options.gateway.SecurityQuoteGateway;
import me.dingtou.options.manager.IndicatorManager;
import me.dingtou.options.manager.OwnerManager;
import me.dingtou.options.manager.TradeManager;
import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.model.OwnerOrder;
import me.dingtou.options.model.OwnerStrategy;
import me.dingtou.options.model.OwnerSummary;
import me.dingtou.options.model.Security;
import me.dingtou.options.model.SecurityQuote;
import me.dingtou.options.model.StockIndicator;
import me.dingtou.options.model.StrategySummary;
import me.dingtou.options.service.SummaryService;
import me.dingtou.options.strategy.OrderTradeStrategy;
import me.dingtou.options.strategy.order.DefaultOrderTradeStrategy;

import org.springframework.util.CollectionUtils;

@Service
public class SummaryServiceImpl implements SummaryService {

    @Autowired
    private OwnerManager ownerManager;

    @Autowired
    private TradeManager tradeManager;

    @Autowired
    private IndicatorManager indicatorManager;

    @Autowired
    private SecurityQuoteGateway securityQuoteGateway;

    @Autowired
    private OwnerStrategyDAO ownerStrategyDAO;

    @Override
    public OwnerSummary queryOwnerSummary(String owner) {
        OwnerSummary ownerSummary = new OwnerSummary();

        BigDecimal allOptionsIncome = BigDecimal.ZERO;
        BigDecimal totalFee = BigDecimal.ZERO;
        BigDecimal unrealizedOptionsIncome = BigDecimal.ZERO;

        List<OwnerStrategy> ownerStrategies = ownerManager.queryOwnerStrategyForSummary(owner);
        List<StrategySummary> strategySummaries = new CopyOnWriteArrayList<>();
        // 批量拉取策略数据
        ownerStrategies.parallelStream().forEach(ownerStrategy -> {
            StrategySummary strategySummary = queryStrategySummary(owner, ownerStrategy);
            strategySummaries.add(strategySummary);
        });
        strategySummaries.stream().filter(summary -> CollectionUtils.isEmpty(summary.getStrategyOrders()))
                .forEach(strategySummaries::remove);
        // 统计未平仓订单
        List<OwnerOrder> unrealizedOrders = new ArrayList<>();
        for (StrategySummary strategySummary : strategySummaries) {
            if (null == strategySummary.getAllOptionsIncome()) {
                continue;
            }
            allOptionsIncome = allOptionsIncome.add(strategySummary.getAllOptionsIncome());
            totalFee = totalFee.add(strategySummary.getTotalFee());
            unrealizedOptionsIncome = unrealizedOptionsIncome.add(strategySummary.getUnrealizedOptionsIncome());

            strategySummary.getStrategyOrders().stream()
                    .filter(OwnerOrder::isOpen)
                    .forEach(unrealizedOrders::add);
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
                    .filter(OwnerOrder::isOptionsOrder)
                    .filter(OwnerOrder::isTraded)
                    .map(order -> {
                        BigDecimal sign = new BigDecimal(TradeSide.of(order.getSide()).getSign());
                        BigDecimal quantity = new BigDecimal(order.getQuantity());
                        BigDecimal lotSize = stockLotSizeMap.get(order.getUnderlyingCode());
                        lotSize = null != lotSize ? lotSize : BigDecimal.valueOf(100);
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
        String accountSizeConf = account.getExtValue(AccountExt.ACCOUNT_SIZE);
        String marginRatioConf = account.getExtValue(AccountExt.MARGIN_RATIO);
        String positionRatioConf = account.getExtValue(AccountExt.POSITION_RATIO);

        if (StringUtils.isNotBlank(accountSizeConf) && StringUtils.isNotBlank(marginRatioConf)) {
            BigDecimal accountSize = new BigDecimal(accountSizeConf);
            BigDecimal marginRatio = new BigDecimal(marginRatioConf);
            BigDecimal positionRatio = StringUtils.isNotBlank(positionRatioConf) ? new BigDecimal(positionRatioConf)
                    : new BigDecimal("0.1");
            ownerSummary.setAccountSize(accountSize);
            ownerSummary.setMarginRatio(marginRatio);
            ownerSummary.setPositionRatio(positionRatio);

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
            ownerSummary.setAvailableFunds(accountSize.subtract(putMarginOccupied).subtract(totalStockCost));
            ownerSummary.setTotalInvestment(putMarginOccupied.add(totalStockCost));

            // 计算未平仓订单的头寸占比
            for (OwnerOrder order : ownerSummary.getUnrealizedOrders()) {
                // 计算订单金额
                BigDecimal orderAmount = OwnerOrder.strikePrice(order)
                        .multiply(new BigDecimal(OwnerOrder.lotSize(order)));
                // 计算该订单的头寸占比
                BigDecimal scaleRatio = orderAmount.divide(accountSize, 4, RoundingMode.HALF_UP);
                order.getExt().put("scaleRatio", scaleRatio.toString());
                // 添加头寸比例阈值
                order.getExt().put("positionRatio", positionRatio.toString());
            }
        }

        return ownerSummary;
    }

    public StrategySummary queryStrategySummary(String owner, OwnerStrategy ownerStrategy) {
        StrategySummary summary = new StrategySummary();

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
        List<OwnerOrder> securityOrders = ownerOrders.stream()
                .filter(OwnerOrder::isStockOrder)
                .toList();
        for (OwnerOrder securityOrder : securityOrders) {
            TradeSide tradeSide = TradeSide.of(securityOrder.getSide());
            BigDecimal totalPrice = securityOrder.getPrice().multiply(new BigDecimal(securityOrder.getQuantity()));
            switch (tradeSide) {
                case BUY:
                case BUY_BACK:
                    holdStockNum += securityOrder.getQuantity();
                    totalStockCost = totalStockCost.add(totalPrice);
                    break;
                case SELL:
                case SELL_SHORT:
                    holdStockNum -= securityOrder.getQuantity();
                    totalStockCost = totalStockCost.subtract(totalPrice);
                    break;
                default:
                    break;
            }

        }
        // 股票持有数量
        summary.setHoldStockNum(holdStockNum);
        // 股票成本
        summary.setTotalStockCost(totalStockCost);
        BigDecimal averageStockCost = holdStockNum == 0 ? BigDecimal.ZERO
                : totalStockCost.divide(new BigDecimal(holdStockNum), 4, RoundingMode.HALF_UP);
        summary.setAverageStockCost(averageStockCost);

        // 期权总金额
        List<OwnerOrder> allOptionsOrders = ownerOrders.stream()
                .filter(order -> OrderStatus.of(order.getStatus()).isTraded())
                .filter(OwnerOrder::isOptionsOrder)
                .toList();

        BigDecimal lotSize = new BigDecimal(ownerStrategy.getLotSize());
        // 所有期权利润
        BigDecimal allOptionsIncome = allOptionsOrders.stream()
                .map(OwnerOrder::income)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // 期权利润
        summary.setAllOptionsIncome(allOptionsIncome.subtract(totalFee));

        // 所有未平仓的期权利润
        BigDecimal unrealizedOptionsIncome = allOptionsOrders.stream()
                .filter(OwnerOrder::isOpen)
                .map(OwnerOrder::income)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // 期权利润
        summary.setUnrealizedOptionsIncome(unrealizedOptionsIncome);

        // 计算PUT订单保证金占用
        String marginRatioConfig = account.getExtValue(AccountExt.MARGIN_RATIO);

        if (null != marginRatioConfig) {
            BigDecimal marginRatio = new BigDecimal(marginRatioConfig);
            BigDecimal putMarginOccupied = allOptionsOrders.stream()
                    .filter(OwnerOrder::isOpen)
                    .filter(OwnerOrder::isOptionsOrder)
                    .filter(OwnerOrder::isSell)
                    .map(order -> {
                        BigDecimal result = new BigDecimal(0);
                        if (OwnerOrder.isPut(order)) {
                            BigDecimal strikePrice = OwnerOrder.strikePrice(order);
                            result = strikePrice.multiply(lotSize)
                                    .multiply(BigDecimal.valueOf(order.getQuantity()))
                                    .multiply(marginRatio);
                        }
                        return result;
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            summary.setPutMarginOccupied(putMarginOccupied);
        }

        // 计算未平仓订单的AI提示
        List<OwnerOrder> openOrders = ownerOrders.stream()
                .filter(OwnerOrder::isOpen)
                .filter(OwnerOrder::isOptionsOrder)
                .toList();
        // 未平仓订单处理策略
        OrderTradeStrategy defaultOrderTradeStrategy = new DefaultOrderTradeStrategy();
        for (OwnerOrder order : openOrders) {
            StockIndicator stockIndicator = indicatorManager.calculateStockIndicator(account, security);
            defaultOrderTradeStrategy.calculate(account, order, stockIndicator);
        }

        return summary;
    }

    @Override
    public StrategySummary queryStrategySummary(String owner, String strategyId) {
        OwnerStrategy ownerStrategy = ownerStrategyDAO.queryStrategyByStrategyId(strategyId);
        if (ownerStrategy.getStatus() == 0) {
            return null;
        }
        return queryStrategySummary(owner, ownerStrategy);
    }

}
