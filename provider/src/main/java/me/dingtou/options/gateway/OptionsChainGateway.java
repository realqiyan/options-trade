package me.dingtou.options.gateway;

import me.dingtou.options.model.Options;
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
     * 查询期权列表
     *
     * @param security   证券
     * @param strikeTime 行权时间
     * @return 期权列表
     */
    List<Options> queryAllOptions(Security security, String strikeTime);

    /**
     * 查询期权链
     *
     * @param security   证券
     * @param strikeTime 行权时间
     * @param lastDone   最新价
     * @return 期权链
     */
    OptionsChain queryOptionsChain(Security security, String strikeTime, BigDecimal lastDone);

    /**
     * 查询期权实时数据
     *
     * @param optionsSecurityList 期权证券列表
     * @return 期权实时数据列表
     */
    List<OptionsRealtimeData> queryOptionsRealtimeData(List<Security> optionsSecurityList);
}
