package me.dingtou.options.model;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 指标项
 *
 * @author qiyan
 */
@Data
public class StockIndicatorItem {
    /**
     * 指标日期
     */
    private String date;
    /**
     * 指标值
     */
    private BigDecimal value;

    public StockIndicatorItem() {
    }

    public StockIndicatorItem(String date, BigDecimal value) {
        this.date = date;
        this.value = value;
    }
}
