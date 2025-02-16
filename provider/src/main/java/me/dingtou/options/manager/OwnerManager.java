package me.dingtou.options.manager;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.constant.OrderExt;
import me.dingtou.options.constant.OrderStatus;
import me.dingtou.options.constant.StrategyStatus;
import me.dingtou.options.constant.TradeSide;
import me.dingtou.options.dao.OwnerAccountDAO;
import me.dingtou.options.dao.OwnerOrderDAO;
import me.dingtou.options.dao.OwnerSecurityDAO;
import me.dingtou.options.dao.OwnerStrategyDAO;
import me.dingtou.options.gateway.OptionsTradeGateway;
import me.dingtou.options.gateway.futu.executor.QueryExecutor;
import me.dingtou.options.gateway.futu.executor.func.query.FuncGetOptionsRealtimeData;
import me.dingtou.options.model.*;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Component
public class OwnerManager {

    @Autowired
    private OwnerAccountDAO ownerAccountDAO;

    @Autowired
    private OwnerSecurityDAO ownerSecurityDAO;

    @Autowired
    private OwnerStrategyDAO ownerStrategyDAO;

    @Autowired
    private OwnerOrderDAO ownerOrderDAO;

    @Autowired
    private OptionsTradeGateway optionsTradeGateway;


    public Owner queryOwner(String owner) {
        Owner ownerObj = new Owner();
        ownerObj.setOwner(owner);
        ownerObj.setAccount(queryOwnerAccount(owner));
        ownerObj.setSecurityList(queryOwnerSecurity(owner));
        ownerObj.setStrategyList(queryOwnerStrategy(owner));
        return ownerObj;
    }

    public OwnerAccount queryOwnerAccount(String owner) {
        QueryWrapper<OwnerAccount> query = new QueryWrapper<>();
        query.eq("owner", owner)
                .eq("status", StrategyStatus.VALID.getCode());
        return ownerAccountDAO.selectOne(query);
    }

    public List<OwnerSecurity> queryOwnerSecurity(String owner) {
        QueryWrapper<OwnerSecurity> query = new QueryWrapper<>();
        query.eq("owner", owner)
                .eq("status", StrategyStatus.VALID.getCode());
        return ownerSecurityDAO.selectList(query);
    }

    public List<OwnerStrategy> queryOwnerStrategy(String owner) {
        QueryWrapper<OwnerStrategy> query = new QueryWrapper<>();
        query.eq("owner", owner)
                .eq("status", StrategyStatus.VALID.getCode());

        return ownerStrategyDAO.selectList(query);
    }


    public OwnerOrder queryOwnerOrder(String owner, String platformOrderId, String platformFillId) {
        QueryWrapper<OwnerOrder> query = new QueryWrapper<>();
        query.eq("owner", owner);
        query.eq("platform_order_id", platformOrderId);
        List<OwnerOrder> ownerOrderList = ownerOrderDAO.selectList(query);
        if (null == ownerOrderList || ownerOrderList.isEmpty()) {
            throw new RuntimeException("query owner order error");
        }
        if (ownerOrderList.size() == 1) {
            return ownerOrderList.get(0);
        } else {
            for (OwnerOrder ownerOrder : ownerOrderList) {
                if (ownerOrder.getPlatformFillId().equals(platformFillId)) {
                    return ownerOrder;
                }
            }
        }
        throw new RuntimeException("query owner order error");
    }

    public List<OwnerOrder> queryStrategyOrder(OwnerStrategy strategy) {
        QueryWrapper<OwnerOrder> query = new QueryWrapper<>();
        query.eq("owner", strategy.getOwner());
        query.eq("strategy_id", strategy.getStrategyId());
        List<OwnerOrder> ownerOrders = ownerOrderDAO.selectList(query);
        // 初始化订单扩展字段
        for (OwnerOrder ownerOrder : ownerOrders) {
            if (null == ownerOrder.getExt()) {
                ownerOrder.setExt(new HashMap<>());
            }
        }
        calculateBasic(strategy, ownerOrders);
        calculateProfit(ownerOrders);

        return ownerOrders;
    }

    private void calculateBasic(OwnerStrategy strategy, List<OwnerOrder> ownerOrders) {
        if (null == ownerOrders || ownerOrders.isEmpty()) {
            return;
        }
        Date now = new Date();
        // 计算提交的交易单是否已经平仓用
        Map<String, List<OwnerOrder>> codeOrdersMap = ownerOrders.stream().collect(Collectors.groupingBy(OwnerOrder::getCode));
        Map<String, Boolean> orderClose = new HashMap<>();
        for (Map.Entry<String, List<OwnerOrder>> codeOrders : codeOrdersMap.entrySet()) {
            String code = codeOrders.getKey();
            List<OwnerOrder> orders = codeOrders.getValue();
            // 已成交的订单买入卖出的数量是否为0
            List<OwnerOrder> successOrders = orders.stream()
                    .filter(order -> OrderStatus.of(order.getStatus()).isTraded())
                    .toList();
            if (successOrders.isEmpty()) {
                continue;
            }
            BigDecimal totalQuantity = successOrders.stream()
                    .map(order -> new BigDecimal(order.getQuantity()).multiply(new BigDecimal(TradeSide.of(order.getSide()).getSign())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (BigDecimal.ZERO.equals(totalQuantity)) {
                orderClose.put(code, true);
            }
        }


        for (OwnerOrder ownerOrder : ownerOrders) {
            if (!ownerOrder.getCode().equals(ownerOrder.getUnderlyingCode())) {
                BigDecimal totalIncome = ownerOrder.getPrice()
                        .multiply(new BigDecimal(ownerOrder.getQuantity()))
                        .multiply(new BigDecimal(strategy.getLotSize()))
                        .multiply(new BigDecimal(TradeSide.of(ownerOrder.getSide()).getSign()));
                // 订单收益
                ownerOrder.getExt().put(OrderExt.TOTAL_INCOME.getCode(), totalIncome.toString());
            }

            if (!OrderStatus.of(ownerOrder.getStatus()).isValid()) {
                // 无效订单直接标记关闭
                ownerOrder.getExt().put(OrderExt.IS_CLOSE.getCode(), Boolean.TRUE.toString());
            } else {
                // 订单是不是已经过了行权日
                boolean isTimeout = ownerOrder.getStrikeTime().before(now) && !DateUtils.isSameDay(ownerOrder.getStrikeTime(), now);
                // 订单是否已经平仓
                Boolean isClose = isTimeout || orderClose.getOrDefault(ownerOrder.getCode(), false);
                ownerOrder.getExt().put(OrderExt.IS_CLOSE.getCode(), String.valueOf(isClose));
            }

            //计算期权到期日ownerOrder.getStrikeTime()和now的间隔天数
            LocalDate strikeTime = ownerOrder.getStrikeTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate currentDate = now.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            long daysToExpiration = ChronoUnit.DAYS.between(currentDate, strikeTime);
            ownerOrder.getExt().put(OrderExt.CUR_DTE.getCode(), String.valueOf(daysToExpiration));

        }


    }

    private void calculateProfit(List<OwnerOrder> ownerOrders) {
        if (null == ownerOrders || ownerOrders.isEmpty()) {
            return;
        }
        Date now = new Date();
        List<Security> securityList = new ArrayList<>();
        for (OwnerOrder ownerOrder : ownerOrders) {
            if (ownerOrder.getStrikeTime().before(now)) {
                continue;
            }
            if (!ownerOrder.getCode().equals(ownerOrder.getUnderlyingCode())) {
                securityList.add(Security.of(ownerOrder.getCode(), ownerOrder.getMarket()));
            }
        }

        if (securityList.isEmpty()) {
            return;
        }
        List<OptionsRealtimeData> optionsBasicInfo = QueryExecutor.query(new FuncGetOptionsRealtimeData(securityList));
        if (optionsBasicInfo.isEmpty()) {
            log.warn("query options basic info failed, retry");
            optionsBasicInfo = QueryExecutor.query(new FuncGetOptionsRealtimeData(securityList));
        }
        if (optionsBasicInfo.isEmpty()) {
            return;
        }
        // 计算利润
        for (OwnerOrder ownerOrder : ownerOrders) {
            Security security = Security.of(ownerOrder.getCode(), ownerOrder.getMarket());
            Optional<OptionsRealtimeData> any = optionsBasicInfo.stream().filter(options -> options.getSecurity().equals(security)).findAny();
            if (any.isEmpty()) {
                continue;
            }
            BigDecimal curPrice = any.get().getCurPrice();
            ownerOrder.getExt().put(OrderExt.CUR_PRICE.getCode(), curPrice.toString());
            TradeSide tradeSide = TradeSide.of(ownerOrder.getSide());
            if (TradeSide.SELL.equals(tradeSide) || TradeSide.SELL_SHORT.equals(tradeSide)) {
                BigDecimal profitRatio = ownerOrder.getPrice().subtract(curPrice).divide(ownerOrder.getPrice(), 4, RoundingMode.HALF_UP).multiply(new BigDecimal(100)).setScale(2, RoundingMode.HALF_UP);
                ownerOrder.getExt().put(OrderExt.PROFIT_RATIO.getCode(), profitRatio.toString());
            }

        }

    }
}
