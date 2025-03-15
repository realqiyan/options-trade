package me.dingtou.options.manager;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.constant.OrderExt;
import me.dingtou.options.constant.Status;
import me.dingtou.options.constant.TradeSide;
import me.dingtou.options.dao.OwnerAccountDAO;
import me.dingtou.options.dao.OwnerOrderDAO;
import me.dingtou.options.dao.OwnerSecurityDAO;
import me.dingtou.options.dao.OwnerStrategyDAO;
import me.dingtou.options.gateway.futu.executor.QueryExecutor;
import me.dingtou.options.gateway.futu.executor.func.query.FuncGetOptionsRealtimeData;
import me.dingtou.options.model.*;
import me.dingtou.options.util.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    /**
     * 查询owner
     * 
     * @param owner 账号
     * @return owner
     */
    public Owner queryOwner(String owner) {
        Owner ownerObj = new Owner();
        ownerObj.setOwner(owner);
        ownerObj.setAccount(queryOwnerAccount(owner));
        ownerObj.setSecurityList(queryOwnerSecurity(owner));
        ownerObj.setStrategyList(queryOwnerStrategy(owner));
        return ownerObj;
    }

    /**
     * 查询owner账号
     * 
     * @param owner 账号
     * @return owner账号
     */
    public OwnerAccount queryOwnerAccount(String owner) {
        return ownerAccountDAO.queryOwner(owner);
    }

    /**
     * 查询所有owner账号
     * 
     * @return 所有owner账号
     */
    public List<OwnerAccount> queryAllOwnerAccount() {
        QueryWrapper<OwnerAccount> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", Status.VALID.getCode());
        return ownerAccountDAO.selectList(queryWrapper);
    }

    /**
     * 查询owner股票
     * 
     * @param owner 账号
     * @return owner股票
     */
    public List<OwnerSecurity> queryOwnerSecurity(String owner) {
        QueryWrapper<OwnerSecurity> query = new QueryWrapper<>();
        query.eq("owner", owner)
                .eq("status", Status.VALID.getCode());
        return ownerSecurityDAO.selectList(query);
    }

    /**
     * 查询owner策略
     * 
     * @param owner 账号
     * @return owner策略
     */
    public List<OwnerStrategy> queryOwnerStrategy(String owner) {
        return ownerStrategyDAO.queryOwnerStrategies(owner);
    }

    /**
     * 查询owner订单
     * 
     * @param owner           账号
     * @param platformOrderId 平台订单ID
     * @param platformFillId  平台成交ID
     * @return owner订单
     */
    public OwnerOrder queryOwnerOrder(String owner, String platformOrderId, String platformFillId) {
        List<OwnerOrder> ownerOrderList = ownerOrderDAO.queryOwnerPlatformOrder(owner, platformOrderId);
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

    /**
     * 查询owner策略订单
     * 
     * @param strategy 策略
     * @return owner策略订单
     */
    public List<OwnerOrder> queryStrategyOrder(OwnerStrategy strategy) {
        List<OwnerOrder> ownerOrders = ownerOrderDAO.queryOwnerStrategyOrder(strategy.getOwner(),
                strategy.getStrategyId());
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

    /**
     * 计算基础数据
     * 
     * @param strategy    策略
     * @param ownerOrders owner订单
     */
    private void calculateBasic(OwnerStrategy strategy, List<OwnerOrder> ownerOrders) {
        if (null == ownerOrders || ownerOrders.isEmpty()) {
            return;
        }

        // 计算提交的交易单是否已经平仓用
        Map<String, List<OwnerOrder>> codeOrdersMap = ownerOrders.stream()
                .collect(Collectors.groupingBy(OwnerOrder::getCode));
        Map<String, Boolean> orderClose = new HashMap<>();
        for (Map.Entry<String, List<OwnerOrder>> codeOrders : codeOrdersMap.entrySet()) {
            String code = codeOrders.getKey();
            List<OwnerOrder> orders = codeOrders.getValue();
            // 已成交的订单买入卖出的数量是否为0
            List<OwnerOrder> successOrders = orders.stream()
                    .filter(OwnerOrder::isTraded)
                    .toList();
            if (successOrders.isEmpty()) {
                continue;
            }
            BigDecimal totalQuantity = successOrders.stream()
                    .map(order -> new BigDecimal(order.getQuantity())
                            .multiply(new BigDecimal(TradeSide.of(order.getSide()).getSign())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (BigDecimal.ZERO.equals(totalQuantity)) {
                orderClose.put(code, true);
            }
        }

        for (OwnerOrder ownerOrder : ownerOrders) {
            if (!ownerOrder.getCode().equals(ownerOrder.getUnderlyingCode())) {
                BigDecimal totalIncome = NumberUtils.scale(ownerOrder.getPrice()
                        .multiply(new BigDecimal(ownerOrder.getQuantity()))
                        .multiply(new BigDecimal(strategy.getLotSize()))
                        .multiply(new BigDecimal(TradeSide.of(ownerOrder.getSide()).getSign())));
                // 订单收益
                ownerOrder.getExt().put(OrderExt.TOTAL_INCOME.getKey(), totalIncome.toPlainString());
            }

            // 订单是否平仓
            boolean currentOrderClose = OwnerOrder.isClose(ownerOrder);
            Boolean isClose = currentOrderClose || orderClose.getOrDefault(ownerOrder.getCode(), false);
            ownerOrder.getExt().put(OrderExt.IS_CLOSE.getKey(), String.valueOf(isClose));

            // 计算期权到期日ownerOrder.getStrikeTime()和now的间隔天数
            long daysToExpiration = OwnerOrder.daysToExpiration(ownerOrder);
            ownerOrder.getExt().put(OrderExt.CUR_DTE.getKey(), String.valueOf(daysToExpiration));

        }

    }

    /**
     * 计算利润
     * 
     * @param ownerOrders owner订单
     */
    private void calculateProfit(List<OwnerOrder> ownerOrders) {
        if (null == ownerOrders || ownerOrders.isEmpty()) {
            return;
        }
        List<Security> securityList = new ArrayList<>();
        for (OwnerOrder ownerOrder : ownerOrders) {
            if (OwnerOrder.isClose(ownerOrder)) {
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
            Optional<OptionsRealtimeData> any = optionsBasicInfo.stream()
                    .filter(options -> options.getSecurity().equals(security))
                    .findAny();
            if (any.isEmpty()) {
                continue;
            }
            BigDecimal curPrice = any.get().getCurPrice();
            ownerOrder.getExt().put(OrderExt.CUR_PRICE.getKey(), curPrice.toPlainString());
            if (OwnerOrder.isSell(ownerOrder)) {
                BigDecimal profitRatio = ownerOrder.getPrice()
                        .subtract(curPrice)
                        .divide(ownerOrder.getPrice(), 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal(100));
                profitRatio = NumberUtils.scale(profitRatio);
                ownerOrder.getExt().put(OrderExt.PROFIT_RATIO.getKey(), profitRatio.toPlainString());
            }
        }
    }
}
