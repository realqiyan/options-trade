package me.dingtou.options.model;

import lombok.Data;

import java.util.Map;

/**
 * 用户期权策略
 *
 * @author qiyan
 */
@Data
public class OwnerStrategy {
    /**
     * ID
     */
    private Integer id;
    /**
     * 所有者的标识符，表示策略属于哪个用户或实体
     */
    private String owner;

    /**
     * 平台的标识符，表示策略所属的平台或系统
     */
    private String platform;

    /**
     * 策略的唯一标识符，用于区分不同的策略
     */
    private String strategyId;
    /**
     * 策略的名称，用于在用户界面中显示
     */
    private String strategyName;

    /**
     * 策略的类型，描述策略的种类或分类
     */
    private String strategyType;
    
    /**
     * 当前阶段
     */
    private String currentStage;

    /**
     * 账户的标识符，表示策略关联的账户或用户
     */
    private String accountId;

    /**
     * 策略的代码标识符，可能用于表示策略的版本或特定代码
     */
    private String code;

    /**
     * 市场的标识符，表示策略适用的市场或地区
     */
    private Integer market;

    /**
     * 扩展信息的集合，允许在策略中存储额外的自定义键值对
     */
    private Map<String, String> ext;
}
