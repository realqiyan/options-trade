package me.dingtou.options.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.util.Date;
import java.util.HashMap;
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
    private Long id;
    /**
     * 所有者的标识符，表示策略属于哪个用户或实体
     */
    private String owner;
    /**
     * 策略的唯一标识符，用于区分不同的策略
     */
    private String strategyId;
    /**
     * 策略的名称，用于在用户界面中显示
     */
    private String strategyName;
    /**
     * 策略的策略编码：如wheel_strategy
     */
    private String strategyCode;
    /**
     * 策略的当前阶段，用于表示策略的当前状态或阶段
     */
    private String stage;
    /**
     * 策略的开始时间，用于记录策略的创建或启用时间
     */
    private Date startTime;
    /**
     * 策略的代码标识符，可能用于表示策略的版本或特定代码
     */
    private String code;
    /**
     * 每手数量
     */
    private Integer lotSize;
    /**
     * 策略的状态，用于表示策略的当前运行状态或状态
     */
    private Integer status;

    /**
     * 扩展信息的集合，允许在策略中存储额外的自定义键值对
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, String> ext;

    /**
     * 获取扩展字段值
     *
     * @param extKey       扩展字段枚举
     * @param defaultValue 默认值
     * @return 扩展字段值
     */
    public String getExtValue(StrategyExt extKey, String defaultValue) {
        if (ext == null) {
            return defaultValue;
        }
        return ext.getOrDefault(extKey.getKey(), defaultValue);
    }

    /**
     * 设置扩展字段值
     *
     * @param extKey 扩展字段枚举
     * @param value  扩展字段值
     */
    public void setExtValue(StrategyExt extKey, String value) {
        if (ext == null) {
            ext = new HashMap<>();
        }
        ext.put(extKey.getKey(), value);
    }
}
