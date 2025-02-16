package me.dingtou.options.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.util.Date;
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
     * 策略的唯一标识符，用于区分不同的策略
     */
    private String strategyId;
    /**
     * 策略的名称，用于在用户界面中显示
     */
    private String strategyName;
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
}
