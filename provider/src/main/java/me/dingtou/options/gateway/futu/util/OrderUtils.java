package me.dingtou.options.gateway.futu.util;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.futu.openapi.pb.TrdCommon;

import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.constant.OrderStatus;
import me.dingtou.options.constant.TradeFrom;
import me.dingtou.options.constant.TradeSide;
import me.dingtou.options.model.Owner;
import me.dingtou.options.model.OwnerOrder;
import me.dingtou.options.model.OwnerSecurity;

@Slf4j
public class OrderUtils {
    // BABA241220P830000 BABA1241220P830000
    private static Pattern CODE_PATTERN = Pattern.compile("^([A-Z0-9]*)([0-9]{6})([CP])([0-9]*)$");

    /**
     * 转换富途订单为OwnerOrder
     * 
     * @param order 富途订单
     * @param owner 用户
     * @return OwnerOrder
     */
    public static OwnerOrder convertOwnerOrder(TrdCommon.Order order, Owner owner) {
        try {
            // name:BABA 241220 83.00P
            // code:BABA241220P830000

            String code = order.getCode();
            Date strikeTime = null;

            Matcher matcher = CODE_PATTERN.matcher(code);
            final String underlyingCode;
            if (!matcher.find()) {
                underlyingCode = code;
                strikeTime = parseDate(order.getCreateTime());
            } else {
                underlyingCode = matcher.group(1);
                SimpleDateFormat strikeTimeFormat = new SimpleDateFormat("yyMMdd");
                strikeTime = strikeTimeFormat.parse(matcher.group(2));
            }

            // 检查是否属于目标股票 股票分红后历史期权代码会加编号 例如：BABA1 BABA2
            Optional<OwnerSecurity> securityOptional = owner.getSecurityList().stream()
                    .filter(security -> security.getCode().equals(underlyingCode)
                            || Pattern.compile(security.getCode() + "[0-9]").matcher(underlyingCode).matches())
                    .findAny();
            if (securityOptional.isEmpty()) {
                return null;
            }

            OwnerOrder ownerOrder = new OwnerOrder();
            ownerOrder.setOwner(owner.getOwner());
            ownerOrder.setUnderlyingCode(securityOptional.get().getCode());
            ownerOrder.setMarket(owner.getAccount().getMarket());
            ownerOrder.setTradeFrom(TradeFrom.PULL_ORDER.getCode());

            ownerOrder.setSide(TradeSide.of(order.getTrdSide()).getCode());
            ownerOrder.setPlatformOrderId(String.valueOf(order.getOrderID()));
            ownerOrder.setPlatformOrderIdEx(order.getOrderIDEx());
            ownerOrder.setStrikeTime(strikeTime);
            ownerOrder.setTradeTime(parseDate(order.getCreateTime()));
            ownerOrder.setCreateTime(parseDate(order.getCreateTime()));
            ownerOrder.setUpdateTime(parseDate(order.getUpdateTime()));
            ownerOrder.setCode(order.getCode());
            ownerOrder.setPrice(BigDecimal.valueOf(order.getPrice()));
            ownerOrder.setQuantity((int) order.getQty());
            ownerOrder.setStatus(OrderStatus.of(order.getOrderStatus()).getCode());

            return ownerOrder;
        } catch (Exception e) {
            log.error("convertOwnerOrder error. order: {}", order, e);
            return null;
        }
    }

    /**
     * 转换富途订单为OwnerOrder
     * 
     * @param order 富途订单
     * @param owner 用户
     * @return OwnerOrder
     */
    public static OwnerOrder convertOwnerOrder(TrdCommon.OrderFill order, Owner owner) {
        try {
            // BABA 241220 83.00P

            String code = order.getCode();
            Date strikeTime = null;

            String regexStr = "^([A-Z0-9]*)([0-9]{6})([CP])([0-9]*)$";
            Pattern codePattern = Pattern.compile(regexStr);
            Matcher matcher = codePattern.matcher(code);
            final String underlyingCode;
            if (!matcher.find()) {
                underlyingCode = code;
                strikeTime = parseDate(order.getCreateTime());
            } else {
                underlyingCode = matcher.group(1);
                SimpleDateFormat strikeTimeFormat = new SimpleDateFormat("yyMMdd");
                strikeTime = strikeTimeFormat.parse(matcher.group(2));
            }

            // 检查是否属于目标股票
            Optional<OwnerSecurity> securityOptional = owner.getSecurityList().stream()
                    .filter(security -> security.getCode().equals(underlyingCode))
                    .findAny();
            if (securityOptional.isEmpty()) {
                return null;
            }

            OwnerOrder ownerOrder = new OwnerOrder();
            ownerOrder.setOwner(owner.getOwner());
            ownerOrder.setUnderlyingCode(underlyingCode);
            ownerOrder.setMarket(owner.getAccount().getMarket());

            ownerOrder.setSide(TradeSide.of(order.getTrdSide()).getCode());
            ownerOrder.setPlatformOrderId(String.valueOf(order.getOrderID()));
            ownerOrder.setPlatformOrderIdEx(order.getOrderIDEx());
            ownerOrder.setPlatformFillId(String.valueOf(order.getFillID()));
            ownerOrder.setStrikeTime(strikeTime);
            ownerOrder.setTradeTime(parseDate(order.getCreateTime()));
            ownerOrder.setCreateTime(parseDate(order.getCreateTime()));
            ownerOrder.setUpdateTime(new Date((long) (order.getUpdateTimestamp() * 1000)));
            ownerOrder.setCode(order.getCode());
            ownerOrder.setPrice(BigDecimal.valueOf(order.getPrice()));
            ownerOrder.setQuantity((int) order.getQty());

            ownerOrder.setStatus(OrderStatus.FILLED_ALL.getCode());
            ownerOrder.setTradeFrom(TradeFrom.PULL_FILL.getCode());

            return ownerOrder;
        } catch (Exception e) {
            log.error("convertOwnerOrder error. order: {}", order, e);
            return null;
        }
    }

    /**
     * 解析日期
     * 
     * @param dateStr 日期字符串
     * @return 日期
     * @throws ParseException
     */
    private static Date parseDate(String dateStr) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        try {
            return dateFormat.parse(dateStr);
        } catch (java.text.ParseException e) {
            dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return dateFormat.parse(dateStr);
        }
    }
}
