package me.dingtou.options.manager;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import me.dingtou.options.constant.OrderExt;
import me.dingtou.options.constant.OrderStatus;
import me.dingtou.options.constant.TradeFrom;
import me.dingtou.options.dao.OwnerOrderDAO;
import me.dingtou.options.dao.OwnerStrategyDAO;
import me.dingtou.options.gateway.OptionsTradeGateway;
import me.dingtou.options.model.Options;
import me.dingtou.options.model.OwnerOrder;
import me.dingtou.options.model.OwnerStrategy;
import me.dingtou.options.model.Security;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class TradeManager {

    @Autowired
    private OwnerOrderDAO ownerOrderDAO;

    @Autowired
    private OptionsTradeGateway optionsTradeGateway;

    @Autowired
    private OwnerStrategyDAO ownerStrategyDAO;

    /**
     * 执行交易操作
     *
     * @param ownerStrategy 交易策略
     * @param side          交易方向，买或卖
     * @param quantity      交易数量
     * @param price         交易价格
     * @param options       交易选项配置
     * @return 返回执行后的订单对象
     */
    @Transactional(rollbackFor = Exception.class)
    public OwnerOrder trade(OwnerStrategy ownerStrategy, int side, Integer quantity, BigDecimal price, Options options) {
        // 获取基础配置中的证券信息
        Security security = options.getBasic().getSecurity();

        // 创建并初始化订单对象
        Date now = new Date();
        OwnerOrder ownerOrder = new OwnerOrder();
        ownerOrder.setStrategyId(ownerStrategy.getStrategyId());
        ownerOrder.setUnderlyingCode(ownerStrategy.getCode());
        ownerOrder.setPlatform(ownerStrategy.getPlatform());
        ownerOrder.setOwner(ownerStrategy.getOwner());
        ownerOrder.setMarket(security.getMarket());
        ownerOrder.setAccountId(ownerStrategy.getAccountId());
        ownerOrder.setTradeTime(now);
        SimpleDateFormat strikeTimeFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date strikeTime = null;
        try {
            strikeTime = strikeTimeFormat.parse(options.getOptionExData().getStrikeTime());
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        ownerOrder.setStrikeTime(strikeTime);
        ownerOrder.setCode(security.getCode());
        ownerOrder.setQuantity(quantity);
        ownerOrder.setPrice(price);
        ownerOrder.setSide(side);
        ownerOrder.setStatus(OrderStatus.WAITING_SUBMIT.getCode());
        ownerOrder.setTradeFrom(TradeFrom.SYS_CREATE.getCode());
        Map<String, String> ext = new HashMap<>();
        ext.put(OrderExt.SOURCE_OPTIONS.getCode(), OrderExt.SOURCE_OPTIONS.toString(options));
        ownerOrder.setExt(ext);

        // 将订单信息插入数据库
        ownerOrder.setCreateTime(now);
        ownerOrder.setUpdateTime(now);
        ownerOrderDAO.insert(ownerOrder);

        // 通过交易网关执行交易操作
        ownerOrder = optionsTradeGateway.trade(ownerOrder);
        ownerOrder.setStatus(OrderStatus.SUBMITTED.getCode());
        // 更新数据库中的订单信息
        ownerOrder.setUpdateTime(new Date());
        ownerOrderDAO.updateById(ownerOrder);

        // 返回执行后的订单对象
        return ownerOrder;
    }

    /**
     * 平仓
     *
     * @param ownerStrategy 交易策略
     * @param side          交易方向，买或卖
     * @param quantity      交易数量
     * @param price         交易价格
     * @param hisOrder      历史订单
     * @return 订单
     */
    @Transactional(rollbackFor = Exception.class)
    public OwnerOrder close(OwnerStrategy ownerStrategy, int side, Integer quantity, BigDecimal price, OwnerOrder hisOrder) {
        // 创建并初始化订单对象
        Date now = new Date();
        OwnerOrder ownerOrder = new OwnerOrder();
        ownerOrder.setStrategyId(ownerStrategy.getStrategyId());
        ownerOrder.setUnderlyingCode(ownerStrategy.getCode());
        ownerOrder.setPlatform(ownerStrategy.getPlatform());
        ownerOrder.setOwner(ownerStrategy.getOwner());
        ownerOrder.setAccountId(ownerStrategy.getAccountId());
        ownerOrder.setMarket(hisOrder.getMarket());
        ownerOrder.setTradeTime(now);
        ownerOrder.setStrikeTime(hisOrder.getStrikeTime());
        ownerOrder.setCode(hisOrder.getCode());
        ownerOrder.setQuantity(quantity);
        ownerOrder.setPrice(price);
        ownerOrder.setSide(side);
        ownerOrder.setStatus(OrderStatus.WAITING_SUBMIT.getCode());
        ownerOrder.setTradeFrom(TradeFrom.SYS_CLOSE.getCode());
        Map<String, String> ext = new HashMap<>();
        ext.put(OrderExt.SOURCE_ORDER.getCode(), OrderExt.SOURCE_ORDER.toString(hisOrder));
        ownerOrder.setExt(ext);

        // 将订单信息插入数据库
        ownerOrder.setCreateTime(now);
        ownerOrder.setUpdateTime(now);
        ownerOrderDAO.insert(ownerOrder);

        // 通过交易网关执行交易操作
        ownerOrder = optionsTradeGateway.trade(ownerOrder);
        ownerOrder.setStatus(OrderStatus.SUBMITTED.getCode());
        // 更新数据库中的订单信息
        ownerOrder.setUpdateTime(new Date());
        ownerOrderDAO.updateById(ownerOrder);
        return ownerOrder;
    }

    @Transactional(rollbackFor = Exception.class)
    public OwnerOrder cancel(OwnerOrder dbOrder) {
        optionsTradeGateway.cancel(dbOrder);
        dbOrder.setStatus(OrderStatus.CANCELLED_ALL.getCode());
        dbOrder.setUpdateTime(new Date());
        int update = ownerOrderDAO.updateById(dbOrder);
        if (update != 1) {
            throw new RuntimeException("cancel order error");
        }
        return dbOrder;
    }

    @Transactional(rollbackFor = Exception.class)
    public List<OwnerOrder> syncOrder(OwnerStrategy strategy) {
        Date now = new Date();
        QueryWrapper<OwnerOrder> queryOrder = new QueryWrapper<>();
        queryOrder.eq("owner", strategy.getOwner());
        queryOrder.eq("strategy_id", strategy.getStrategyId());
        // 本地订单
        List<OwnerOrder> dbOrders = ownerOrderDAO.selectList(queryOrder);
        // 拉取平台订单列表1:1
        List<OwnerOrder> platformOrders = optionsTradeGateway.pullOrder(strategy);
        // 拉取平台成交单列表1:n
        List<OwnerOrder> platformOrderFills = optionsTradeGateway.pullOrderFill(strategy);
        // 获取平台订单手续费
        Map<String, BigDecimal> feeMap = optionsTradeGateway.totalFee(strategy, platformOrderFills);

        // 组合订单platformOrders中拉取不到，成交单中会有多条记录

        // 平台订单订单号唯一
        Map<String, OwnerOrder> orderMap = new HashMap<>();
        platformOrders.forEach(platformOrder -> {
            orderMap.put(platformOrder.getPlatformOrderId(), platformOrder);
        });

        // 平台订单成交单一个订单号对应多条成交单
        Map<String, List<OwnerOrder>> orderFillMap = platformOrderFills.stream().collect(Collectors.groupingBy(OwnerOrder::getPlatformOrderId));
        // 使用过的成交单
        Map<String, List<OwnerOrder>> usedOrderFillMap = new HashMap<>();


        // 更新本地订单
        for (OwnerOrder dbOrder : dbOrders) {
            String platformOrderId = dbOrder.getPlatformOrderId();
            OwnerOrder platformOrder = orderMap.get(platformOrderId);
            if (null != platformOrder) {
                dbOrder.setQuantity(platformOrder.getQuantity());
                dbOrder.setStatus(platformOrder.getStatus());
                dbOrder.setTradeTime(platformOrder.getTradeTime());
                dbOrder.setStrikeTime(platformOrder.getStrikeTime());
                dbOrder.setPlatformOrderIdEx(platformOrder.getPlatformOrderIdEx());
                orderMap.remove(platformOrderId);
            }

            boolean subOrder = false;
            List<OwnerOrder> orderFills = orderFillMap.get(platformOrderId);
            if (null == orderFills) {
                subOrder = true;
                orderFills = usedOrderFillMap.get(platformOrderId);
            }
            if (null != orderFills && !orderFills.isEmpty()) {
                OwnerOrder orderFill = null;
                if (orderFills.size() == 1) {
                    orderFill = orderFills.get(0);
                } else {
                    Optional<OwnerOrder> orderOptional = orderFills.stream().filter(order -> order.getPlatformFillId().equals(dbOrder.getPlatformFillId())).findAny();
                    if (orderOptional.isPresent()) {
                        orderFill = orderOptional.get();
                    }
                }
                List<OwnerOrder> removed = orderFillMap.remove(platformOrderId);
                if (null != removed) {
                    usedOrderFillMap.put(platformOrderId, removed);
                }

                if (null != orderFill) {
                    dbOrder.setSubOrder(subOrder);
                    dbOrder.setPrice(orderFill.getPrice());
                    dbOrder.setTradeTime(orderFill.getTradeTime());
                    dbOrder.setStrikeTime(orderFill.getStrikeTime());
                    dbOrder.setPlatformFillId(orderFill.getPlatformFillId());
                    dbOrder.setPlatformOrderIdEx(orderFill.getPlatformOrderIdEx());
                }
            }
        }
        for (OwnerOrder dbOrder : dbOrders) {
            if (Boolean.TRUE.equals(dbOrder.getSubOrder())) {
                dbOrder.setOrderFee(BigDecimal.ZERO);
            } else {
                dbOrder.setOrderFee(feeMap.get(dbOrder.getPlatformOrderIdEx()));
            }
            dbOrder.setUpdateTime(now);
            ownerOrderDAO.updateById(dbOrder);
        }


        // 新订单
        List<OwnerOrder> platformNewOrders = orderMap.values().stream().toList();
        for (OwnerOrder platformNewOrder : platformNewOrders) {
            String platformOrderId = platformNewOrder.getPlatformOrderId();
            List<OwnerOrder> ownerOrderFills = orderFillMap.get(platformOrderId);
            if (null != ownerOrderFills) {
                if (ownerOrderFills.size() == 1) {
                    OwnerOrder ownerOrderFill = ownerOrderFills.get(0);
                    platformNewOrder.setPlatformFillId(ownerOrderFill.getPlatformFillId());
                }
                orderFillMap.remove(platformOrderId);
            }
        }


        List<OwnerOrder> newOrders = new ArrayList<>();
        newOrders.addAll(platformNewOrders);
        // 处理未使用过的订单 第一单为主订单，其他为子订单。
        for (Map.Entry<String, List<OwnerOrder>> entries : orderFillMap.entrySet()) {
            List<OwnerOrder> orderFills = entries.getValue();
            for (int i = 0; i < orderFills.size(); i++) {
                OwnerOrder platformNewOrderFill = orderFills.get(i);
                platformNewOrderFill.setSubOrder(i == 0 ? false : true);
                newOrders.add(platformNewOrderFill);
            }
        }

        for (OwnerOrder newOrder : newOrders) {
            if (Boolean.TRUE.equals(newOrder.getSubOrder())) {
                newOrder.setOrderFee(BigDecimal.ZERO);
            } else {
                newOrder.setOrderFee(feeMap.get(newOrder.getPlatformOrderIdEx()));
            }
            newOrder.setOrderFee(feeMap.get(newOrder.getPlatformOrderIdEx()));
            ownerOrderDAO.insert(newOrder);
        }
        List<OwnerOrder> allOrders = new ArrayList<>();
        allOrders.addAll(dbOrders);
        allOrders.addAll(newOrders);
        return allOrders;

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

    public BigDecimal queryTotalOrderFee(OwnerStrategy strategy, List<OwnerOrder> ownerOrders) {
        Map<String, BigDecimal> totalFeeMap = optionsTradeGateway.totalFee(strategy, ownerOrders);
        return totalFeeMap.values().stream().reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
    }

}
