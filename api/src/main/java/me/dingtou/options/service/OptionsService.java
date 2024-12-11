package me.dingtou.options.service;

import me.dingtou.options.model.OptionsChain;
import me.dingtou.options.model.OptionsExpDate;
import me.dingtou.options.model.Security;

import java.util.List;

/**
 * 期权查询服务
 *
 * @author qiyan
 */
public interface OptionsService {

    /**
     * 查询底层资产列表
     *
     * @param owner 所有者
     * @return 底层资产列表
     */
    List<Security> querySecurity(String owner);

    /**
     * 查询期权到期日列表
     *
     * @param security 底层资产
     * @return 期权到期日列表
     */
    List<OptionsExpDate> queryOptionsExpDate(Security security);


    /**
     * 查询期权链
     *
     * @param security       底层资产
     * @param optionsExpDate 期权到期日
     * @return 期权链
     */
    OptionsChain queryOptionsChain(Security security, OptionsExpDate optionsExpDate);
}
