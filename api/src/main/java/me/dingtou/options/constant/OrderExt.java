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

    /**
     * 当前价格
     */
    CUR_PRICE("curPrice", String.class),

    /**
     * 总收益
     */
    TOTAL_INCOME("totalIncome", String.class),

    /**
     * 是否平仓
     */
    IS_CLOSE("isClose", String.class),

    /**
     * 盈亏比例
     */
    PROFIT_RATIO("profitRatio", String.class),

    /**
     * 来源订单
     */
    SOURCE_ORDER("sourceOrder", OwnerOrder.class),

    /**
     * 来源期权
     */
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
