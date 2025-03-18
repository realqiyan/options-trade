package me.dingtou.options.constant;

import lombok.Getter;
import me.dingtou.options.model.OwnerOrder;
import me.dingtou.options.model.SecurityQuote;

/**
 * 数据推送类型
 */
@Getter
public enum PushDataType {

    /**
     * 纽约时间
     */
    NYC_TIME("nyc_time", String.class, false),
    /**
     * 股票价格
     */
    STOCK_PRICE("stock_price", SecurityQuote.class, false),
    /**
     * 订单推送
     */
    ORDER_PUSH("order_push", OwnerOrder.class, true),
    ;

    /**
     * 是否是内部数据
     */
    private final boolean innerData;
    /**
     * 数据编码
     */
    private final String code;
    /**
     * 数据类型
     */
    private final Class<?> dataClass;

    PushDataType(String code, Class<?> dataClass, boolean innerData) {
        this.code = code;
        this.dataClass = dataClass;
        this.innerData = innerData;
    }

    public static PushDataType of(String code) {
        PushDataType[] values = PushDataType.values();
        for (PushDataType val : values) {
            if (val.getCode().equals(code)) {
                return val;
            }
        }
        throw new IllegalArgumentException(code + " not found.");
    }
}
