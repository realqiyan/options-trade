package me.dingtou.options.service;

import me.dingtou.options.model.OwnerSummary;
import me.dingtou.options.model.StrategySummary;

/**
 * 总结服务
 * 
 * @author qiyan
 */
public interface SummaryService {

    /**
     * 查询owner汇总
     * 
     * @param owner 所有者
     * @return 汇总信息
     */
    OwnerSummary queryOwnerSummary(String owner);

    /**
     * 查询策略汇总
     * 
     * @param owner      策略所有者
     * @param strategyId 策略ID
     * @return 订单列表
     */
    StrategySummary queryStrategySummary(String owner, String strategyId);
}
