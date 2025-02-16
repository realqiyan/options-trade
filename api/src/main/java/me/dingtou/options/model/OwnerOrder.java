package me.dingtou.options.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

/**
 * 订单
 *
 * @author qiyan
 */
@Data
public class OwnerOrder implements Cloneable {
    /**
     * ID
     */
    private Long id;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 修改时间
     */
    private Date updateTime;

    /**
     * 策略的唯一标识符，表示该订单所属的策略
     */
    private String strategyId;

    /**
     * 标的基础代码，表示该订单交易的基础资产
     */
    private String underlyingCode;

    /**
     * 订单的代码标识符，可能用于表示订单的版本或特定代码
     */
    private String code;

    /**
     * 市场的标识符，表示订单适用的市场或地区
     */
    private Integer market;

    /**
     * 交易时间，表示订单的交易日期
     */
    private Date tradeTime;

    /**
     * 行权时间，表示订单的行权日期，默认为NULL
     */
    private Date strikeTime;

    /**
     * 订单方向，表示买入或卖出
     */
    private Integer side;

    /**
     * 价格，表示订单的交易价格
     */
    private BigDecimal price;

    /**
     * 订单交易费用，订单号维度的交易费用，默认为NULL
     */
    private BigDecimal orderFee;

    /**
     * 数量，表示订单的交易数量
     */
    private Integer quantity;

    /**
     * 订单来源
     */
    private String tradeFrom;

    /**
     * 是否为子订单
     */
    private Boolean subOrder;


    /**
     * 状态，表示订单的状态
     */
    private Integer status;

    /**
     * 所有者的标识符，表示订单属于哪个用户或实体
     */
    private String owner;

    /**
     * 平台订单ID，表示订单在平台上的唯一标识符，默认为NULL
     */
    private String platformOrderId;

    /**
     * 平台的扩展订单ID，表示订单在平台上的唯一标识符，默认为NULL
     */
    private String platformOrderIdEx;

    /**
     * 平台的成交ID，表示订单在平台上成交的唯一标识符，默认为NULL
     */
    private String platformFillId;


    /**
     * 扩展信息的集合，允许在订单中存储额外的自定义键值对，默认为NULL
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, String> ext;

    @Override
    public OwnerOrder clone() {
        try {
            OwnerOrder cloneOrder = (OwnerOrder) super.clone();
            cloneOrder.setId(null);
            cloneOrder.setPlatformOrderId(null);
            cloneOrder.setPlatformOrderIdEx(null);
            cloneOrder.setPlatformFillId(null);
            cloneOrder.setOrderFee(null);
            return cloneOrder;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
