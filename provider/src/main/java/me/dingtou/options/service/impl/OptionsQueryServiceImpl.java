package me.dingtou.options.service.impl;

import me.dingtou.options.manager.OptionsManager;
import me.dingtou.options.manager.OwnerManager;
import me.dingtou.options.model.*;
import me.dingtou.options.service.OptionsQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;

@Service
public class OptionsQueryServiceImpl implements OptionsQueryService {

    @Autowired
    private OptionsManager optionsManager;

    @Autowired
    private OwnerManager ownerManager;

    @Override
    public Owner queryOwner(String owner) {
        return ownerManager.queryOwner(owner);
    }

    @Override
    public List<Security> querySecurity(String owner) {
        Owner ownerObj = this.queryOwner(owner);
        if (null == ownerObj || null == ownerObj.getSecurityList()) {
            return Collections.emptyList();
        }
        return ownerObj.getSecurityList();
    }

    @Override
    public List<OptionsExpDate> queryOptionsExpDate(Security security) {
        return optionsManager.queryOptionsExpDate(security.getCode(), security.getMarket());
    }

    @Override
    public OptionsChain queryOptionsChain(Security security, OptionsExpDate optionsExpDate) {
        OptionsChain optionsChain = optionsManager.queryOptionsChain(security.getCode(), security.getMarket(), optionsExpDate);

        // 计算策略数据
        calculateStrategyData(optionsChain, optionsExpDate);

        return optionsChain;
    }


    /**
     * 计算策略数据
     *
     * @param optionsChain   期权链
     * @param optionsExpDate 期权到期日
     */
    private void calculateStrategyData(OptionsChain optionsChain, OptionsExpDate optionsExpDate) {
        SecurityQuote securityQuote = optionsChain.getSecurityQuote();
        BigDecimal securityPrice = securityQuote.getLastDone();
        BigDecimal dte = new BigDecimal(optionsExpDate.getOptionExpiryDateDistance());
        optionsChain.getOptionList().forEach(optionsTuple -> {
            Options call = optionsTuple.getCall();
            if (null != call && call.getOptionExData().getStrikePrice().compareTo(securityPrice) > 0) {
                OptionsStrategyData callStrategyData = new OptionsStrategyData();
                BigDecimal sellCallAnnualYield = calculateAnnualYield(call, securityPrice, dte);
                callStrategyData.setSellAnnualYield(sellCallAnnualYield);
                call.setStrategyData(callStrategyData);
            }

            Options put = optionsTuple.getPut();
            if (null != put && put.getOptionExData().getStrikePrice().compareTo(securityPrice) < 0) {
                OptionsStrategyData putStrategyData = new OptionsStrategyData();
                BigDecimal sellPutAnnualYield = calculateAnnualYield(put, securityPrice, dte);
                putStrategyData.setSellAnnualYield(sellPutAnnualYield);
                put.setStrategyData(putStrategyData);
            }
        });
    }

    /**
     * //（期权合约价格 x lotSize）/（DTE） * 一年 365 天/（除以股价 x lotSize）
     *
     * @param options       期权
     * @param securityPrice 股票价格
     * @param dte           天数
     * @return 年化收益
     */
    private static BigDecimal calculateAnnualYield(Options options, BigDecimal securityPrice, BigDecimal dte) {
        BigDecimal lotSize = new BigDecimal(options.getBasic().getLotSize());
        OptionsRealtimeData realtimeData = options.getRealtimeData();
        if (null == realtimeData) {
            return BigDecimal.ZERO;
        }
        BigDecimal income = realtimeData.getCurPrice().multiply(lotSize);
        BigDecimal fee = calculateFee(options.getBasic().getSecurity());
        BigDecimal afterIncome = income.subtract(fee);
        BigDecimal totalPrice = securityPrice.multiply(lotSize);
        // 到期日当天也计算持有日
        dte = dte.add(BigDecimal.ONE);
        if (dte.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return afterIncome
                .divide(dte, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal(365))
                .divide(totalPrice, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal calculateFee(Security security) {
        // 富途手续费预估
        return BigDecimal.valueOf(2.52);
    }
}
