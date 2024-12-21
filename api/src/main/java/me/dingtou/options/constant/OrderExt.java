package me.dingtou.options.constant;

import com.alibaba.fastjson.JSON;
import lombok.Getter;
import me.dingtou.options.model.Options;
import me.dingtou.options.model.OwnerOrder;

/**
 * 订单扩展信息
 *
 * @author qiyan
 */
@Getter
public enum OrderExt {

    TOTAL_INCOME("totalIncome", String.class),

    CUR_STATUS("curStatus", String.class),

    CUR_PRICE("curPrice", String.class),

    PROFIT_RATIO("profitRatio", String.class),

    SOURCE_ORDER("sourceOrder", OwnerOrder.class),


    SOURCE_OPTIONS("sourceOptions", Options.class);

    private final String code;
    private final Class<?> classType;

    OrderExt(String code, Class<?> classType) {
        this.code = code;
        this.classType = classType;
    }

    public static OrderExt of(String code) {
        OrderExt[] values = OrderExt.values();
        for (OrderExt val : values) {
            if (val.getCode().equals(code)) {
                return val;
            }
        }
        throw new IllegalArgumentException(code + " not found.");
    }

    public String toString(Object obj) {
        if (obj.getClass().isAssignableFrom(this.getClassType())) {
            return JSON.toJSONString(obj);
        }
        throw new IllegalArgumentException("type not match");
    }

    public Object fromString(String str) {
        return JSON.parseObject(str, this.getClassType());
    }
}
