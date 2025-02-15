package me.dingtou.options.gateway;

import me.dingtou.options.constant.CandlestickAdjustType;
import me.dingtou.options.constant.CandlestickPeriod;
import me.dingtou.options.model.Security;
import me.dingtou.options.model.SecurityCandlestick;

/**
 * 获取K线数据
 *
 * @author yhb
 */
public interface CandlestickGateway {
    /**
     * 获取K线数据
     *
     * @param security   股票
     * @param period     周期
     * @param count      数量
     * @param adjustType 调整类型
     * @return K线数据
     */
    SecurityCandlestick getCandlesticks(Security security, CandlestickPeriod period, Integer count, CandlestickAdjustType adjustType);
}
