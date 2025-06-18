package me.dingtou.options.model;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class OwnerPosition {
    private String owner;
    private String securityCode;
    private String securityName;
    private BigDecimal quantity;
    private BigDecimal canSellQty;
    private BigDecimal costPrice;
    private BigDecimal currentPrice;
    private BigDecimal profit;
    private BigDecimal profitRate;
}
