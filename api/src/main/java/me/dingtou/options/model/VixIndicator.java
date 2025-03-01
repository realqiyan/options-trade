package me.dingtou.options.model;


import lombok.Data;

/**
 * @author qiyan
 */
@Data
public class VixIndicator {
    /**
     * 恐慌指数
     */
    private StockIndicatorItem currentVix;

    /**
     * 标普500指数
     */
    private StockIndicatorItem currentSp500;
}
