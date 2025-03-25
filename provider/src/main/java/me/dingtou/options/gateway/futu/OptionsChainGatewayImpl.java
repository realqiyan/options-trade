package me.dingtou.options.gateway.futu;

import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.gateway.OptionsChainGateway;
import me.dingtou.options.gateway.futu.executor.QueryExecutor;
import me.dingtou.options.gateway.futu.executor.func.query.FuncGetOptionsRealtimeData;
import me.dingtou.options.gateway.futu.executor.func.query.FuncGetOptionChain;
import me.dingtou.options.gateway.futu.executor.func.query.FuncGetOptionExpirationDate;
import me.dingtou.options.model.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Component
@Slf4j
public class OptionsChainGatewayImpl implements OptionsChainGateway {
    /**
     * 期权链的长度
     */
    private static final int STRIKE_DATE_SIZE = 8;
    /**
     * 期权链上下浮动的价格范围
     */
    private static final BigDecimal PRICE_RANGE = BigDecimal.valueOf(0.20);

    @Override
    public List<OptionsStrikeDate> getOptionsExpDate(Security security) {
        List<OptionsStrikeDate> strikeDateList = QueryExecutor
                .query(new FuncGetOptionExpirationDate(security.getMarket(), security.getCode()));
        if (null == strikeDateList || strikeDateList.isEmpty()) {
            return Collections.emptyList();
        }
        int min = Math.min(STRIKE_DATE_SIZE, strikeDateList.size());
        return strikeDateList.subList(0, min);
    }

    @Override
    public OptionsChain queryOptionsChain(Security security, String strikeTime, BigDecimal lastDone) {
        BigDecimal minStrikePrice = BigDecimal.ZERO;
        BigDecimal maxStrikePrice = BigDecimal.valueOf(Long.MAX_VALUE);
        if (null != lastDone && !BigDecimal.ZERO.equals(lastDone)) {
            minStrikePrice = lastDone.multiply(BigDecimal.ONE.subtract(PRICE_RANGE));
            maxStrikePrice = lastDone.multiply(BigDecimal.ONE.add(PRICE_RANGE));
        }

        List<Options> optionsList = QueryExecutor
                .query(new FuncGetOptionChain(security.getMarket(), security.getCode(), strikeTime));
        if (null == optionsList || optionsList.isEmpty()) {
            throw new RuntimeException("FuncGetOptionChain result null");
        }

        OptionsChain optionsChain = new OptionsChain();
        optionsChain.setStrikeTime(strikeTime);

        List<Options> chainOptionsList = new ArrayList<>();
        Set<Security> allSecurity = new HashSet<>();
        for (Options options : optionsList) {
            if (null == options) {
                continue;
            }

            Security optionsSecurity = options.getBasic().getSecurity();
            if (null == optionsSecurity) {
                continue;
            }

            BigDecimal strikePrice = options.getOptionExData().getStrikePrice();
            if (strikePrice.compareTo(minStrikePrice) >= 0 && strikePrice.compareTo(maxStrikePrice) <= 0) {
                allSecurity.add(optionsSecurity);
                chainOptionsList.add(options);
            }
        }
        optionsChain.setOptionsList(chainOptionsList);

        List<Security> securityList = new ArrayList<>(allSecurity);
        List<OptionsRealtimeData> optionsBasicInfo = QueryExecutor.query(new FuncGetOptionsRealtimeData(securityList));
        if (optionsBasicInfo.isEmpty()) {
            log.warn("query options basic info failed, retry");
            optionsBasicInfo = QueryExecutor.query(new FuncGetOptionsRealtimeData(securityList));
        }
        mergeRealtimeData(optionsChain, optionsBasicInfo, lastDone);
        return optionsChain;
    }

    private void mergeRealtimeData(OptionsChain optionsChain, List<OptionsRealtimeData> optionsBasicInfoList,
            BigDecimal lastDone) {
        Map<Security, OptionsRealtimeData> realtimeDataMap = new HashMap<>();
        for (OptionsRealtimeData optionsBasicInfo : optionsBasicInfoList) {
            realtimeDataMap.put(optionsBasicInfo.getSecurity(), optionsBasicInfo);
        }

        for (Options options : optionsChain.getOptionsList()) {
            if (null == options || null == options.getBasic() || null == options.getBasic().getSecurity()) {
                continue;
            }
            Security security = options.getBasic().getSecurity();
            OptionsRealtimeData optionsRealtimeData = realtimeDataMap.get(security);
            options.setRealtimeData(optionsRealtimeData);
            // 计算期权的时间价值和内在价值
            calculateOptionsValues(options, lastDone);
        }
    }

    /**
     * 计算期权链中所有期权的时间价值和内在价值
     * 
     * @param optionsChain 期权链
     */
    private void calculateOptionsValues(Options options, BigDecimal stockPrice) {
        if (options == null ||
                options.getRealtimeData() == null ||
                options.getOptionExData() == null) {
            return;
        }

        BigDecimal optionPrice = options.getRealtimeData().getCurPrice();
        BigDecimal strikePrice = options.getOptionExData().getStrikePrice();
        BigDecimal intrinsicValue = BigDecimal.ZERO;
        int optionType = options.getOptionExData().getType(); // 1为看涨，2为看跌

        if (optionType == 1) { // 看涨期权
            // 内在价值 = Max(0, 标的价格 - 行权价格)
            if (stockPrice.compareTo(strikePrice) > 0) {
                intrinsicValue = stockPrice.subtract(strikePrice);
            }
        } else if (optionType == 2) { // 看跌期权
            // 内在价值 = Max(0, 行权价格 - 标的价格)
            if (strikePrice.compareTo(stockPrice) > 0) {
                intrinsicValue = strikePrice.subtract(stockPrice);
            }
        }

        // 时间价值 = 期权价格 - 内在价值
        BigDecimal timeValue = optionPrice.subtract(intrinsicValue);

        // 设置计算结果到realtimeData中
        options.getRealtimeData().setIntrinsicValue(intrinsicValue);
        options.getRealtimeData().setTimeValue(timeValue);
    }
}
