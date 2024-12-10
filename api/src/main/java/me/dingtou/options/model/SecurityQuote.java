package me.dingtou.options.model;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 股票行情
 *
 * @author qiyan
 */
@Data
public class SecurityQuote {

    /**
     * 股票信息
     */
    private Security security;
    /**
     * 最新价
     */
    private BigDecimal lastDone;

}
