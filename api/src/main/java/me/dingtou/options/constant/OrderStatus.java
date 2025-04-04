package me.dingtou.options.constant;

import lombok.Getter;

/**
 * 状态
 *
 * @author qiyan
 */
@Getter
public enum OrderStatus {


    UNKNOWN(-1, false, false), //未知状态
    WAITING_SUBMIT(1, true, false), //等待提交
    SUBMITTING(2, true, false), //提交中
    SUBMITTED(5, true, false), //已提交，等待成交
    FILLED_PART(10, true, true), //部分成交
    FILLED_ALL(11, true, true), //全部已成
    CANCELLED_PART(14, true, true), //部分成交，剩余部分已撤单
    CANCELLED_ALL(15, false, false), //全部已撤单，无成交
    FAILED(21, false, false), //下单失败，服务拒绝
    DISABLED(22, false, false), //已失效
    DELETED(23, false, false), //已删除，无成交的订单才能删除
    FILL_CANCELLED(24, false, false), //成交被撤销
    EARLY_ASSIGNED(25, false, true), //提前指派
    ;

    /**
     * 状态码
     */
    private final Integer code;
    /**
     * 是否有效
     */
    private final boolean valid;
    /**
     * 是否已成交
     */
    private final boolean traded;

    OrderStatus(int code, boolean valid, boolean traded) {
        this.code = code;
        this.valid = valid;
        this.traded = traded;
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
