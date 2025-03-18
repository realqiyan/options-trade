package me.dingtou.options.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import me.dingtou.options.constant.TradeTaskExt;
import me.dingtou.options.constant.TradeTaskStatus;
import me.dingtou.options.constant.TradeTaskType;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 交易任务
 *
 * @author qiyan
 */
@Data
public class OwnerTradeTask {
    /**
     * 任务ID
     */
    private Long id;

    /**
     * 所有者
     */
    private String owner;

    /**
     * 会话ID，关联AI助手会话
     */
    private String sessionId;

    /**
     * 任务类型
     * @see TradeTaskType
     */
    private Integer taskType;

    /**
     * 任务状态
     * @see TradeTaskStatus
     */
    private Integer status;

    /**
     * 任务标的市场
     */
    private Integer market;

    /**
     * 任务标的代码
     */
    private String code;

    /**
     * 策略ID
     */
    private String strategyId;

    /**
     * 任务开始时间
     */
    private Date startTime;

    /**
     * 任务结束时间
     */
    private Date endTime;

    /**
     * 任务创建时间
     */
    private Date createTime;

    /**
     * 任务更新时间
     */
    private Date updateTime;

    /**
     * 扩展信息的集合，允许在任务中存储额外的自定义键值对
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, String> ext;

    /**
     * 获取扩展字段值
     *
     * @param extKey 扩展字段枚举
     * @return 扩展字段值
     */
    public String getExtValue(TradeTaskExt extKey) {
        if (ext == null) {
            return null;
        }
        return ext.get(extKey.getKey());
    }

    /**
     * 设置扩展字段值
     *
     * @param extKey 扩展字段枚举
     * @param value  扩展字段值
     */
    public void setExtValue(TradeTaskExt extKey, Object value) {
        if (ext == null) {
            ext = new HashMap<>();
        }
        ext.put(extKey.getKey(), extKey.toString(value));
    }
} 