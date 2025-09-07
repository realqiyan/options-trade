package me.dingtou.options.gateway;

import me.dingtou.options.constant.OptionsFilterType;
import me.dingtou.options.model.OptionsChain;
import me.dingtou.options.model.OptionsRealtimeData;
import me.dingtou.options.model.OptionsStrikeDate;
import me.dingtou.options.model.Security;

import java.math.BigDecimal;
import java.util.List;

/**
 * 期权链管理
 *
 * @author qiyan
 */
public interface OptionsChainGateway {

    /**
     * 获取期权到期时间线
     *
     * @param security 证券
     * @return 期权到期时间线
     */
    List<OptionsStrikeDate> getOptionsExpDate(Security security);

    /**
     * 查询期权链
     *
     * @param security   证券
     * @param strikeTime 行权时间
     * @param lastDone   最新价
     * @param filterType 期权过滤类型
     * @return 期权链
     */
    OptionsChain queryOptionsChain(Security security,
            String strikeTime,
            BigDecimal lastDone,
            OptionsFilterType filterType);

    /**
     * 查询期权实时数据
     *
     * @param optionsSecurityList 期权证券列表
     * @return 期权实时数据列表
     */
    List<OptionsRealtimeData> queryOptionsRealtimeData(List<Security> optionsSecurityList);
}
