package me.dingtou.options.service.impl;

import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.manager.OptionsManager;
import me.dingtou.options.manager.OwnerManager;
import me.dingtou.options.manager.TradeManager;
import me.dingtou.options.model.*;
import me.dingtou.options.service.OptionsQueryService;
import me.dingtou.options.service.SummaryService;
import me.dingtou.options.strategy.OptionsTradeStrategy;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class OptionsQueryServiceImpl implements OptionsQueryService {

    @Autowired
    private SummaryService summaryService;

    @Autowired
    private List<OptionsTradeStrategy> allOptionsStrategy;

    @Autowired
    private OptionsManager optionsManager;

    @Autowired
    private OwnerManager ownerManager;

    @Autowired
    private TradeManager tradeManager;

    @Override
    public Owner queryOwner(String owner) {
        Owner ownerObj = ownerManager.queryOwner(owner);
        // 服务请求时屏蔽账户扩展信息和otp信息
        ownerObj.getAccount().setOtpAuth(StringUtils.EMPTY);
        ownerObj.getAccount().setExt(Collections.emptyMap());
        return ownerObj;
    }

    @Override
    public Owner queryOwnerWithOrder(String owner) {
        Owner ownerObj = ownerManager.queryOwnerWithOrder(owner);
        // 服务请求时屏蔽账户扩展信息和otp信息
        ownerObj.getAccount().setOtpAuth(StringUtils.EMPTY);
        ownerObj.getAccount().setExt(Collections.emptyMap());
        return ownerObj;
    }

    @Override
    public List<OptionsStrikeDate> queryOptionsExpDate(Security security) {
        return optionsManager.queryOptionsExpDate(security.getCode(), security.getMarket());
    }

    @Override
    public SecurityOrderBook queryOrderBook(Security security) {
        return tradeManager.querySecurityOrderBook(security.getCode(), security.getMarket());
    }

    @Override
    public OptionsChain queryOptionsChain(String owner,
            Security security,
            OptionsStrikeDate optionsStrikeDate,
            OwnerStrategy strategy) {
        OwnerAccount account = ownerManager.queryOwnerAccount(owner);
        OptionsChain optionsChain = optionsManager.queryOptionsChain(account, security, optionsStrikeDate);

        if (null == allOptionsStrategy || allOptionsStrategy.isEmpty()) {
            return optionsChain;
        }

        // 计算策略数据
        StrategySummary summary = null;
        if (null != strategy) {
            summary = summaryService.queryStrategySummary(strategy.getOwner(), strategy.getStrategyId());
        }
        for (OptionsTradeStrategy optionsStrategy : allOptionsStrategy) {
            if (optionsStrategy.isSupport(strategy)) {
                optionsStrategy.calculate(account, optionsStrikeDate, optionsChain, summary);
            }
        }

        return optionsChain;
    }

    @Override
    public List<OwnerOrder> queryDraftOrder(String owner) {
        return tradeManager.queryDraftOrder(owner);
    }

}
