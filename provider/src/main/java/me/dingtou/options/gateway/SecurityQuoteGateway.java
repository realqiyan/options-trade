package me.dingtou.options.gateway;

import me.dingtou.options.model.SecurityQuote;
import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.model.Security;

import java.util.List;
import java.util.function.Function;

/**
 * 证券接口
 *
 * @author qiyan
 */
public interface SecurityQuoteGateway {

    /**
     * 查询行情
     *
     * @param ownerAccount 账户
     * @param security     证券
     * @return 行情
     */
    SecurityQuote quote(OwnerAccount ownerAccount, Security security);

    /**
     * 批量查询行情
     *
     * @param ownerAccount 账户
     * @param securityList 证券列表
     * @return 行情列表
     */
    List<SecurityQuote> quote(OwnerAccount ownerAccount, List<Security> securityList);

    /**
     * 订阅行情
     *
     * @param ownerAccount 账户
     * @param securities   证券列表
     * @param callback     回调
     */
    void subscribeQuote(OwnerAccount ownerAccount, List<Security> securities, Function<SecurityQuote, Void> callback);
}
