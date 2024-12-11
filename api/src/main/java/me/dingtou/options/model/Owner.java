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
     * 订单
     */
    List<Order> orderList;
}
