package me.dingtou.options.manager;

import me.dingtou.options.constant.OrderExt;
import me.dingtou.options.constant.OrderStatus;
import me.dingtou.options.constant.TradeFrom;
import me.dingtou.options.constant.TradeSide;
import me.dingtou.options.dao.OwnerOrderDAO;
import me.dingtou.options.dao.OwnerStrategyDAO;
import me.dingtou.options.gateway.OptionsTradeGateway;
import me.dingtou.options.gateway.SecurityOrderBookGateway;
import me.dingtou.options.model.*;
import org.apache.commons.lang3.StringUtils;
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
    private OwnerStrategyDAO ownerStrategyDAO;

    @Autowired
    private OptionsTradeGateway optionsTradeGateway;

    @Autowired
    private SecurityOrderBookGateway securityOrderBookGateway;

    /**
     * 执行交易操作
     *
     * @param account  账号
     * @param strategy 交易策略
     * @param side     交易方向，买或卖
     * @param quantity 交易数量
     * @param price    交易价格
     * @param options  交易选项配置
     * @return 返回执行后的订单对象
     */
    @Transactional(rollbackFor = Exception.class)
    public OwnerOrder trade(OwnerAccount account, OwnerStrategy strategy, TradeSide side, Integer quantity,
            BigDecimal price, Options options) {
        // 获取基础配置中的证券信息
        Security security = options.getBasic().getSecurity();

        // 创建并初始化订单对象
        Date now = new Date();
        OwnerOrder ownerOrder = new OwnerOrder();
        ownerOrder.setStrategyId(strategy.getStrategyId());
        ownerOrder.setUnderlyingCode(strategy.getCode());
        ownerOrder.setOwner(strategy.getOwner());
        ownerOrder.setMarket(security.getMarket());
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
        ownerOrder.setSide(side.getCode());
        ownerOrder.setStatus(OrderStatus.WAITING_SUBMIT.getCode());
        ownerOrder.setTradeFrom(TradeFrom.SYS_CREATE.getCode());

        ownerOrder.setExtValue(OrderExt.SOURCE_OPTIONS, options);
        ownerOrder.setExtValue(OrderExt.LOT_SIZE, strategy.getLotSize());

        // 将订单信息插入数据库
        ownerOrder.setCreateTime(now);
        ownerOrder.setUpdateTime(now);
        ownerOrderDAO.insert(ownerOrder);

        // 通过交易网关执行交易操作
        ownerOrder = optionsTradeGateway.trade(account, ownerOrder);
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
     * @param hisOrder 历史订单
     * @param price    交易价格
     * @return 订单
     */
    @Transactional(rollbackFor = Exception.class)
    public OwnerOrder close(OwnerAccount account, OwnerOrder hisOrder, BigDecimal price) {
        // 创建并初始化订单对象
        OwnerOrder ownerOrder = hisOrder.clone();
        Date now = new Date();
        ownerOrder.setTradeTime(now);
        ownerOrder.setCreateTime(now);
        ownerOrder.setUpdateTime(now);
        ownerOrder.setPrice(price);
        ownerOrder.setSide(TradeSide.of(hisOrder.getSide()).getReverseCode());
        ownerOrder.setStatus(OrderStatus.WAITING_SUBMIT.getCode());
        ownerOrder.setTradeFrom(TradeFrom.SYS_CLOSE.getCode());

        ownerOrder.setExtValue(OrderExt.SOURCE_ORDER, hisOrder);

        // 将订单信息插入数据库
        ownerOrderDAO.insert(ownerOrder);

        // 通过交易网关执行交易操作
        ownerOrder = optionsTradeGateway.trade(account, ownerOrder);
        ownerOrder.setStatus(OrderStatus.SUBMITTED.getCode());
        // 更新数据库中的订单信息
        ownerOrder.setUpdateTime(new Date());
        ownerOrderDAO.updateById(ownerOrder);
        return ownerOrder;
    }

    @Transactional(rollbackFor = Exception.class)
    public OwnerOrder cancel(OwnerAccount account, OwnerOrder dbOrder) {
        dbOrder = optionsTradeGateway.cancel(account, dbOrder);
        dbOrder.setStatus(OrderStatus.CANCELLED_ALL.getCode());
        dbOrder.setUpdateTime(new Date());
        int update = ownerOrderDAO.updateById(dbOrder);
        if (update != 1) {
            throw new RuntimeException("cancel order error");
        }
        return dbOrder;
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean delete(OwnerAccount account, OwnerOrder oldOrder) {
        if (null == oldOrder || !Objects.equals(oldOrder.getStatus(), OrderStatus.CANCELLED_ALL.getCode())) {
            return false;
        }
        int updateRow = 0;
        if (optionsTradeGateway.delete(account, oldOrder)) {
            List<Long> orderIds = new ArrayList<>();
            orderIds.add(oldOrder.getId());
            updateRow = ownerOrderDAO.deleteByIds(orderIds);
        }
        return updateRow != 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean syncOrder(Owner owner) {
        Date now = new Date();
        // 本地所有订单
        List<OwnerOrder> dbOrders = ownerOrderDAO.queryOwnerOrder(owner.getOwner());
        // 拉取平台订单列表1:1
        List<OwnerOrder> platformOrders = optionsTradeGateway.pullOrder(owner);
        // 拉取平台成交单列表1:n
        List<OwnerOrder> platformOrderFills = optionsTradeGateway.pullOrderFill(owner);
        // 获取平台订单手续费
        Map<String, BigDecimal> feeMap = optionsTradeGateway.totalFee(owner.getAccount(), platformOrderFills);

        // 组合订单platformOrders中拉取不到，可以通过成交单拉取。

        // 平台订单订单号唯一
        Map<String, OwnerOrder> plateformOrderMap = new HashMap<>();
        platformOrders.forEach(platformOrder -> {
            plateformOrderMap.put(platformOrder.getPlatformOrderId(), platformOrder);
        });

        // 平台订单成交单一个订单号对应多条成交单
        Map<String, List<OwnerOrder>> platformOrderFillMap = platformOrderFills.stream()
                .collect(Collectors.groupingBy(OwnerOrder::getPlatformOrderId));

        // 使用过的成交单
        Map<String, List<OwnerOrder>> usedPlatformOrderFillMap = new HashMap<>();

        // 新订单
        List<OwnerOrder> platformNewOrders = new ArrayList<>();

        // 更新本地订单
        for (OwnerOrder dbOrder : dbOrders) {
            String platformOrderId = dbOrder.getPlatformOrderId();
            // 通过平台订单来更新本地订单
            OwnerOrder platformOrder = plateformOrderMap.get(platformOrderId);
            if (null != platformOrder) {
                dbOrder.setQuantity(platformOrder.getQuantity());
                // 提前指派后不再更新订单状态
                if (!OrderStatus.EARLY_ASSIGNED.equals(OrderStatus.of(platformOrder.getStatus()))) {
                    dbOrder.setStatus(platformOrder.getStatus());
                }
                dbOrder.setTradeTime(platformOrder.getTradeTime());
                dbOrder.setStrikeTime(platformOrder.getStrikeTime());
                dbOrder.setPlatformOrderIdEx(platformOrder.getPlatformOrderIdEx());
                plateformOrderMap.remove(platformOrderId);
            }

            boolean subOrder = false;

            List<OwnerOrder> orderFills = platformOrderFillMap.get(platformOrderId);
            if (null == orderFills) {
                subOrder = true;
                orderFills = usedPlatformOrderFillMap.get(platformOrderId);
            }
            if (null != orderFills && !orderFills.isEmpty()) {
                OwnerOrder orderFill = null;
                // 一笔成交单直接赋值
                if (orderFills.size() == 1) {
                    orderFill = orderFills.get(0);
                } else {
                    // 多比成交单 如果订单的成交单id不为空就从平台订单中找到对应的订单走更新逻辑
                    if (null != dbOrder.getPlatformFillId()) {
                        // 找到对应的成交单
                        Optional<OwnerOrder> orderOptional = orderFills.stream()
                                .filter(order -> order.getPlatformFillId().equals(dbOrder.getPlatformFillId()))
                                .findAny();
                        if (orderOptional.isPresent()) {
                            orderFill = orderOptional.get();
                        }
                    } else {
                        // 一笔订单生成了多笔成交单
                        orderFill = orderFills.get(0);
                        orderFill.setSubOrder(false);
                        List<OwnerOrder> subOrders = orderFills.subList(1, orderFills.size());
                        subOrders.forEach(order -> order.setSubOrder(true));
                        platformNewOrders.addAll(subOrders);
                    }
                }
                List<OwnerOrder> removed = platformOrderFillMap.remove(platformOrderId);
                if (null != removed) {
                    usedPlatformOrderFillMap.put(platformOrderId, removed);
                }

                if (null != orderFill) {
                    dbOrder.setSubOrder(subOrder);
                    dbOrder.setPrice(orderFill.getPrice());
                    dbOrder.setQuantity(orderFill.getQuantity());
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
                dbOrder.setOrderFee(feeMap.getOrDefault(dbOrder.getPlatformOrderIdEx(), BigDecimal.ZERO));
            }
            dbOrder.setUpdateTime(now);
            if (null != dbOrder.getStrategyId()) {
                OwnerStrategy strategy = owner.getStrategyList()
                        .stream()
                        .filter(s -> s.getStrategyId().equals(dbOrder.getStrategyId()))
                        .findFirst()
                        .orElse(null);
                if (null != strategy) {
                    dbOrder.setExtValue(OrderExt.LOT_SIZE, strategy.getLotSize());
                }
            }

            ownerOrderDAO.updateById(dbOrder);
        }

        // 新订单
        platformNewOrders.addAll(plateformOrderMap.values().stream().toList());
        for (OwnerOrder platformNewOrder : platformNewOrders) {
            String platformOrderId = platformNewOrder.getPlatformOrderId();
            List<OwnerOrder> ownerOrderFills = platformOrderFillMap.get(platformOrderId);
            if (null != ownerOrderFills) {
                if (ownerOrderFills.size() == 1) {
                    OwnerOrder ownerOrderFill = ownerOrderFills.get(0);
                    platformNewOrder.setPlatformFillId(ownerOrderFill.getPlatformFillId());
                }
                platformOrderFillMap.remove(platformOrderId);
            }
        }

        List<OwnerOrder> newOrders = new ArrayList<>();
        newOrders.addAll(platformNewOrders);
        // 处理未使用过的订单 第一单为主订单，其他为子订单。
        for (Map.Entry<String, List<OwnerOrder>> entries : platformOrderFillMap.entrySet()) {
            List<OwnerOrder> orderFills = entries.getValue();
            for (int i = 0; i < orderFills.size(); i++) {
                boolean subOrder = i != 0;
                OwnerOrder platformNewOrderFill = orderFills.get(i);
                platformNewOrderFill.setSubOrder(subOrder);
                newOrders.add(platformNewOrderFill);
            }
        }

        for (OwnerOrder newOrder : newOrders) {
            if (Boolean.TRUE.equals(newOrder.getSubOrder())) {
                newOrder.setOrderFee(BigDecimal.ZERO);
            } else {
                newOrder.setOrderFee(feeMap.getOrDefault(newOrder.getPlatformOrderIdEx(), BigDecimal.ZERO));
            }
            ownerOrderDAO.insert(newOrder);
        }
        return Boolean.TRUE;

    }

    public OwnerStrategy queryStrategy(String ownerStrategyId) {
        return ownerStrategyDAO.queryStrategyByStrategyId(ownerStrategyId);
    }

    public BigDecimal queryTotalOrderFee(OwnerAccount account, List<OwnerOrder> ownerOrders) {
        BigDecimal totalFee = ownerOrders.stream()
                .map(OwnerOrder::getOrderFee)
                .filter(Objects::nonNull)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);
        if (!BigDecimal.ZERO.equals(totalFee)) {
            return totalFee;
        }
        Map<String, BigDecimal> totalFeeMap = optionsTradeGateway.totalFee(account, ownerOrders);
        return totalFeeMap.values().stream().reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
    }

    public SecurityOrderBook querySecurityOrderBook(String code, Integer market) {
        if (StringUtils.isBlank(code) || null == market) {
            return null;
        }
        Security security = Security.of(code, market);
        return securityOrderBookGateway.getOrderBook(security);
    }

    public List<OwnerOrder> queryDraftOrder(String owner) {
        // 本地所有未挂靠订单
        return ownerOrderDAO.queryOwnerDraftOrder(owner);
    }

    @Transactional(rollbackFor = Exception.class)
    public Integer updateOrderStrategy(OwnerAccount account, List<Long> orderIds, OwnerStrategy strategy) {
        int num = 0;
        for (Long orderId : orderIds) {
            OwnerOrder dbOrder = ownerOrderDAO.queryOwnerOrderById(account.getOwner(), orderId);
            if (null == dbOrder || !account.getOwner().equals(dbOrder.getOwner())) {
                continue;
            }
            dbOrder.setStrategyId(strategy.getStrategyId());
            dbOrder.setExtValue(OrderExt.LOT_SIZE, strategy.getLotSize());
            num += ownerOrderDAO.updateById(dbOrder);
        }
        return num;
    }

    /**
     * 修改订单状态
     *
     * @param account 账户
     * @param orderId 订单ID
     * @param status  新状态
     * @return 是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateOrderStatus(OwnerAccount account, Long orderId, OrderStatus status) {
        OwnerOrder dbOrder = ownerOrderDAO.queryOwnerOrderById(account.getOwner(), orderId);
        if (null == dbOrder || !account.getOwner().equals(dbOrder.getOwner())) {
            return false;
        }
        dbOrder.setStatus(status.getCode());
        dbOrder.setUpdateTime(new Date());
        return ownerOrderDAO.updateById(dbOrder) > 0;
    }
}
