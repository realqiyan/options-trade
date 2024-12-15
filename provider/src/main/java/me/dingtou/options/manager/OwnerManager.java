package me.dingtou.options.manager;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import me.dingtou.options.dao.OwnerOrderDAO;
import me.dingtou.options.dao.OwnerStrategyDAO;
import me.dingtou.options.gateway.OptionsTradeGateway;
import me.dingtou.options.model.Owner;
import me.dingtou.options.model.OwnerOrder;
import me.dingtou.options.model.OwnerStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        QueryWrapper<OwnerStrategy> querySecurity = new QueryWrapper<>();
        querySecurity.eq("owner", owner);
        List<OwnerStrategy> ownerStrategyList = ownerStrategyDAO.selectList(querySecurity);
        if (null != ownerStrategyList && !ownerStrategyList.isEmpty()) {
            ownerObj.setStrategyList(ownerStrategyList);
        }
        QueryWrapper<OwnerOrder> queryOrder = new QueryWrapper<OwnerOrder>();
        queryOrder.eq("owner", owner);
        List<OwnerOrder> ownerOrderList = ownerOrderDAO.selectList(queryOrder);
        if (null != ownerOrderList && !ownerOrderList.isEmpty()) {
            ownerObj.setOrderList(ownerOrderList);
        }

        return ownerObj;
    }


    public OwnerStrategy queryStrategy(String ownerStrategyId) {
        QueryWrapper<OwnerStrategy> querySecurity = new QueryWrapper<>();
        querySecurity.eq("strategy_id", ownerStrategyId);
        List<OwnerStrategy> ownerStrategyList = ownerStrategyDAO.selectList(querySecurity);
        if (null == ownerStrategyList || ownerStrategyList.size() != 1) {
            return null;
        }
        return ownerStrategyList.get(0);

    }

    public OwnerOrder queryOwnerOrder(String owner, String platform, String platformOrderId) {
        QueryWrapper<OwnerOrder> queryOrder = new QueryWrapper<OwnerOrder>();
        queryOrder.eq("owner", owner);
        queryOrder.eq("platform", platform);
        queryOrder.eq("platform_order_id", platformOrderId);
        List<OwnerOrder> ownerOrderList = ownerOrderDAO.selectList(queryOrder);
        if (null != ownerOrderList && ownerOrderList.size() == 1) {
            return ownerOrderList.get(0);
        }
        throw new RuntimeException("query owner order error");
    }

    public List<OwnerOrder> syncOrder(String owner) {
        QueryWrapper<OwnerOrder> queryOrder = new QueryWrapper<OwnerOrder>();
        queryOrder.eq("owner", owner);
        // queryOrder.notIn("status", OrderStatus.FILL_CANCELLED.getCode(), OrderStatus.DELETED.getCode(), OrderStatus.DISABLED.getCode(), OrderStatus.FAILED.getCode(), OrderStatus.CANCELLED_ALL.getCode());
        List<OwnerOrder> ownerOrderList = ownerOrderDAO.selectList(queryOrder);
        if (null == ownerOrderList || ownerOrderList.isEmpty()) {
            return Collections.emptyList();
        }

        List<OwnerOrder> result = new ArrayList<>();

        Map<String, List<OwnerOrder>> strategyIdOrderList = ownerOrderList.stream().collect(Collectors.groupingBy(OwnerOrder::getStrategyId));

        for (String strategyId : strategyIdOrderList.keySet()) {
            List<OwnerOrder> orderList = strategyIdOrderList.get(strategyId);
            OwnerStrategy ownerStrategy = queryStrategy(strategyId);
            if (null == ownerStrategy) {
                continue;
            }
            result.addAll(optionsTradeGateway.syncOrder(ownerStrategy, orderList));
        }
        for (OwnerOrder ownerOrder : result) {
            ownerOrderDAO.updateById(ownerOrder);
        }
        return result;

    }
}
