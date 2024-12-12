package me.dingtou.options.model;

import lombok.Data;

/**
 * 账户
 */
@Data
public class Account {

    /**
     * 账户拥有者
     */
    private String owner;

    /**
     * 账户ID
     */
    private String accountId;

    /**
     * 平台
     */
    private String platform;

    /**
     * 扩展信息
     */
    private String ext;
}