package me.dingtou.options.strategy.trade;

import me.dingtou.options.model.*;
import me.dingtou.options.strategy.OptionsTradeStrategy;
import me.dingtou.options.util.NumberUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 基础策略
 *
 * @author qiyan
 */
public abstract class BaseTradeStrategy implements OptionsTradeStrategy {

    /**
     * Sell年化收益率
     */
    private static final BigDecimal SELL_ANNUAL_YIELD = BigDecimal.valueOf(15);
    /**
     * 最大delta
     */
    private static final BigDecimal MAX_DELTA = BigDecimal.valueOf(0.4);

    /**
     * 继续加工
     *
     * @param account        账户
     * @param optionsChain   期权链
     * @param summary        策略信息（可选）
     */
    abstract void process(OwnerAccount account,
            OptionsChain optionsChain,
            StrategySummary summary);

    @Override
    final public void calculate(OwnerAccount account, OptionsChain optionsChain,
            StrategySummary summary) {
        StockIndicator stockIndicator = optionsChain.getStockIndicator();
        SecurityQuote securityQuote = stockIndicator.getSecurityQuote();
        BigDecimal securityPrice = securityQuote.getLastDone();
        BigDecimal dte = new BigDecimal(optionsChain.dte());
        optionsChain.setTradeLevel(1);
        optionsChain.getOptionsList().forEach(options -> {
            OptionsStrategyData strategyData = new OptionsStrategyData();
            options.setStrategyData(strategyData);
            BigDecimal strikePrice = options.getOptionExData().getStrikePrice();

            // 计算卖出收益
            BigDecimal sellCallAnnualYield = calculateAnnualYield(options, securityPrice, dte);
            strategyData.setSellAnnualYield(sellCallAnnualYield);

            // 是否推荐卖出
            int level = 0;
            OptionsRealtimeData realtimeData = options.getRealtimeData();
            if (null != realtimeData) {
                BigDecimal delta = realtimeData.getDelta().abs();
                // delta小于0.30
                boolean deltaRecommend = delta.compareTo(MAX_DELTA) <= 0;
                // 卖出收益大于SELL_ANNUAL_YIELD
                boolean annualYieldRecommend = sellCallAnnualYield.compareTo(SELL_ANNUAL_YIELD) > 0;
                if (deltaRecommend && annualYieldRecommend) {
                    level++;
                    // 当前价+价格波幅 行权价大于这个值 level++
                    // 周价格波幅
                    // 计算股票价格加减一周波幅的范围
                    BigDecimal maxWeekPriceRange = securityPrice.add(stockIndicator.getWeekPriceRange());
                    BigDecimal minWeekPriceRange = securityPrice.subtract(stockIndicator.getWeekPriceRange());
                    // 如果期权行权价在一周波幅范围之外,说明行权价偏离当前价格较远,风险较小,level加1
                    if (maxWeekPriceRange.compareTo(options.getOptionExData().getStrikePrice()) < 0
                            || minWeekPriceRange.compareTo(options.getOptionExData().getStrikePrice()) > 0) {
                        level++;
                    }
                    // 月价格波幅
                    // 计算股票价格加减一月波幅的范围
                    BigDecimal maxMonthPriceRange = securityPrice.add(stockIndicator.getMonthPriceRange());
                    BigDecimal minMonthPriceRange = securityPrice.subtract(stockIndicator.getMonthPriceRange());
                    // 如果期权行权价在一月波幅范围之外,说明行权价偏离当前价格较远,风险较小,level加1
                    if (maxMonthPriceRange.compareTo(options.getOptionExData().getStrikePrice()) < 0
                            || minMonthPriceRange.compareTo(options.getOptionExData().getStrikePrice()) > 0) {
                        level++;
                    }
                }

                strategyData.setRecommendLevel(level);
                // 成交量大于0 且 delta不为0
                boolean noZero = realtimeData.getVolume() > 0 && !BigDecimal.ZERO.equals(realtimeData.getDelta());
                strategyData.setRecommend(deltaRecommend && annualYieldRecommend && noZero);
            }

            // 涨跌幅
            strategyData.setRange(calculateRange(strikePrice, securityPrice));

        });
        process(account, optionsChain, summary);
    }

    /**
     * //（期权合约价格 x lotSize）/（DTE） * 一年 365 天/（除以股价 x lotSize）
     *
     * @param options       期权
     * @param securityPrice 股票价格
     * @param dte           天数
     * @return 年化收益
     */
    private BigDecimal calculateAnnualYield(Options options, BigDecimal securityPrice, BigDecimal dte) {
        BigDecimal lotSize = new BigDecimal(options.getBasic().getLotSize());
        OptionsRealtimeData realtimeData = options.getRealtimeData();
        if (null == realtimeData || BigDecimal.ZERO.equals(securityPrice)) {
            return BigDecimal.ZERO;
        }
        BigDecimal curPrice = realtimeData.getCurPrice();
        BigDecimal income = curPrice.multiply(lotSize);
        BigDecimal fee = calculateFee(options.getBasic().getSecurity());
        BigDecimal afterIncome = income.subtract(fee);
        BigDecimal totalPrice = securityPrice.multiply(lotSize);
        // 到期日当天也计算持有日
        dte = dte.add(BigDecimal.ONE);
        if (dte.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return NumberUtils.scale(afterIncome
                .divide(dte, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal(365))
                .divide(totalPrice, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)));
    }

    private BigDecimal calculateFee(Security security) {
        // 使用富途手续费预估： 佣金1.99 平台使用费0.3 证监会规费 0.01 期权监管费 0.012 期权清算费 0.02 期权交手费 0.18
        return BigDecimal.valueOf(2.52);
    }

    private BigDecimal calculateRange(BigDecimal strikePrice, BigDecimal securityPrice) {
        if (BigDecimal.ZERO.equals(securityPrice)) {
            return BigDecimal.ZERO;
        }
        return NumberUtils.scale(strikePrice.subtract(securityPrice)
                .divide(securityPrice, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)));
    }
}
