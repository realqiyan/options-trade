package me.dingtou.options.gateway;

import me.dingtou.options.model.Security;
import me.dingtou.options.model.SecurityOrderBook;
import me.dingtou.options.model.SecurityQuote;

import java.util.List;

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
