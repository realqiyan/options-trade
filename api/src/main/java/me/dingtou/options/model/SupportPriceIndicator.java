package me.dingtou.options.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SupportPriceIndicator {

    private BigDecimal lowestSupportPrice;

    private BigDecimal smaSupportPrice;

    private BigDecimal bollingerLowerSupportPrice;

}
