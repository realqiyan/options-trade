package me.dingtou.options.gateway;

import me.dingtou.options.model.Security;
import me.dingtou.options.model.SecurityOrderBook;

/**
 * 证券接口
 *
 * @author qiyan
 */
public interface SecurityOrderBookGateway {

    /**
     * 查询摆盘
     *
     * @param security 证券
     * @return 摆盘
     */
    SecurityOrderBook getOrderBook(Security security);

}
