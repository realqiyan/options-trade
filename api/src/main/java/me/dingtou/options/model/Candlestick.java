package me.dingtou.options.model;

import lombok.Data;

import java.math.BigDecimal;

/**
 * K线
 * <p>
 * {
 * "close": "362.000",
 * "open": "364.600",
 * "low": "361.600",
 * "high": "368.800",
 * "volume": 10853604,
 * "turnover": "3954556819.000",
 * "timestamp": 1650384000
 * }
 *
 * @author yhb
 */
@Data
public class Candlestick {
    /**
     * 收盘价
     */
    private BigDecimal close;

    /**
     * 开盘价
     */
    private BigDecimal open;

    /**
     * 最低价
     */
    private BigDecimal low;

    /**
     * 最高价
     */
    private BigDecimal high;

    /**
     * 成交量
     */
    private Long volume;

    /**
     * 成交额
     */
    private BigDecimal turnover;

    /**
     * 时间戳
     */
    private Long timestamp;
}
