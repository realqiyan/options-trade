package me.dingtou.options.service;

import me.dingtou.options.model.OptionsChain;
import me.dingtou.options.model.OptionsExpDate;
import me.dingtou.options.model.UnderlyingAsset;

import java.util.List;

/**
 * 期权查询服务
 *
 * @author qiyan
 */
public interface OptionsReadService {

    /**
     * 查询底层资产列表
     *
     * @param owner 所有者
     * @return 底层资产列表
     */
    List<UnderlyingAsset> queryUnderlyingAsset(String owner);

    /**
     * 查询期权到期日列表
     *
     * @param underlyingAsset 底层资产
     * @return 期权到期日列表
     */
    List<OptionsExpDate> queryOptionsExpDate(UnderlyingAsset underlyingAsset);


    /**
     * 查询期权链
     *
     * @param underlyingAsset 底层资产
     * @param optionsExpDate  期权到期日
     * @return 期权链
     */
    OptionsChain queryOptionsChain(UnderlyingAsset underlyingAsset, OptionsExpDate optionsExpDate);
}
