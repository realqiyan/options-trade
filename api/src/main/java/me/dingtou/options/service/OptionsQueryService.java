package me.dingtou.options.service;

import me.dingtou.options.model.*;

import java.util.List;

/**
 * 期权查询服务
 *
 * @author qiyan
 */
public interface OptionsQueryService {

    /**
     * 查询owner信息（不包含订单）
     *
     * @param owner 所有者
     * @return owner信息
     */
    Owner queryOwner(String owner);

    /**
     * 查询owner的订单
     *
     * @param owner 所有者
     * @return 订单
     */
    Owner queryOwnerWithOrder(String owner);

    /**
     * 查询期权到期日列表
     *
     * @param security 底层资产
     * @return 期权到期日列表
     */
    List<OptionsStrikeDate> queryOptionsExpDate(Security security);

    /**
     * 查询市场实时盘口
     *
     * @param security 证券
     * @return 实时盘口
     */
    SecurityOrderBook queryOrderBook(Security security);

    /**
     * 查询期权链
     *
     * @param owner      所有者
     * @param security   底层资产
     * @param strikeDate 期权到期日
     * @param strategy   策略
     * @return 期权链
     */
    OptionsChain queryOptionsChain(String owner,
            Security security,
            String strikeDate,
            OwnerStrategy strategy);

    /**
     * 查询owner的草稿订单
     *
     * @param owner 所有者
     * @return 草稿订单
     */
    List<OwnerOrder> queryDraftOrder(String owner);

}
