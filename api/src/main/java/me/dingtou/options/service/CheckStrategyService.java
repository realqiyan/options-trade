package me.dingtou.options.service;

/**
 * 检查持仓策略服务
 */
public interface CheckStrategyService {

    /**
     * 检查持仓策略
     */
    void checkALlOwnerStrategy();

    /**
     * 检查用户持仓策略
     * 
     * @param owner 用户ID
     */
    void checkOwnerStrategy(String owner);
}
