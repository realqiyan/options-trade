package me.dingtou.options.manager;

import me.dingtou.options.gateway.OptionsChainGateway;
import me.dingtou.options.gateway.VixQueryGateway;
import me.dingtou.options.model.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.List;

@Component
public class OptionsManager {

    @Autowired
    private OptionsChainGateway optionsChainGateway;

    @Autowired
    private VixQueryGateway vixQueryGateway;

    @Autowired
    private IndicatorManager indicatorManager;

    /**
     * 查询期权到期日
     * 
     * @param code   股票代码
     * @param market 市场
     * @return 期权到期日
     */
    public List<OptionsStrikeDate> queryOptionsExpDate(String code, Integer market) {
        if (StringUtils.isBlank(code) || null == market) {
            return Collections.emptyList();
        }
        return optionsChainGateway.getOptionsExpDate(Security.of(code, market));
    }

    /**
     * 查询期权标的
     * 
     * @param security   股票
     * @param strikeDate 期权到期日
     * @return
     */
    public List<Options> queryAllOptions(Security security, String strikeDate) {
        return optionsChainGateway.queryAllOptions(security, strikeDate);
    }

    /**
     * 查询期权链
     * 
     * @param ownerAccount 账户
     * @param security     股票
     * @param strikeDate   期权到期日
     * @return 期权链
     */
    public OptionsChain queryOptionsChain(OwnerAccount ownerAccount,
            Security security,
            String strikeDate) {
        if (null == security || null == strikeDate) {
            return null;
        }

        // 策略分析提供基础指标数据
        StockIndicator stockIndicator = indicatorManager.calculateStockIndicator(ownerAccount, security);

        // 期权链
        OptionsChain optionsChain = optionsChainGateway.queryOptionsChain(security,
                strikeDate,
                stockIndicator.getSecurityQuote().getLastDone());
        optionsChain.setStockIndicator(stockIndicator);

        VixIndicator vixIndicator = vixQueryGateway.queryCurrentVix();
        optionsChain.setVixIndicator(vixIndicator);

        return optionsChain;
    }

    /**
     * 查询期权实时数据
     *
     * @param optionsSecurityList 期权证券列表
     * @return 期权实时数据列表
     */
    public List<OptionsRealtimeData> queryOptionsRealtimeData(List<Security> optionsSecurityList) {
        if (null == optionsSecurityList || optionsSecurityList.isEmpty()) {
            return Collections.emptyList();
        }
        return optionsChainGateway.queryOptionsRealtimeData(optionsSecurityList);
    }

}
