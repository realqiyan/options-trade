package me.dingtou.options.constant;

import com.alibaba.fastjson.JSON;
import lombok.Getter;

/**
 * 任务扩展字段
 *
 * @author qiyan
 */
@Getter
public enum TradeTaskExt {
    /**
     *  操作类型，可选值：buy_option, sell_option, close_option, roll_option
     */
    TYPE("type"),

    /**
     * 标的代码，格式为"市场.代码"
     */
    UNDERLYING_CODE("underlyingCode"),

    /**
     * 期权代码
     */
    CODE("code"),

    /**
     * 期权行权价
     */
    STRIKE_PRICE("strikePrice"),

    /**
     * 交易价格
     */
    PRICE("price"),

    /**
     * 交易数量
     */
    QUANTITY("quantity"),

    /**
     * 交易方向，可选值：buy, sell
     */
    SIDE("side"),

    /**
     * 策略ID
     */
    STRATEGY_ID("strategyId"),

    /** 
     * 建议执行时间
     */
    START_TIME("startTime"),

    /**
     * 建议结束时间
     */
    END_TIME("endTime"),

    /**
     * 操作描述
     */
    DESCRIPTION("description"),

    /**
     * 执行条件
     */
    CONDITION("condition"),

    /**
     * AI助手原始返回
     */
    AI_RESPONSE("aiResponse"),

    /**
     * 任务执行结果
     */
    EXECUTION_RESULT("executionResult");

    private final String key;

    TradeTaskExt(String key) {
        this.key = key;
    }

    /**
     * 将对象转换为字符串
     *
     * @param value 对象
     * @return 字符串
     */
    public String toString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return (String) value;
        }
        return JSON.toJSONString(value);
    }
} 