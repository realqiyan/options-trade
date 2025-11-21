package me.dingtou.options.service;

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
import me.dingtou.options.constant.OptionsStrategy;
import me.dingtou.options.constant.OrderExt;
import me.dingtou.options.constant.OrderStatus;
import me.dingtou.options.constant.TradeSide;
import me.dingtou.options.gateway.SecurityQuoteGateway;
import me.dingtou.options.manager.KnowledgeManager;
import me.dingtou.options.manager.OptionsQueryManager;
import me.dingtou.options.manager.OwnerManager;
import me.dingtou.options.manager.TradeManager;
import me.dingtou.options.model.OptionsRealtimeData;
import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.model.OwnerKnowledge;
import me.dingtou.options.model.OwnerOrder;
import me.dingtou.options.model.OwnerOrderGroup;
import me.dingtou.options.model.OwnerPosition;
import me.dingtou.options.model.OwnerStrategy;
import me.dingtou.options.model.OwnerSummary;
import me.dingtou.options.model.Security;
import me.dingtou.options.model.SecurityQuote;
import me.dingtou.options.model.StockSummary;
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
    private KnowledgeManager knowledgeManager;

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
        BigDecimal allTotalStockCost = BigDecimal.ZERO;

        List<OwnerStrategy> ownerStrategies = ownerManager.queryOwnerStrategy(owner);
        List<StrategySummary> strategySummaries = new CopyOnWriteArrayList<>();
        // 批量拉取策略数据
        ownerStrategies.parallelStream().forEach(ownerStrategy -> {
            StrategySummary strategySummary = calculateStrategySummary(owner, ownerStrategy);
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
                        order.setExtValue(OrderExt.STRATEGY_AVG_DELTA, strategySummary.getAvgDelta());
                        order.setExtValue(OrderExt.STRATEGY_PROMPT, strategySummary.getStrategyPrompt());
                        unrealizedOrders.add(order);
                    });

            allHoldStockProfit = allHoldStockProfit.add(strategySummary.getHoldStockProfit());
            allIncome = allIncome.add(strategySummary.getAllIncome());
            allTotalStockCost = allTotalStockCost.add(strategySummary.getTotalStockCost());
            allOpenOptionsQuantity = allOpenOptionsQuantity.add(strategySummary.getOpenOptionsQuantity());
        }
        ownerSummary.setAllTotalStockCost(allTotalStockCost);
        ownerSummary.setAllOptionsIncome(allOptionsIncome);
        ownerSummary.setTotalFee(totalFee);
        ownerSummary.setUnrealizedOptionsIncome(unrealizedOptionsIncome);
        ownerSummary.setAllOpenOptionsQuantity(allOpenOptionsQuantity);
        strategySummaries.sort(new Comparator<StrategySummary>() {
            @Override
            public int compare(StrategySummary o1, StrategySummary o2) {
                // 优先按股票code排序，相同股票内按收益降序排列，最后按stage排序，将suspend状态的策略排在最后

                // 首先按股票code排序
                String code1 = o1.getStrategy().getCode();
                String code2 = o2.getStrategy().getCode();
                int codeCompare = code1.compareTo(code2);
                if (codeCompare != 0) {
                    return codeCompare;
                }

                // 相同股票代码内，先按suspend状态排序（suspend排在最后），再按收益降序排列
                String stage1 = o1.getStrategy().getStage();
                String stage2 = o2.getStrategy().getStage();
                boolean stage1IsSuspend = "suspend".equals(stage1);
                boolean stage2IsSuspend = "suspend".equals(stage2);

                // 如果一个是suspend而另一个不是，非suspend的排在前面
                if (stage1IsSuspend && !stage2IsSuspend) {
                    return 1; // o1是suspend，在相同股票内排在后面
                } else if (!stage1IsSuspend && stage2IsSuspend) {
                    return -1; // o2是suspend，在相同股票内排在后面
                }

                // 两者都是suspend或都不是suspend，按收益降序排列
                BigDecimal income1 = o1.getAllOptionsIncome() != null ? o1.getAllOptionsIncome() : BigDecimal.ZERO;
                BigDecimal income2 = o2.getAllOptionsIncome() != null ? o2.getAllOptionsIncome() : BigDecimal.ZERO;
                int incomeCompare = income2.compareTo(income1);
                if (incomeCompare != 0) {
                    return incomeCompare;
                }

                // 最后按stage排序
                return stage1.compareTo(stage2);
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
                BigDecimal holdStockCost = averageStockCost.multiply(new BigDecimal(initialStockNum));
                initStockAccountSize = initStockAccountSize.add(holdStockCost);
            }
            accountSize = accountSize.add(initStockAccountSize);
            ownerSummary.setAccountSize(accountSize);

            // 计算PUT订单保证金占用和持有股票总成本
            BigDecimal putMarginOccupied = BigDecimal.ZERO;
            BigDecimal holdStockCost = BigDecimal.ZERO;
            for (StrategySummary strategySummary : strategySummaries) {
                putMarginOccupied = putMarginOccupied.add(strategySummary.getPutMarginOccupied());
                holdStockCost = holdStockCost.add(strategySummary.getTotalStockCost());
            }
            ownerSummary.setPutMarginOccupied(putMarginOccupied);
            // 计算持有股票总成本(初始股票扣除)
            ownerSummary.setHoldStockCost(holdStockCost.add(initStockAccountSize));

            // 计算可用资金
            BigDecimal availableFunds = accountSize.subtract(putMarginOccupied)
                    .subtract(holdStockCost)
                    .subtract(initStockAccountSize);
            ownerSummary.setAvailableFunds(availableFunds);
            ownerSummary.setTotalInvestment(putMarginOccupied.add(holdStockCost));

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

        // 计算按股票汇总的收益明细
        List<StockSummary> stockSummaries = calculateStockSummaries(strategySummaries);
        ownerSummary.setStockSummaries(stockSummaries);

        return ownerSummary;
    }

    @Override
    public StrategySummary queryStrategySummary(String owner, String strategyId) {
        OwnerStrategy ownerStrategy = ownerStrategyDAO.queryStrategyByStrategyId(strategyId);
        if (ownerStrategy.getStatus() == 0) {
            return null;
        }
        return calculateStrategySummary(owner, ownerStrategy);
    }

    /**
     * 计算策略汇总
     * 
     * @param owner    所有者
     * @param strategy 策略
     * @return 策略汇总
     */
    private StrategySummary calculateStrategySummary(String owner, OwnerStrategy strategy) {
        StrategySummary summary = new StrategySummary();

        // 获取账号配置
        OwnerAccount account = ownerManager.queryOwnerAccount(owner);

        // 设置策略信息
        summary.setStrategy(strategy);

        // 设置策略知识
        String code = strategy.getStrategyCode();
        OwnerKnowledge optionsStrategy = knowledgeManager.getStrategyByOwnerAndCode(owner, code);
        summary.setOptionsStrategy(optionsStrategy);

        // 查询订单列表 所有收益都基于订单计算
        List<OwnerOrder> ownerOrders = ownerManager.queryStrategyOrder(strategy);
        summary.setStrategyOrders(ownerOrders);

        // 按平台订单号platformOrderId聚合订单收益信息
        Map<String, OwnerOrderGroup> orderGroups = calculateOrderGroups(ownerOrders);
        summary.setOrderGroups(orderGroups);

        // 订单费用
        List<OwnerOrder> securityOrders = ownerOrders.stream()
                .filter(OwnerOrder::isStockOrder)
                .toList();
        BigDecimal allOrderFee = tradeManager.queryTotalOrderFee(account, securityOrders);

        List<OwnerOrder> allOptionsOrders = ownerOrders.stream()
                .filter(order -> OrderStatus.of(order.getStatus()).isTraded())
                .filter(OwnerOrder::isOptionsOrder)
                .toList();
        BigDecimal allOptionsFee = tradeManager.queryTotalOrderFee(account, allOptionsOrders);

        summary.setTotalFee(allOrderFee.add(allOptionsFee));

        // 股票现价
        Security security = Security.of(strategy.getCode(), account.getMarket());
        SecurityQuote securityQuote = securityQuoteGateway.quote(account, security);
        BigDecimal lastDone = securityQuote.getLastDone();
        summary.setCurrentStockPrice(lastDone);

        // 计算持有股票数
        // 初始股票数&成本
        String initialStockNumStr = strategy.getExtValue(StrategyExt.INITIAL_STOCK_NUM, "0");
        int initialStockNum = Integer.parseInt(initialStockNumStr);
        String initialStockCostStr = strategy.getExtValue(StrategyExt.INITIAL_STOCK_COST, lastDone.toString());
        BigDecimal initialStockCost = new BigDecimal(initialStockCostStr);

        // 持有股票数 = 初始股票数 + 买入订单数量 - 卖出订单数量
        // 持股股票金额 = 初始股票成本 + 买入订单成本 - 卖出订单成本
        int holdStockNum = 0;
        BigDecimal holdStockCost = BigDecimal.ZERO;

        for (OwnerOrder securityOrder : securityOrders) {
            holdStockNum += OwnerOrder.tradeQuantity(securityOrder).intValue();
            holdStockCost = holdStockCost.subtract(OwnerOrder.income(securityOrder));
        }

        // 初始股票成本
        BigDecimal initStockCost = BigDecimal.ZERO;
        if (holdStockNum >= initialStockNum) {
            // 如果持股数大于初始股票数，则初始股票没有卖出，成本为当前股价*初始股票数
            initStockCost = lastDone.multiply(BigDecimal.valueOf(initialStockNum));
        } else {
            // 如果持股数小于初始股票数，则卖出的部分使用初始股票成本*已经卖出初始股票数 + 现价*没有卖出的初始股数量
            int sellInitialStockNum = initialStockNum - holdStockNum;
            BigDecimal holdInitStockCost = lastDone.multiply(BigDecimal.valueOf(holdStockNum));
            initStockCost = initialStockCost
                    .multiply(BigDecimal.valueOf(sellInitialStockNum))
                    .add(holdInitStockCost);
        }
        holdStockNum = holdStockNum + initialStockNum;
        holdStockCost = holdStockCost.add(initStockCost);

        // 股票平均成本 = 股票持有数量 / 股票数量
        BigDecimal averageStockCost = BigDecimal.ZERO;
        switch (OptionsStrategy.of(strategy.getCode())) {
            case CC_STRATEGY:
                averageStockCost = initialStockCost;
                break;
            default:
                if (holdStockNum != 0) {
                    averageStockCost = holdStockCost.divide(new BigDecimal(holdStockNum), 4, RoundingMode.HALF_UP);
                } else {
                    averageStockCost = lastDone;
                }
                break;
        }

        summary.setAverageStockCost(averageStockCost);

        // 总股票持有数量
        summary.setHoldStockNum(holdStockNum);

        // 股票花费（总花费-初始成本）
        BigDecimal totalStockCost = holdStockCost.subtract(initStockCost);
        summary.setTotalStockCost(totalStockCost);

        // 计算持股盈利（持股盈利需要扣除初始股票成本）
        BigDecimal profitPrice = lastDone.subtract(averageStockCost);
        BigDecimal holdStockProfit = profitPrice
                .multiply(BigDecimal.valueOf(holdStockNum));
        summary.setHoldStockProfit(holdStockProfit);

        // 期权订单

        BigDecimal lotSize = new BigDecimal(strategy.getLotSize());
        // 所有期权利润
        BigDecimal allOptionsIncome = allOptionsOrders.stream()
                .map(OwnerOrder::income)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        allOptionsIncome = allOptionsIncome.subtract(allOptionsFee);
        summary.setAllOptionsIncome(allOptionsIncome);

        // 总收入
        BigDecimal allTradeIncome = holdStockProfit.subtract(allOrderFee).add(allOptionsIncome);
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

        // 策略平均每股Delta avgDelta 未持股直接取期权整体Delta 否则取策略总Delta/持股数
        BigDecimal avgDelta = BigDecimal.ZERO;
        if (holdStockNum != 0) {
            BigDecimal holdStockNumBigDecimal = BigDecimal.valueOf(holdStockNum);
            avgDelta = strategyDelta.divide(holdStockNumBigDecimal, 4, RoundingMode.HALF_UP);
        } else if (!BigDecimal.ZERO.equals(openOptionsQuantity)) {
            avgDelta = optionsDelta.divide(openOptionsQuantity, 4, RoundingMode.HALF_UP);
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

        // 处理提示词
        processPrompt(summary, account, allOpenOptionsOrder);

        return summary;
    }

    /**
     * 处理提示词
     * 
     * @param summary             策略汇总
     * @param account             账户信息
     * @param allOpenOptionsOrder 所有未平仓期权订单
     */
    private void processPrompt(StrategySummary summary, OwnerAccount account, List<OwnerOrder> allOpenOptionsOrder) {
        // 未平仓订单处理策略
        OrderTradeStrategy defaultOrderTradeStrategy = new DefaultTradeStrategy(summary);
        for (OwnerOrder order : allOpenOptionsOrder) {
            defaultOrderTradeStrategy.calculate(account, order);
        }

        StringBuilder prompt = new StringBuilder();
        OwnerStrategy ownerStrategy = summary.getStrategy();
        prompt.append("请帮我对策略：").append(ownerStrategy.getStrategyName()).append(" 进行综合分析。\n")
                .append("策略ID：").append(ownerStrategy.getStrategyId())
                .append("，期权策略Code：").append(ownerStrategy.getStrategyCode())
                .append("，期权策略：").append(summary.getOptionsStrategy().getTitle())
                .append("，等价持股数：").append(summary.getStrategyDelta())
                .append("，策略delta（归一化）：").append(summary.getAvgDelta())
                .append("，请按照期权策略规则、期权策略详情和订单，以及其他你评估需要的信息，给我一些交易建议。");

        // 生成策略分析提示词
        summary.setStrategyPrompt(prompt.toString());
    }

    /**
     * 计算订单分组
     * 
     * @param ownerOrders 未平仓期权订单
     * @return 订单分组
     */
    private Map<String, OwnerOrderGroup> calculateOrderGroups(List<OwnerOrder> ownerOrders) {
        // 订单组聚合
        Map<String, List<OwnerOrder>> groupByPlatformOrderId = ownerOrders.stream()
                .filter(order -> order.getPlatformOrderId() != null)
                .collect(Collectors.groupingBy(OwnerOrder::getPlatformOrderId));

        // 订单组聚合统计
        Map<String, OwnerOrderGroup> orderGroups = new HashMap<>();
        for (Map.Entry<String, List<OwnerOrder>> entry : groupByPlatformOrderId.entrySet()) {
            String platformOrderId = entry.getKey();
            List<OwnerOrder> groupOrders = entry.getValue();
            // 累计收益
            BigDecimal totalIncome = groupOrders.stream()
                    .map(OwnerOrder::income)
                    .filter(Objects::nonNull)
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
        return orderGroups;
    }

    /**
     * 计算按股票汇总的收益明细
     * 
     * @param strategySummaries 策略汇总列表
     * @return 按股票汇总的收益明细
     */
    private List<StockSummary> calculateStockSummaries(List<StrategySummary> strategySummaries) {
        if (CollectionUtils.isEmpty(strategySummaries)) {
            return new ArrayList<>();
        }

        // 按股票代码分组
        Map<String, List<StrategySummary>> stockGroups = strategySummaries.stream()
                .collect(Collectors.groupingBy(summary -> summary.getStrategy().getCode()));

        List<StockSummary> stockSummaries = new ArrayList<>();
        for (Map.Entry<String, List<StrategySummary>> entry : stockGroups.entrySet()) {
            String stockCode = entry.getKey();
            List<StrategySummary> stockStrategies = entry.getValue();
            
            StockSummary stockSummary = new StockSummary();
            stockSummary.setStockCode(stockCode);
            stockSummary.setStrategyCount(stockStrategies.size());

            // 计算汇总数据
            BigDecimal totalOptionsIncome = BigDecimal.ZERO;
            BigDecimal totalStockProfit = BigDecimal.ZERO;
            BigDecimal totalIncome = BigDecimal.ZERO;
            BigDecimal totalFee = BigDecimal.ZERO;
            BigDecimal unrealizedOptionsIncome = BigDecimal.ZERO;
            Integer totalHoldStockNum = 0;
            BigDecimal totalHoldStockCost = BigDecimal.ZERO;
            BigDecimal totalAverageCost = BigDecimal.ZERO;

            for (StrategySummary strategySummary : stockStrategies) {
                if (strategySummary.getAllOptionsIncome() != null) {
                    totalOptionsIncome = totalOptionsIncome.add(strategySummary.getAllOptionsIncome());
                }
                if (strategySummary.getHoldStockProfit() != null) {
                    totalStockProfit = totalStockProfit.add(strategySummary.getHoldStockProfit());
                }
                if (strategySummary.getAllIncome() != null) {
                    totalIncome = totalIncome.add(strategySummary.getAllIncome());
                }
                if (strategySummary.getTotalFee() != null) {
                    totalFee = totalFee.add(strategySummary.getTotalFee());
                }
                if (strategySummary.getUnrealizedOptionsIncome() != null) {
                    unrealizedOptionsIncome = unrealizedOptionsIncome.add(strategySummary.getUnrealizedOptionsIncome());
                }
                if (strategySummary.getHoldStockNum() != null) {
                    totalHoldStockNum += strategySummary.getHoldStockNum();
                }
                if (strategySummary.getTotalStockCost() != null) {
                    totalHoldStockCost = totalHoldStockCost.add(strategySummary.getTotalStockCost());
                }
                if (strategySummary.getAverageStockCost() != null) {
                    totalAverageCost = totalAverageCost.add(strategySummary.getAverageStockCost());
                }
            }

            stockSummary.setTotalOptionsIncome(totalOptionsIncome);
            stockSummary.setStockProfit(totalStockProfit);
            stockSummary.setTotalIncome(totalIncome);
            stockSummary.setTotalFee(totalFee);
            stockSummary.setHoldStockNum(totalHoldStockNum);
           

            stockSummaries.add(stockSummary);
        }

        // 按股票代码排序
        stockSummaries.sort((s1, s2) -> s1.getStockCode().compareTo(s2.getStockCode()));

        return stockSummaries;
    }

}
