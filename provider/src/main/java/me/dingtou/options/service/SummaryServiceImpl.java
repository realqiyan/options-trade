package me.dingtou.options.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import me.dingtou.options.dao.OwnerStrategyDAO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import me.dingtou.options.constant.AccountExt;
import me.dingtou.options.constant.CandlestickPeriod;
import me.dingtou.options.constant.OrderExt;
import me.dingtou.options.constant.OrderStatus;
import me.dingtou.options.constant.TradeSide;
import me.dingtou.options.gateway.SecurityQuoteGateway;
import me.dingtou.options.manager.IndicatorManager;
import me.dingtou.options.manager.OptionsQueryManager;
import me.dingtou.options.manager.OwnerManager;
import me.dingtou.options.manager.TradeManager;
import me.dingtou.options.model.Options;
import me.dingtou.options.model.OptionsRealtimeData;
import me.dingtou.options.model.OptionsStrikeDate;
import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.model.OwnerOrder;
import me.dingtou.options.model.OwnerOrderGroup;
import me.dingtou.options.model.OwnerPosition;
import me.dingtou.options.model.OwnerStrategy;
import me.dingtou.options.model.OwnerSummary;
import me.dingtou.options.model.Security;
import me.dingtou.options.model.SecurityQuote;
import me.dingtou.options.model.StockIndicator;
import me.dingtou.options.model.StrategyExt;
import me.dingtou.options.model.StrategySummary;
import me.dingtou.options.strategy.OrderTradeStrategy;
import me.dingtou.options.strategy.order.DefaultTradeStrategy;

import org.springframework.util.CollectionUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SummaryServiceImpl implements SummaryService {

    @Autowired
    private OwnerManager ownerManager;

    @Autowired
    private TradeManager tradeManager;

    @Autowired
    private OptionsQueryManager optionsQueryManager;

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
        BigDecimal allHoldStockProfit = BigDecimal.ZERO;
        BigDecimal allOpenOptionsQuantity = BigDecimal.ZERO;
        BigDecimal allIncome = BigDecimal.ZERO;

        List<OwnerStrategy> ownerStrategies = ownerManager.queryOwnerStrategy(owner);
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
                    .filter(OwnerOrder::isTraded)
                    .filter(OwnerOrder::isOptionsOrder)
                    .filter(order -> OrderStatus.of(order.getStatus()).isValid())
                    .forEach(order -> {
                        order.setExtValue(OrderExt.STRATEGY_ID, strategySummary.getStrategy().getStrategyId());
                        order.setExtValue(OrderExt.STRATEGY_NAME, strategySummary.getStrategy().getStrategyName());
                        unrealizedOrders.add(order);
                    });

            allHoldStockProfit = allHoldStockProfit.add(strategySummary.getHoldStockProfit());
            allIncome = allIncome.add(strategySummary.getAllIncome());
            allOpenOptionsQuantity = allOpenOptionsQuantity.add(strategySummary.getOpenOptionsQuantity());
        }

        ownerSummary.setAllOptionsIncome(allOptionsIncome);
        ownerSummary.setTotalFee(totalFee);
        ownerSummary.setUnrealizedOptionsIncome(unrealizedOptionsIncome);
        ownerSummary.setAllOpenOptionsQuantity(allOpenOptionsQuantity);
        strategySummaries.sort(new Comparator<StrategySummary>() {
            @Override
            public int compare(StrategySummary o1, StrategySummary o2) {
                return o2.getAllOptionsIncome().compareTo(o1.getAllOptionsIncome());
            }
        });
        ownerSummary.setStrategySummaries(strategySummaries);
        ownerSummary.setUnrealizedOrders(unrealizedOrders);
        ownerSummary.setAllHoldStockProfit(allHoldStockProfit);
        ownerSummary.setAllIncome(allIncome);

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
                    .map(OwnerOrder::income)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            monthlyIncome.put(entry.getKey(), income);
        }
        ownerSummary.setMonthlyIncome(monthlyIncome);

        // 获取账户信息
        OwnerAccount account = ownerManager.queryOwnerAccount(owner);
        String accountSizeConf = account.getExtValue(AccountExt.ACCOUNT_SIZE, null);
        String marginRatioConf = account.getExtValue(AccountExt.MARGIN_RATIO, null);
        String positionRatioConf = account.getExtValue(AccountExt.POSITION_RATIO, "0.1");

        if (StringUtils.isNotBlank(accountSizeConf) && StringUtils.isNotBlank(marginRatioConf)) {
            BigDecimal accountSize = new BigDecimal(accountSizeConf);
            BigDecimal marginRatio = new BigDecimal(marginRatioConf);
            BigDecimal positionRatio = new BigDecimal(positionRatioConf);

            ownerSummary.setMarginRatio(marginRatio);
            ownerSummary.setPositionRatio(positionRatio);

            // 根据初始股票数和平均股价计算accountSize
            BigDecimal initStockAccountSize = BigDecimal.ZERO;
            for (StrategySummary strategySummary : strategySummaries) {
                OwnerStrategy strategy = strategySummary.getStrategy();
                int initialStockNum = Integer.parseInt(strategy.getExtValue(StrategyExt.INITIAL_STOCK_NUM, "0"));
                BigDecimal averageStockCost = strategySummary.getAverageStockCost();
                BigDecimal totalStockCost = averageStockCost.multiply(new BigDecimal(initialStockNum));
                initStockAccountSize = initStockAccountSize.add(totalStockCost);
            }
            accountSize = accountSize.add(initStockAccountSize);
            ownerSummary.setAccountSize(accountSize);

            // 计算PUT订单保证金占用和持有股票总成本
            BigDecimal putMarginOccupied = BigDecimal.ZERO;
            BigDecimal totalStockCost = BigDecimal.ZERO;
            for (StrategySummary strategySummary : strategySummaries) {
                putMarginOccupied = putMarginOccupied.add(strategySummary.getPutMarginOccupied());
                totalStockCost = totalStockCost.add(strategySummary.getTotalStockCost());
            }
            ownerSummary.setPutMarginOccupied(putMarginOccupied);
            // 计算持有股票总成本(初始股票扣除)
            ownerSummary.setTotalStockCost(totalStockCost.add(initStockAccountSize));

            // 计算可用资金
            BigDecimal availableFunds = accountSize.subtract(putMarginOccupied)
                    .subtract(totalStockCost)
                    .subtract(initStockAccountSize);
            ownerSummary.setAvailableFunds(availableFunds);
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

        // 获取持仓
        List<String> stockCodes = strategySummaries.stream()
                .map(item -> item.getStrategy().getCode())
                .collect(Collectors.toList());
        List<OwnerPosition> ownerPositionList = tradeManager.queryOwnerPosition(account, stockCodes);
        ownerSummary.setPositions(ownerPositionList);

        return ownerSummary;
    }

    @Override
    public StrategySummary queryStrategySummary(String owner, String strategyId) {
        OwnerStrategy ownerStrategy = ownerStrategyDAO.queryStrategyByStrategyId(strategyId);
        if (ownerStrategy.getStatus() == 0) {
            return null;
        }
        return queryStrategySummary(owner, ownerStrategy);
    }

    /**
     * 获取下一个期权到期日
     * 
     * @param optionsStrikeDates 期权到期日列表
     * @param order              订单
     * @return 下一个期权到期日
     */
    private LocalDate getNextWeekOptionsStrikeDate(List<OptionsStrikeDate> optionsStrikeDates, OwnerOrder order) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String strikeDateStr = dateFormat.format(order.getStrikeTime());
        final LocalDate strikeDate = LocalDate.parse(strikeDateStr);
        return optionsStrikeDates.stream()
                .map(dateObj -> {
                    String strikeTime = dateObj.getStrikeTime();
                    LocalDate orderStrikeDate = LocalDate.parse(strikeTime);
                    return orderStrikeDate;
                })
                .filter(date -> date.isAfter(strikeDate))
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取下一个月度期权到期日
     * 
     * @param optionsStrikeDates 期权到期日列表
     * @param order              订单
     * @return 下一个月度期权到期日
     */
    private LocalDate getNextMonthOptionsStrikeDate(List<OptionsStrikeDate> optionsStrikeDates, OwnerOrder order) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String strikeDateStr = dateFormat.format(order.getStrikeTime());
        final LocalDate strikeDate = LocalDate.parse(strikeDateStr);
        return optionsStrikeDates.stream()
                .map(dateObj -> LocalDate.parse(dateObj.getStrikeTime()))
                .filter(date -> date.isAfter(strikeDate))
                .filter(this::isThirdFriday)
                .min(LocalDate::compareTo)
                .orElse(null);
    }

    /**
     * 判断日期是否为当月的第三个周五
     * 
     * @param date 待检查的日期
     * @return 如果是第三个周五返回true，否则返回false
     */
    private boolean isThirdFriday(LocalDate date) {
        if (date.getDayOfWeek() != DayOfWeek.FRIDAY) {
            return false;
        }
        LocalDate firstDayOfMonth = date.withDayOfMonth(1);
        LocalDate firstFriday = firstDayOfMonth.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));
        LocalDate thirdFriday = firstFriday.plusWeeks(2);
        return date.equals(thirdFriday);
    }

    /**
     * 获取可以Roll的期权
     * 
     * @param order          订单
     * @param nextStrikeDate 下一个期权到期日
     * @return 可以Roll的期权
     */
    private List<Security> getRollOptionsSecurity(OwnerOrder order, LocalDate nextStrikeDate) {
        List<Security> optionsSecurityList = new ArrayList<>();

        BigDecimal strikePrice = OwnerOrder.strikePrice(order);
        // 查询下一个期权到期日
        int range = 5;
        BigDecimal startPrice = strikePrice.subtract(BigDecimal.valueOf(range));
        for (int i = 0; i <= range * 2; i++) {
            BigDecimal nextStrikePrice = startPrice
                    .add(BigDecimal.valueOf(i))
                    .multiply(BigDecimal.valueOf(1000))
                    .setScale(0);
            // BABA250404P133000
            String date = nextStrikeDate.format(DateTimeFormatter.ofPattern("yyMMdd"));
            String code = order.getUnderlyingCode()
                    + date
                    + (OwnerOrder.isPut(order) ? "P" : "C")
                    + nextStrikePrice.toPlainString();
            optionsSecurityList.add(Security.of(code, order.getMarket()));
        }

        // OptionsManager
        return optionsSecurityList;
    }

    /**
     * 查询策略汇总
     * 
     * @param owner         所有者
     * @param ownerStrategy 策略
     * @return 策略汇总
     */
    private StrategySummary queryStrategySummary(String owner, OwnerStrategy ownerStrategy) {
        StrategySummary summary = new StrategySummary();

        summary.setStrategy(ownerStrategy);

        // 订单列表
        List<OwnerOrder> ownerOrders = ownerManager.queryStrategyOrder(ownerStrategy);
        summary.setStrategyOrders(ownerOrders);

        // 订单组聚合
        Map<String, List<OwnerOrder>> groupByPlatformOrderId = ownerOrders.stream()
                .filter(order -> order.getPlatformOrderId() != null)
                .collect(Collectors.groupingBy(OwnerOrder::getPlatformOrderId));
        Map<String, OwnerOrderGroup> orderGroups = new HashMap<>();
        for (Map.Entry<String, List<OwnerOrder>> entry : groupByPlatformOrderId.entrySet()) {
            String platformOrderId = entry.getKey();
            List<OwnerOrder> groupOrders = entry.getValue();
            // 累计收益
            BigDecimal totalIncome = groupOrders.stream()
                    .map(OwnerOrder::income)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            // 累计手续费
            BigDecimal totalOrderFee = groupOrders.stream()
                    .map(OwnerOrder::getOrderFee)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            OwnerOrderGroup group = new OwnerOrderGroup(platformOrderId);
            group.setTotalIncome(totalIncome);
            group.setTotalOrderFee(totalOrderFee);
            group.setOrderCount(groupOrders.size());
            orderGroups.put(platformOrderId, group);
        }
        summary.setOrderGroups(orderGroups);

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
        int orderHoldStockNum = 0;
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
                    orderHoldStockNum += securityOrder.getQuantity();
                    totalStockCost = totalStockCost.add(totalPrice);
                    break;
                case SELL:
                case SELL_SHORT:
                    orderHoldStockNum -= securityOrder.getQuantity();
                    totalStockCost = totalStockCost.subtract(totalPrice);
                    break;
                default:
                    break;
            }

        }

        // 初始股票数&成本
        int initialStockNum = Integer.parseInt(ownerStrategy.getExtValue(StrategyExt.INITIAL_STOCK_NUM, "0"));
        BigDecimal initialStockCost = new BigDecimal(ownerStrategy.getExtValue(StrategyExt.INITIAL_STOCK_COST, "0"));

        // 股票成本
        summary.setTotalStockCost(totalStockCost);

        BigDecimal averageStockCost = BigDecimal.ZERO;
        // 股票平均成本 = 系统设置 or 股票持有数量 / 股票数量
        if (orderHoldStockNum == 0) {
            averageStockCost = initialStockCost;
        } else {
            averageStockCost = totalStockCost.divide(new BigDecimal(orderHoldStockNum), 4, RoundingMode.HALF_UP);
        }

        // 如果平均成本为0，则使用当前价格
        if (BigDecimal.ZERO.equals(averageStockCost)) {
            averageStockCost = lastDone;
        }

        // 如果交易产生了成本或收益 则往平均成本上累计
        if (initialStockNum != 0 && !BigDecimal.ZERO.equals(totalStockCost)) {
            BigDecimal avgCost = totalStockCost.divide(BigDecimal.valueOf(initialStockNum), 4, RoundingMode.HALF_UP);
            averageStockCost = averageStockCost.add(avgCost);
        }

        summary.setAverageStockCost(averageStockCost);

        // 总股票持有数量
        int holdStockNum = initialStockNum + orderHoldStockNum;
        summary.setHoldStockNum(holdStockNum);

        int holdStockNumForProfit = holdStockNum;
        // 当初始股票被卖后 holdStockNum 会小于 initialStockNum
        // 当holdStockNumForProfit小于0时，不计算持股收益
        if (holdStockNumForProfit < 0) {
            holdStockNumForProfit = 0;
        }

        // 持有股票价格
        BigDecimal holdStockPrice = lastDone;

        // 期权订单
        List<OwnerOrder> allOptionsOrders = ownerOrders.stream()
                .filter(order -> OrderStatus.of(order.getStatus()).isTraded())
                .filter(OwnerOrder::isOptionsOrder)
                .toList();

        // 卖空订单
        List<OwnerOrder> sellCallOrders = allOptionsOrders.stream()
                .filter(OwnerOrder::isSell)
                .filter(OwnerOrder::isOpen)
                .filter(OwnerOrder::isCall)
                .toList();

        // 计算持股盈利
        BigDecimal holdStockProfit = BigDecimal.ZERO;
        BigDecimal holdStockNumForProfitLoop = BigDecimal.valueOf(holdStockNumForProfit);
        for (OwnerOrder ownerOrder : sellCallOrders) {
            BigDecimal strikePrice = OwnerOrder.strikePrice(ownerOrder);
            BigDecimal currLotSize = BigDecimal.valueOf(OwnerOrder.lotSize(ownerOrder));
            // 卖空订单小于holdStockPrice 并且剩余持有数量大于当前订单的lotSize时
            if (strikePrice.compareTo(holdStockPrice) <= 0 && holdStockNumForProfitLoop.compareTo(currLotSize) >= 0) {
                BigDecimal currHoldStockProfit = strikePrice.subtract(averageStockCost).multiply(currLotSize);
                holdStockProfit = holdStockProfit.add(currHoldStockProfit);
                holdStockNumForProfitLoop = holdStockNumForProfitLoop.subtract(currLotSize);
            }
        }
        BigDecimal otherHoldStockProfit = holdStockPrice.subtract(averageStockCost).multiply(holdStockNumForProfitLoop);
        holdStockProfit = holdStockProfit.add(otherHoldStockProfit);
        summary.setHoldStockProfit(holdStockProfit);

        BigDecimal lotSize = new BigDecimal(ownerStrategy.getLotSize());
        // 所有期权利润
        BigDecimal allOptionsIncome = allOptionsOrders.stream()
                .map(OwnerOrder::income)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setAllOptionsIncome(allOptionsIncome);

        // 所有期权利润会有误差，后续关注交易收入，否则指派后卖出股票和期权时会多计算盈利。
        BigDecimal allTradeIncome = ownerOrders.stream()
                .map(OwnerOrder::income)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        allTradeIncome = allTradeIncome.subtract(totalFee);
        if (holdStockNum < initialStockNum) {
            // 卖掉的初始本金不算盈利
            int sellNum = initialStockNum - holdStockNum;
            BigDecimal principal = initialStockCost.multiply(BigDecimal.valueOf(sellNum));
            allTradeIncome = allTradeIncome.subtract(principal);
        }
        // 总收入
        summary.setAllIncome(allTradeIncome);

        // 所有未平仓的期权
        List<OwnerOrder> allOpenOptionsOrder = allOptionsOrders.stream()
                .filter(OwnerOrder::isOpen)
                .filter(OwnerOrder::isTraded)
                .filter(OwnerOrder::isOptionsOrder)
                .filter(order -> OrderStatus.of(order.getStatus()).isValid())
                .toList();

        // 未平仓的期权利润
        BigDecimal unrealizedOptionsIncome = allOpenOptionsOrder.stream()
                .map(OwnerOrder::income)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // 期权利润
        summary.setUnrealizedOptionsIncome(unrealizedOptionsIncome);

        // 获取期权Delta
        List<Security> allOpenOptionsSecurity = allOpenOptionsOrder.stream()
                .map(order -> Security.of(order.getCode(), order.getMarket()))
                .toList();
        List<OptionsRealtimeData> allOpenOptionsRealtimeData = optionsQueryManager
                .queryOptionsRealtimeData(allOpenOptionsSecurity);
        // 计算Delta
        Map<Security, OptionsRealtimeData> securityDeltaMap = new HashMap<>();
        for (OptionsRealtimeData realtimeData : allOpenOptionsRealtimeData) {
            securityDeltaMap.put(realtimeData.getSecurity(), realtimeData);
        }
        // 计算未平仓期权的Delta
        BigDecimal optionsDelta = BigDecimal.ZERO;
        BigDecimal optionsGamma = BigDecimal.ZERO;
        BigDecimal optionsTheta = BigDecimal.ZERO;
        BigDecimal openOptionsQuantity = BigDecimal.ZERO;
        for (OwnerOrder order : allOpenOptionsOrder) {
            OptionsRealtimeData realtimeData = securityDeltaMap.get(Security.of(order.getCode(), order.getMarket()));
            if (null == realtimeData) {
                continue;
            }
            BigDecimal quantity = new BigDecimal(order.getQuantity());
            // 卖出为负 买入为正
            BigDecimal side = new BigDecimal(TradeSide.of(order.getSide()).getSign() * -1);

            // 深度虚值或深度实值期权的delta API返回可能为0
            BigDecimal currentDelta = realtimeData.getDelta();
            if (BigDecimal.ZERO.equals(currentDelta)) {
                // 深度虚值时Delta设置为0 深度实值时Delta设置为1
                if (realtimeData.getCurPrice().compareTo(new BigDecimal("0.01")) <= 0) {
                    currentDelta = BigDecimal.ZERO;
                } else if (realtimeData.getCurPrice().compareTo(new BigDecimal("1")) > 0) {
                    currentDelta = BigDecimal.ONE;
                }

            }

            BigDecimal delta = currentDelta.multiply(side).multiply(quantity);
            BigDecimal gamma = realtimeData.getGamma().multiply(side).multiply(quantity);
            BigDecimal theta = realtimeData.getTheta().multiply(side).multiply(quantity);

            optionsDelta = optionsDelta.add(delta);
            optionsGamma = optionsGamma.add(gamma);
            optionsTheta = optionsTheta.add(theta);
            openOptionsQuantity = openOptionsQuantity.add(quantity);
        }
        // 策略Gamma(未平仓期权Delta)
        summary.setOptionsDelta(optionsDelta);
        // 策略Gamma(未平仓期权Gamma)
        summary.setOptionsGamma(optionsGamma);
        // 策略Theta(未平仓期权Theta)
        summary.setOptionsTheta(optionsTheta);
        // 策略期权合约数
        summary.setOpenOptionsQuantity(openOptionsQuantity);

        // 股票Delta
        BigDecimal stockDelta = BigDecimal.valueOf(holdStockNum);

        // 策略Delta
        BigDecimal strategyDelta = stockDelta.add(optionsDelta.multiply(lotSize));
        summary.setStrategyDelta(strategyDelta);

        // avgDelta 未持股直接取期权整体Delta 否则取策略总Delta/持股数
        BigDecimal avgDelta = BigDecimal.ZERO;
        if (holdStockNum != 0) {
            BigDecimal holdStockNumBigDecimal = BigDecimal.valueOf(holdStockNum);
            avgDelta = strategyDelta.divide(holdStockNumBigDecimal, 2, RoundingMode.HALF_UP);
        } else if (!BigDecimal.ZERO.equals(openOptionsQuantity)) {
            avgDelta = optionsDelta.divide(openOptionsQuantity, 2, RoundingMode.HALF_UP);
        }
        summary.setAvgDelta(avgDelta);

        // 计算PUT订单保证金占用
        String marginRatioConfig = account.getExtValue(AccountExt.MARGIN_RATIO, null);

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

        // 未平仓订单处理策略
        OrderTradeStrategy defaultOrderTradeStrategy = new DefaultTradeStrategy(summary);
        for (OwnerOrder order : allOpenOptionsOrder) {

            // 查询未平仓订单可以Roll的期权实时数据
            // 查询股票期权到期日
            Security currentSecurity = Security.of(order.getUnderlyingCode(), order.getMarket());
            List<OptionsStrikeDate> optionsStrikeDates = optionsQueryManager.queryOptionsExpDate(order.getUnderlyingCode(),
                    order.getMarket());
            List<Options> allExistsOptions = new ArrayList<>();
            List<Security> optionsSecurityList = new ArrayList<>();
            optionsSecurityList.add(Security.of(order.getCode(), order.getMarket()));
            LocalDate weekStrikeDate = getNextWeekOptionsStrikeDate(optionsStrikeDates, order);
            if (null != weekStrikeDate) {
                List<Security> weekOptionsSecurityList = getRollOptionsSecurity(order, weekStrikeDate);
                optionsSecurityList.addAll(weekOptionsSecurityList);
                allExistsOptions.addAll(optionsQueryManager.queryAllOptions(currentSecurity, weekStrikeDate.toString()));
            }
            LocalDate monthStrikeDate = getNextMonthOptionsStrikeDate(optionsStrikeDates, order);
            if (null != monthStrikeDate) {
                List<Security> monthOptionsSecurityList = getRollOptionsSecurity(order, monthStrikeDate);
                optionsSecurityList.addAll(monthOptionsSecurityList);
                allExistsOptions.addAll(optionsQueryManager.queryAllOptions(currentSecurity, monthStrikeDate.toString()));
            }
            // 过滤存在的期权
            optionsSecurityList = filterExistsOptions(optionsSecurityList, allExistsOptions);

            List<OptionsRealtimeData> optionsRealtimeDataList = optionsQueryManager
                    .queryOptionsRealtimeData(optionsSecurityList);
            order.setExtValue(OrderExt.ROLL_OPTIONS, optionsRealtimeDataList);
            CandlestickPeriod klinePeriod = me.dingtou.options.util.AccountExtUtils.getKlinePeriod(account);
            StockIndicator stockIndicator = indicatorManager.calculateStockIndicator(account, security, klinePeriod, 30);
            defaultOrderTradeStrategy.calculate(account, order, stockIndicator);
        }

        return summary;
    }

    /**
     * 过滤真实存在的标的
     * 
     * @param optionsSecurityList 期权标的
     * @param allExistsOptions    真实存在的
     * @return
     */
    private List<Security> filterExistsOptions(List<Security> optionsSecurityList, List<Options> allExistsOptions) {
        return optionsSecurityList.stream()
                .filter(security -> allExistsOptions.stream()
                        .anyMatch(option -> option.getBasic().getSecurity().getCode().equals(security.getCode())))
                .collect(Collectors.toList());
    }

}
