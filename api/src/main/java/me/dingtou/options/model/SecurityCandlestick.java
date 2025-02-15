package me.dingtou.options.model;

import lombok.Data;

import java.util.List;

/**
 * {
 * "symbol": "700.HK",
 * "candlesticks": [
 * {
 * "close": "362.000",
 * "open": "364.600",
 * "low": "361.600",
 * "high": "368.800",
 * "volume": 10853604,
 * "turnover": "3954556819.000",
 * "timestamp": 1650384000
 * },
 * {
 * "close": "348.000",
 * "open": "352.000",
 * "low": "343.000",
 * "high": "356.200",
 * "volume": 25738562,
 * "turnover": "8981529950.000",
 * "timestamp": 1650470400
 * }
 * ]
 * }
 */

@Data
public class SecurityCandlestick {
    /**
     * 股票
     */
    private Security security;
    /**
     * K线数据
     */
    private List<Candlestick> candlesticks;
}
