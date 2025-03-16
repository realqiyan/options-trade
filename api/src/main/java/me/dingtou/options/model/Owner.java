package me.dingtou.options.model;

import lombok.Data;

import java.util.List;

/**
 * 用户数据
 *
 * 1.一个账号对应一个平台的一个账号
 * 2.一个账号关注多支证券
 * 3.一个账号下挂多个策略
 * 4.一个账号关联多个订单
 *
 * @author qiyan
 */
@Data
public class Owner {

    /**
     * owner
     */
    private String owner;

    /**
     * 账户信息
     */
    private OwnerAccount account;

    /**
     * 证券列表
     */
    private List<OwnerSecurity> securityList;

    /**
     * 策略列表
     */
    private List<OwnerStrategy> strategyList;

}
