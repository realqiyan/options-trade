package me.dingtou.options.dataobject;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OwnerOrder {

    /**
     * ID
     */
    private Long id;
    /**
     * 所有者
     */
    private String owner;
    /**
     * 代码
     */
    private String code;
    /**
     * 市场
     */
    private Integer market;

    /**
     * 买卖方向 1:买  2:卖
     */
    private Integer side;
    /**
     * 订单价格
     */
    private BigDecimal price;

    /**
     * 订单数量
     */
    private Long quantity;

    /**
     * 订单状态
     */
    private Integer status;

    /**
     * 扩展信息
     */
    private String ext;
}
