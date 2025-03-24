package me.dingtou.options.web.model;

import java.math.BigDecimal;
import java.util.Date;

import lombok.Data;

/**
 * 平仓单任务请求
 */
@Data
public class CloseOrderJobReq {

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 平仓金额（优先级高于profitRatio）
     */
    private BigDecimal price;

    /**
     * 平仓单取消时间
     */
    private Date cannelTime;

    @Override
    public String toString() {
        return "CloseOrderJobReq [enabled=" + enabled +
                ", orderId=" + orderId +
                ", price=" + price +
                ", cannelTime=" + cannelTime +
                "]";
    }

}
