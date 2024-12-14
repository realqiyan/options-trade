package me.dingtou.options.model;

import lombok.Data;

import java.util.List;

/**
 * 用户数据
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
     * 策略列表
     */
    List<? extends OwnerStrategy> strategyList;


    /**
     * 订单列表
     */
    List<? extends OwnerOrder> orderList;
}
