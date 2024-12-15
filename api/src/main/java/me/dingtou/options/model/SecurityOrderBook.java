package me.dingtou.options.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.dingtou.options.constant.Market;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * 证券摆盘
 *
 * @author qiyan
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SecurityOrderBook extends Security {
    /**
     * 买盘列表
     */
    private List<BigDecimal> bidList;
    /**
     * 卖盘列表
     */
    private List<BigDecimal> askList;


}
