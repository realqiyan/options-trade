package me.dingtou.options.gateway;

import me.dingtou.options.model.OptionsChain;
import me.dingtou.options.model.OptionsExpDate;

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
     * @param market 市场
     * @param code   证券代码
     * @return 期权到期时间线
     */
    List<OptionsExpDate> getOptionsExpDate(int market, String code);

    /**
     * 查询期权链
     *
     * @param market     市场
     * @param code       证券代码
     * @param strikeTime 行权时间
     * @return 期权链
     */
    OptionsChain queryOptionsChain(Integer market, String code, String strikeTime);
}
