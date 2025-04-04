package me.dingtou.options.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import me.dingtou.options.constant.AccountExt;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 账户信息
 *
 * @author qiyan
 */
@Data
public class OwnerAccount {
    /**
     * ID
     */
    private Long id;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 所有者的标识符，表示策略属于哪个用户或实体
     */
    private String owner;

    /**
     * 平台的标识符，表示策略所属的平台或系统
     */
    private String platform;
    /**
     * 市场的标识符，表示策略适用的市场或地区
     */
    private Integer market;
    /**
     * 账户的标识符，表示策略关联的账户或用户
     */
    private String accountId;
    /**
     * otp认证信息
     */
    private String otpAuth;

    /**
     * 扩展配置，存储额外的配置信息
     * 只支持一层配置，且只支持字符串类型
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
    public String getExtValue(AccountExt extKey, String defaultValue) {
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
    public void setExtValue(AccountExt extKey, String value) {
        if (ext == null) {
            ext = new HashMap<>();
        }
        ext.put(extKey.getKey(), value);
    }

}
