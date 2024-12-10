package me.dingtou.options.service.impl;

import me.dingtou.options.constant.Market;
import me.dingtou.options.manager.OptionsManager;
import me.dingtou.options.model.*;
import me.dingtou.options.service.OptionsReadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class OptionsReadServiceImpl implements OptionsReadService {

    @Autowired
    private OptionsManager optionsManager;

    @Override
    public List<UnderlyingAsset> queryUnderlyingAsset(String owner) {
        //TODO database
        List<UnderlyingAsset> underlyingAssetList = new ArrayList<>(1);
        {
            UnderlyingAsset underlyingAsset = new UnderlyingAsset();
            underlyingAsset.setOwner(owner);
            underlyingAsset.setCode("BABA");
            underlyingAsset.setMarket(Market.US.getCode());
            underlyingAssetList.add(underlyingAsset);
        }
        {
            UnderlyingAsset underlyingAsset = new UnderlyingAsset();
            underlyingAsset.setOwner(owner);
            underlyingAsset.setCode("KWEB");
            underlyingAsset.setMarket(Market.US.getCode());
            underlyingAssetList.add(underlyingAsset);
        }
        {
            UnderlyingAsset underlyingAsset = new UnderlyingAsset();
            underlyingAsset.setOwner(owner);
            underlyingAsset.setCode("FXI");
            underlyingAsset.setMarket(Market.US.getCode());
            underlyingAssetList.add(underlyingAsset);
        }
        return underlyingAssetList;
    }

    @Override
    public List<OptionsExpDate> queryOptionsExpDate(UnderlyingAsset underlyingAsset) {
        return optionsManager.queryOptionsExpDate(underlyingAsset.getCode(), underlyingAsset.getMarket());
    }

    @Override
    public OptionsChain queryOptionsChain(UnderlyingAsset underlyingAsset, OptionsExpDate optionsExpDate) {
        OptionsChain optionsChain = optionsManager.queryOptionsChain(underlyingAsset.getCode(), underlyingAsset.getMarket(), optionsExpDate);

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
            if (call.getOptionExData().getStrikePrice().compareTo(securityPrice) > 0) {
                OptionsStrategyData callStrategyData = new OptionsStrategyData();
                BigDecimal sellCallAnnualYield = calculateAnnualYield(call, securityPrice, dte);
                callStrategyData.setSellAnnualYield(sellCallAnnualYield);
                call.setStrategyData(callStrategyData);
            }

            Options put = optionsTuple.getPut();
            if (put.getOptionExData().getStrikePrice().compareTo(securityPrice) < 0) {
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
        BigDecimal totalPrice = securityPrice.multiply(lotSize);
        return income
                .divide(dte, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal(365))
                .divide(totalPrice, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
