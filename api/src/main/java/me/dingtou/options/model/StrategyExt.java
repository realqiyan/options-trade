package me.dingtou.options.model;

/**
 * 策略扩展字段枚举
 * 用于管理OwnerStrategy的ext字段中的键名
 *
 * @author qiyan
 */
public enum StrategyExt {
    // 通用配置
    INITIAL_STOCK_NUM("initial_stock_num", "策略初始股票数"),
    INITIAL_STOCK_COST("initial_stock_cost", "策略初始股票成本价"),
    
    // 车轮策略配置
    WHEEL_SELLPUT_STRIKE_PRICE("wheel_sellput_strike_price", "车轮策略Sell Put可接受的行权价");
    
    private final String key;
    private final String desc;
    
    StrategyExt(String key, String desc) {
        this.key = key;
        this.desc = desc;
    }
    
    public String getKey() {
        return key;
    }
    
    public String getDesc() {
        return desc;
    }
} 