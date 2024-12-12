package me.dingtou.options.model;

import lombok.Data;

import java.util.List;

/**
 * Owner
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
     * underlying asset
     */
    List<Security> securityList;

    /**
     * 账户列表
     */
    private List<Account> accountList;

    /**
     * 订单
     */
    List<Order> orderList;
}
