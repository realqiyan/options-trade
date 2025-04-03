package me.dingtou.options.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import me.dingtou.options.constant.Market;
import me.dingtou.options.constant.OrderExt;
import me.dingtou.options.constant.OrderStatus;
import me.dingtou.options.constant.TradeSide;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 订单
 *
 * @author qiyan
 */
@Data
public class OwnerOrder implements Cloneable {

    /**
     * 期权代码正则表达式 ^([A-Z0-9]*)([0-9]{6})([CP])([0-9]*)$
     * 
     * 示例：BABA241220C90000
     */
    private static final Pattern OPTIONS_CODE_REGEX = Pattern.compile("^([A-Z0-9]*)([0-9]{6})([CP])([0-9]*)$");

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

    /**
     * 获取扩展字段值
     *
     * @param extKey 扩展字段枚举
     * @return 扩展字段值
     */
    public String getExtValue(OrderExt extKey) {
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
    public void setExtValue(OrderExt extKey, Object value) {
        if (ext == null) {
            ext = new HashMap<>();
        }
        if (value instanceof String) {
            ext.put(extKey.getKey(), (String) value);
        } else {
            ext.put(extKey.getKey(), extKey.toString(value));
        }
    }

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

    /**
     * 订单是否为看跌期权
     * 
     * @param order 订单
     * @return 是否为看跌期权
     */
    public static boolean isPut(OwnerOrder order) {
        Matcher matcher = OPTIONS_CODE_REGEX.matcher(order.getCode());
        if (!matcher.find()) {
            return false;
        } else {
            return "P".equals(matcher.group(3));
        }
    }

    /**
     * 订单是否为看涨期权
     * 
     * @param order 订单
     * @return 是否为看涨期权
     */
    public static boolean isCall(OwnerOrder order) {
        Matcher matcher = OPTIONS_CODE_REGEX.matcher(order.getCode());
        if (!matcher.find()) {
            return false;
        } else {
            return "C".equals(matcher.group(3));
        }
    }

    /**
     * 订单是否未平仓
     * 
     * @param order 订单
     * @return 是否未平仓
     */
    public static boolean isOpen(OwnerOrder order) {
        return !isClose(order);
    }

    /**
     * 订单是否已成交
     * 
     * @param order 订单
     * @return 是否已成交
     */
    public static boolean isTraded(OwnerOrder order) {
        return OrderStatus.of(order.getStatus()).isTraded();
    }

    /**
     * 订单是否平仓（多订单之间的买卖抵消需要综合计算OrderExt.IS_CLOSE）
     * 
     * @param order 订单
     * @return 是否平仓
     */
    public static boolean isClose(OwnerOrder order) {
        ZoneId zoneId = Market.of(order.getMarket()).getZoneId();
        LocalDate nyLocalDate = new Date().toInstant().atZone(zoneId).toLocalDate();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        String strikeDateStr = dateFormat.format(order.getStrikeTime());
        LocalDate strikeDate = LocalDate.parse(strikeDateStr);
        if (!OrderStatus.of(order.getStatus()).isValid()) {
            // 无效订单直接标记关闭
            return true;
        } else {
            // 订单是不是已经过了行权日
            boolean isTimeout = strikeDate.isBefore(nyLocalDate) && !strikeDate.isEqual(nyLocalDate);
            // 订单是否已经平仓 (多订单之间买卖的无法计算平仓)
            if (isTimeout) {
                return true;
            }
            // 综合其他订单确认是否已经平仓
            if (order.getExt() != null
                    && order.getExt().containsKey(OrderExt.IS_CLOSE.getKey())
                    && Boolean.valueOf(order.getExt().get(OrderExt.IS_CLOSE.getKey()))) {
                return true;
            }
            return false;
        }
    }

    /**
     * 订单是否为股票订单
     * 
     * @param order 订单
     * @return 是否为股票订单
     */
    public static boolean isStockOrder(OwnerOrder order) {
        return order.getUnderlyingCode().equals(order.getCode());
    }

    /**
     * 订单是否为期权订单
     * 
     * @param order 订单
     * @return 是否为期权订单
     */
    public static boolean isOptionsOrder(OwnerOrder order) {
        return !isStockOrder(order);
    }

    /**
     * 订单是否为卖出订单
     * 
     * @param order 订单
     * @return 是否为卖出订单
     */
    public static boolean isSell(OwnerOrder order) {
        return order.getSide().equals(TradeSide.SELL.getCode())
                || order.getSide().equals(TradeSide.SELL_SHORT.getCode());
    }

    /**
     * 订单是否为买入订单
     * 
     * @param order 订单
     * @return 是否为买入订单
     */
    public static boolean isBuy(OwnerOrder order) {
        return order.getSide().equals(TradeSide.BUY.getCode())
                || order.getSide().equals(TradeSide.BUY_BACK.getCode());
    }

    /**
     * 订单的行权价格
     * 
     * @param order 订单
     * @return 行权价格
     */
    public static BigDecimal strikePrice(OwnerOrder order) {
        Matcher matcher = OPTIONS_CODE_REGEX.matcher(order.getCode());
        if (!matcher.find()) {
            throw new IllegalArgumentException("invalid options code: " + order.getCode());
        } else {
            return new BigDecimal(matcher.group(4)).divide(BigDecimal.valueOf(1000));
        }
    }

    /**
     * 订单的到期日daysToExpiration
     * 
     * @param order 订单
     * @return 到期日
     */
    public static long dte(OwnerOrder order) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        ZoneId zoneId = Market.of(order.getMarket()).getZoneId();
        LocalDate localDate = new Date().toInstant().atZone(zoneId).toLocalDate();
        String strikeDateStr = simpleDateFormat.format(order.getStrikeTime());
        LocalDate strikeDate = LocalDate.parse(strikeDateStr);
        return ChronoUnit.DAYS.between(localDate, strikeDate);
    }

    /**
     * 订单的合约数量
     * 
     * @param order 订单
     * @return 合约数量
     */
    public static int lotSize(OwnerOrder order) {
        String lotSize = order.getExtValue(OrderExt.LOT_SIZE);
        if (null == lotSize) {
            return 100;
        }
        return (int) OrderExt.LOT_SIZE.fromString(lotSize);
    }

    /**
     * 订单的收益
     * 
     * @param order 订单
     * @return 收益
     */
    public static BigDecimal income(OwnerOrder order) {
        return order.getPrice()
                .multiply(new BigDecimal(order.getQuantity()))
                .multiply(new BigDecimal(lotSize(order)))
                .multiply(new BigDecimal(TradeSide.of(order.getSide()).getSign()));
    }

}
