package me.dingtou.options.gateway;

import me.dingtou.options.model.SecurityQuote;
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
     * @param security 证券
     * @return 行情
     */
    SecurityQuote quote(Security security);

    /**
     * 批量查询行情
     *
     * @param securityList 证券列表
     * @return 行情列表
     */
    List<SecurityQuote> quote(List<Security> securityList);

    /**
     * 订阅行情
     *
     * @param securities 证券列表
     * @param callback   回调
     */
    void subscribeQuote(List<Security> securities, Function<SecurityQuote, Void> callback);
}
