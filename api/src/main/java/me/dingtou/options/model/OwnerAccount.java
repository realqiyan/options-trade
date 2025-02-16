package me.dingtou.options.model;

import lombok.Data;

import java.util.Date;

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
    private Integer id;
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
}
