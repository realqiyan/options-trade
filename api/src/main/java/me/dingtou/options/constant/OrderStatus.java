package me.dingtou.options.constant;

import lombok.Getter;

/**
 * 状态
 *
 * @author qiyan
 */
@Getter
public enum OrderStatus {


    UNKNOWN(-1), //未知状态
    WAITING_SUBMIT(1), //等待提交
    SUBMITTING(2), //提交中
    SUBMITTED(5), //已提交，等待成交
    FILLED_PART(10), //部分成交
    FILLED_ALL(11), //全部已成
    CANCELLED_PART(14), //部分成交，剩余部分已撤单
    CANCELLED_ALL(15), //全部已撤单，无成交
    FAILED(21), //下单失败，服务拒绝
    DISABLED(22), //已失效
    DELETED(23), //已删除，无成交的订单才能删除
    FILL_CANCELLED(24), //成交被撤销
    ;

    private final Integer code;

    OrderStatus(int code) {
        this.code = code;
    }

    public static OrderStatus of(Integer code) {
        OrderStatus[] values = OrderStatus.values();
        for (OrderStatus val : values) {
            if (val.getCode().equals(code)) {
                return val;
            }
        }
        throw new IllegalArgumentException(code + " not found.");
    }

}
