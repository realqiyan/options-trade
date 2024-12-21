package me.dingtou.options.manager;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.constant.TradeSide;
import me.dingtou.options.dao.OwnerOrderDAO;
import me.dingtou.options.dao.OwnerStrategyDAO;
import me.dingtou.options.gateway.OptionsTradeGateway;
import me.dingtou.options.gateway.futu.executor.QueryExecutor;
import me.dingtou.options.gateway.futu.executor.func.query.FuncGetOptionsRealtimeData;
import me.dingtou.options.model.*;
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
        List<OwnerOrder> ownerOrderList = queryOwnerOrder(owner);
        if (null != ownerOrderList && !ownerOrderList.isEmpty()) {
            ownerObj.setOrderList(ownerOrderList);

            List<Security> securityList = new ArrayList<>();
            for (OwnerOrder ownerOrder : ownerOrderList) {
                Date now = new Date();
                if (ownerOrder.getStrikeTime().before(now)) {
                    continue;
                }
                securityList.add(Security.of(ownerOrder.getCode(), ownerOrder.getMarket()));
            }

            if (!securityList.isEmpty()) {
                List<OptionsRealtimeData> optionsBasicInfo = QueryExecutor.query(new FuncGetOptionsRealtimeData(securityList));
                if (optionsBasicInfo.isEmpty()) {
                    log.warn("query options basic info failed, retry");
                    optionsBasicInfo = QueryExecutor.query(new FuncGetOptionsRealtimeData(securityList));
                }
                if (!optionsBasicInfo.isEmpty()) {
                    // 计算利润
                    for (OwnerOrder ownerOrder : ownerOrderList) {
                        Security security = Security.of(ownerOrder.getCode(), ownerOrder.getMarket());
                        Optional<OptionsRealtimeData> any = optionsBasicInfo.stream().filter(options -> options.getSecurity().equals(security)).findAny();
                        if (any.isPresent()) {
                            BigDecimal curPrice = any.get().getCurPrice();
                            TradeSide tradeSide = TradeSide.of(ownerOrder.getSide());

                            if (TradeSide.SELL.equals(tradeSide)) {
                                BigDecimal profitRatio = ownerOrder.getPrice().subtract(curPrice).divide(ownerOrder.getPrice(), 4, RoundingMode.HALF_UP).multiply(new BigDecimal(100)).setScale(2, RoundingMode.HALF_UP);
                                if (null == ownerOrder.getExt()) {
                                    ownerOrder.setExt(new HashMap<>());
                                }
                                ownerOrder.getExt().put("curPrice", curPrice.toString());
                                ownerOrder.getExt().put("profitRatio", profitRatio.toString());
                            }
                        }
                    }
                }
            }
        }

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

    public List<OwnerOrder> queryOwnerOrder(String owner, String strategyId) {
        QueryWrapper<OwnerOrder> queryOrder = new QueryWrapper<>();
        queryOrder.eq("owner", owner);
        queryOrder.eq("strategy_id", strategyId);
        return ownerOrderDAO.selectList(queryOrder);
    }
}
