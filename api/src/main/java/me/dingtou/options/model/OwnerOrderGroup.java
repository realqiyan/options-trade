package me.dingtou.options.model;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单组，按平台订单号聚合
 */
@Data
public class OwnerOrderGroup implements Serializable {
    /** 
     * 平台订单号，作为订单组主键 
     */
    private String platformOrderId;
    /** 
     * 累计收益 
     */
    private BigDecimal totalIncome;
    /** 
     * 累计手续费 
     */
    private BigDecimal totalOrderFee;
}