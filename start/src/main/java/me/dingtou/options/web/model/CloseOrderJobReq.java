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
     * 订单ID
     */
    private Long orderId;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 平仓单取消时间
     */
    private Date cannelTime;

    /**
     * 平仓盈利比例
     */
    private BigDecimal profitRatio;

    @Override
    public String toString() {
        return "CloseOrderJobReq [orderId=" + orderId +
                ", enabled=" + enabled +
                ", cannelTime=" + cannelTime +
                ", profitRatio=" + profitRatio + "]";
    }

}
