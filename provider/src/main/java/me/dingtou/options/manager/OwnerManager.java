package me.dingtou.options.manager;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.constant.OrderExt;
import me.dingtou.options.constant.TradeSide;
import me.dingtou.options.dao.OwnerOrderDAO;
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
import java.util.*;


@Slf4j
@Component
public class OwnerManager {

    @Autowired
    private OwnerStrategyDAO ownerStrategyDAO;

    @Autowired
    private OwnerOrderDAO ownerOrderDAO;

    @Autowired
    private OptionsTradeGateway optionsTradeGateway;


    public Owner queryOwner(String owner) {
        Owner ownerObj = new Owner();
        ownerObj.setOwner(owner);
        ownerObj.setStrategyList(queryOwnerStrategy(owner));
        return ownerObj;
    }


    public List<OwnerStrategy> queryOwnerStrategy(String owner) {
        QueryWrapper<OwnerStrategy> querySecurity = new QueryWrapper<>();
        querySecurity.eq("owner", owner);
        return ownerStrategyDAO.selectList(querySecurity);
    }

    public List<OwnerOrder> queryOwnerOrder(String owner) {
        QueryWrapper<OwnerOrder> queryOrder = new QueryWrapper<>();
        queryOrder.eq("owner", owner);
        return ownerOrderDAO.selectList(queryOrder);
    }

    public OwnerOrder queryOwnerOrder(String owner, String platform, String platformOrderId) {
        QueryWrapper<OwnerOrder> queryOrder = new QueryWrapper<>();
        queryOrder.eq("owner", owner);
        queryOrder.eq("platform", platform);
        queryOrder.eq("platform_order_id", platformOrderId);
        List<OwnerOrder> ownerOrderList = ownerOrderDAO.selectList(queryOrder);
        if (null != ownerOrderList && ownerOrderList.size() == 1) {
            return ownerOrderList.get(0);
        }
        throw new RuntimeException("query owner order error");
    }

    public List<OwnerOrder> queryStrategyOrder(OwnerStrategy strategy) {
        QueryWrapper<OwnerOrder> queryOrder = new QueryWrapper<>();
        queryOrder.eq("owner", strategy.getOwner());
        queryOrder.eq("strategy_id", strategy.getStrategyId());
        List<OwnerOrder> ownerOrders = ownerOrderDAO.selectList(queryOrder);
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
        for (OwnerOrder ownerOrder : ownerOrders) {
            BigDecimal totalIncome = ownerOrder.getPrice()
                    .multiply(new BigDecimal(ownerOrder.getQuantity()))
                    .multiply(new BigDecimal(strategy.getLotSize()))
                    .multiply(new BigDecimal(TradeSide.of(ownerOrder.getSide()).getSign()));
            ownerOrder.getExt().put(OrderExt.TOTAL_INCOME.getCode(), totalIncome.toString());

            // 订单是不是完结
            if (ownerOrder.getStrikeTime().before(now) && !DateUtils.isSameDay(ownerOrder.getStrikeTime(), now)) {
                ownerOrder.getExt().put(OrderExt.CUR_STATUS.getCode(), "finished");
            }
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
            securityList.add(Security.of(ownerOrder.getCode(), ownerOrder.getMarket()));
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
            TradeSide tradeSide = TradeSide.of(ownerOrder.getSide());

            if (TradeSide.SELL.equals(tradeSide) || TradeSide.SELL_SHORT.equals(tradeSide)) {
                BigDecimal profitRatio = ownerOrder.getPrice().subtract(curPrice).divide(ownerOrder.getPrice(), 4, RoundingMode.HALF_UP).multiply(new BigDecimal(100)).setScale(2, RoundingMode.HALF_UP);
                ownerOrder.getExt().put(OrderExt.CUR_PRICE.getCode(), curPrice.toString());
                ownerOrder.getExt().put(OrderExt.PROFIT_RATIO.getCode(), profitRatio.toString());
            }

        }

    }
}
