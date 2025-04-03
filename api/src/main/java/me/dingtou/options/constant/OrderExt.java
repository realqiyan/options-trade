package me.dingtou.options.constant;

import java.math.BigDecimal;
import java.util.List;

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

    ////////////////////////////////
    // 实时属性 //
    ////////////////////////////////
    /**
     * 当前价格
     */
    CUR_PRICE("curPrice", String.class),

    /**
     * 当前到期天数 Days to Expiration
     */
    CUR_DTE("curDTE", Long.class),

    /**
     * 盈亏比例
     */
    PROFIT_RATIO("profitRatio", String.class),

    /**
     * 总收益
     */
    TOTAL_INCOME("totalIncome", BigDecimal.class),

    /**
     * 是否平仓（会计算多订单之间的买卖抵消）
     */
    IS_CLOSE("isClose", Boolean.class),

    /**
     * AI分析提示词
     */
    PROMPT("prompt", String.class),

    /**
     * 可以Roll的期权实时数据
     */
    ROLL_OPTIONS("rollOptions", List.class),

    ////////////////////////////////
    // 静态属性 //
    ////////////////////////////////

    /**
     * 一笔订单的合约数量
     */
    LOT_SIZE("lotSize", Integer.class),

    /**
     * 当前行权价
     */
    STRIKE_PRICE("strikePrice", BigDecimal.class),

    /**
     * 是否是认沽订单
     */
    IS_PUT("isPut", Boolean.class),

    /**
     * 是否是认购订单
     */
    IS_CALL("isCall", Boolean.class),

    /**
     * 来源订单
     */
    SOURCE_ORDER("sourceOrder", OwnerOrder.class),

    /**
     * 来源期权
     */
    SOURCE_OPTIONS("sourceOptions", Options.class);

    private final String key;
    private final Class<?> classType;

    OrderExt(String key, Class<?> classType) {
        this.key = key;
        this.classType = classType;
    }

    public static OrderExt of(String key) {
        OrderExt[] values = OrderExt.values();
        for (OrderExt val : values) {
            if (val.getKey().equals(key)) {
                return val;
            }
        }
        throw new IllegalArgumentException(key + " not found.");
    }

    public String toString(Object obj) {
        if (obj == null) {
            return null;
        }
        if (this.getClassType().isAssignableFrom(obj.getClass())) {
            return JSON.toJSONString(obj);
        }
        throw new IllegalArgumentException("type not match: expected " + this.getClassType().getName() + ", but got " + obj.getClass().getName());
    }

    public Object fromString(String str) {
        return JSON.parseObject(str, this.getClassType());
    }
}
