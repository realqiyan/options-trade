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
     * 订单组主键
     */
    private String groupId;
    /**
     * 累计收益
     */
    private BigDecimal totalIncome;
    /**
     * 累计手续费
     */
    private BigDecimal totalOrderFee;
    /**
     * 分组订单数
     */
    private Integer orderCount;

    public OwnerOrderGroup() {
    }

    public OwnerOrderGroup(String groupId) {
        this.groupId = groupId;
    }
}