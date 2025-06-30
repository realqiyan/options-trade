package me.dingtou.options.constant;

/**
 * 账户扩展字段枚举
 * 用于管理OwnerAccount的ext字段中的键名
 *
 * @author qiyan
 */
public enum AccountExt {
    // 长桥平台配置
    LONGPORT_APP_KEY("longport_app_key", "长桥平台App Key"),
    LONGPORT_APP_SECRET("longport_app_secret", "长桥平台App Secret"),
    LONGPORT_ACCESS_TOKEN("longport_access_token", "长桥平台Access Token"),
    
    // AI配置
    AI_BASE_URL("ai_base_url", "AI接口Base URL"),
    AI_API_MODEL("ai_api_model", "AI模型名称"),
    AI_API_KEY("ai_api_key", "AI接口Key"),
    AI_API_TEMPERATURE("ai_api_temperature", "AI温度参数"),
    AI_MCP_SETTINGS("ai_mcp_settings", "MCP服务器配置"),
    
    // 分析配置
    KLINE_PERIOD("kline_period", "K线周期，可选值：DAY(日K线)、WEEK(周K线)，默认为WEEK"),
    
    // 账户资金配置
    ACCOUNT_SIZE("account_size", "账户资金规模"),
    MARGIN_RATIO("margin_ratio", "保证金比例"),
    POSITION_RATIO("position_ratio", "头寸比例");
    
    private final String key;
    private final String desc;
    
    AccountExt(String key, String desc) {
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