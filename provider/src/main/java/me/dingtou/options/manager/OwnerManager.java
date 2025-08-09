package me.dingtou.options.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
     * 查询所有owner
     *
     * @return owner列表
     */
    public List<Owner> queryAllOwner() {
        List<OwnerAccount> accounts = queryAllOwnerAccount();
        List<Owner> owners = new ArrayList<>();
        for (OwnerAccount account : accounts) {
            Owner owner = queryOwner(account.getOwner());
            owners.add(owner);
        }
        return owners;
    }

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

    public Owner queryOwnerWithOrder(String owner) {
        Owner ownerObj = queryOwner(owner);

        List<OwnerStrategy> strategyList = ownerObj.getStrategyList();
        List<OwnerOrder> ownerOrderList = new ArrayList<>();

        strategyList.parallelStream().forEach(strategy -> {
            List<OwnerOrder> orders = queryStrategyOrder(strategy);
            ownerOrderList.addAll(orders);
        });

        // 过滤出到期未行权的订单
        List<OwnerOrder> unexercisedOrders = ownerOrderList.stream()
            .filter(order -> !order.getCode().equals(order.getUnderlyingCode())) // 是期权订单
            .filter(order -> !OwnerOrder.isClose(order)) // 未平仓
            .filter(order -> OwnerOrder.dte(order) > 0) // 已到期
            .toList();

        // 按标的股票代码分组
        Map<String, List<OwnerOrder>> groupedOrders = unexercisedOrders.stream()
            .collect(Collectors.groupingBy(OwnerOrder::getUnderlyingCode));

        // 将分组后的订单设置到对应的OwnerSecurity中
        List<OwnerSecurity> securityList = ownerObj.getSecurityList();
        if (securityList != null) {
            for (OwnerSecurity security : securityList) {
                // 初始化空列表
                security.setUnexercisedOrders(new ArrayList<>());

                String code = security.getCode();
                List<OwnerOrder> orders = groupedOrders.get(code);
                if (orders != null) {
                    security.setUnexercisedOrders(orders);
                }
            }
        }

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
     * 更新owner账号
     *
     * @param owner 账号
     * @return 更新行
     */
    public int updateOwnerAccount(OwnerAccount account) {
        return ownerAccountDAO.updateById(account);
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
        query.eq("owner", owner).eq("status", Status.VALID.getCode());
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
     * @param owner   账号
     * @param orderId 订单ID
     * @return owner订单
     */
    public OwnerOrder queryOwnerOrder(String owner, Long orderId) {
        return ownerOrderDAO.queryOwnerOrderById(owner, orderId);
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
            .collect(Collectors.groupingBy(OwnerOrder::logicCode));
        Map<String, Boolean> orderClose = new HashMap<>();
        for (Map.Entry<String, List<OwnerOrder>> codeOrders : codeOrdersMap.entrySet()) {
            String code = codeOrders.getKey();
            List<OwnerOrder> orders = codeOrders.getValue();
            // 已成交的订单买入卖出的数量是否为0
            List<OwnerOrder> successOrders = orders.stream().filter(OwnerOrder::isTraded).toList();
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

            // 订单是否平仓
            boolean currentOrderClose = OwnerOrder.isClose(ownerOrder);
            Boolean isClose = currentOrderClose || orderClose.getOrDefault(ownerOrder.logicCode(), false);
            ownerOrder.setExtValue(OrderExt.IS_CLOSE, isClose);

            // 订单标的类型 （Call、Put、Stock）
            ownerOrder.setExtValue(OrderExt.CODE_TYPE, OwnerOrder.codeType(ownerOrder));
            // 订单收益
            BigDecimal totalIncome = OwnerOrder.income(ownerOrder);
            ownerOrder.setExtValue(OrderExt.TOTAL_INCOME, NumberUtils.scale(totalIncome).toPlainString());

            if (OwnerOrder.isOptionsOrder(ownerOrder)) {

                // 计算期权到期日ownerOrder.getStrikeTime()和now的间隔天数
                long daysToExpiration = OwnerOrder.dte(ownerOrder);
                ownerOrder.setExtValue(OrderExt.CUR_DTE, daysToExpiration);

                // 计算期权行权价
                BigDecimal strikePrice = OwnerOrder.strikePrice(ownerOrder);
                ownerOrder.setExtValue(OrderExt.STRIKE_PRICE, strikePrice.toPlainString());

                // 计算期权合约数量
                int lotSize = OwnerOrder.lotSize(ownerOrder);
                ownerOrder.setExtValue(OrderExt.LOT_SIZE, lotSize);

                // 计算期权类型
                if (OwnerOrder.isPut(ownerOrder)) {
                    ownerOrder.setExtValue(OrderExt.IS_PUT, true);
                }
                if (OwnerOrder.isCall(ownerOrder)) {
                    ownerOrder.setExtValue(OrderExt.IS_CALL, true);
                }
            }
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
            log.warn("query FuncGetOptionsRealtimeData failed, retry");
            optionsBasicInfo = QueryExecutor.query(new FuncGetOptionsRealtimeData(securityList));
        }
        if (optionsBasicInfo.isEmpty()) {
            return;
        }
        // 计算利润
        for (OwnerOrder ownerOrder : ownerOrders) {
            Security security = Security.of(ownerOrder.getCode(), ownerOrder.getMarket());
            Optional<OptionsRealtimeData> any = optionsBasicInfo.stream()
                .filter(options -> options.getSecurity().equals(security)).findAny();
            if (any.isEmpty()) {
                continue;
            }
            BigDecimal curPrice = any.get().getCurPrice();
            ownerOrder.getExt().put(OrderExt.CUR_PRICE.getKey(), curPrice.toPlainString());
            if (OwnerOrder.isSell(ownerOrder)) {
                BigDecimal profitRatio = ownerOrder.getPrice().subtract(curPrice)
                    .divide(ownerOrder.getPrice(), 4, RoundingMode.HALF_UP).multiply(new BigDecimal(100));
                profitRatio = NumberUtils.scale(profitRatio);
                ownerOrder.getExt().put(OrderExt.PROFIT_RATIO.getKey(), profitRatio.toPlainString());
            }
        }
    }


    /**
     * 查询用户的期权标的
     *
     * @param owner 用户
     * @return 期权标的列表
     */
    public List<OwnerSecurity> listSecurities(String owner) {
        LambdaQueryWrapper<OwnerSecurity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StringUtils.isNotBlank(owner), OwnerSecurity::getOwner, owner);
        queryWrapper.orderByDesc(OwnerSecurity::getCreateTime);
        List<OwnerSecurity> securities = ownerSecurityDAO.selectList(queryWrapper);
        return securities;
    }

    /**
     * 保存用户的期权标的
     *
     * @param security 期权标的
     * @return 期权标的
     */
    @Transactional(rollbackFor = Exception.class)
    public OwnerSecurity saveSecurity(OwnerSecurity security) {
        if (security.getId() == null) {
            // 新增
            security.setCreateTime(new Date());
            if (security.getStatus() == null) {
                security.setStatus(1);
            }
            ownerSecurityDAO.insert(security);
        } else {
            // 更新
            ownerSecurityDAO.updateById(security);
        }
        return security;
    }

    /**
     * 更新用户的期权标的状态
     *
     * @param id     期权标的ID
     * @param status 状态
     * @return 是否更新成功
     */
    public boolean updateSecurityStatus(Long id, Integer status) {
        LambdaUpdateWrapper<OwnerSecurity> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(OwnerSecurity::getId, id);
        updateWrapper.set(OwnerSecurity::getStatus, status);
        return ownerSecurityDAO.update(null, updateWrapper) > 0;
    }

    /**
     * 查询用户的有效策略
     *
     * @param owner 用户
     * @return 策略列表
     */
    public List<OwnerStrategy> listStrategies(String owner) {
        return ownerStrategyDAO.queryOwnerStrategies(owner);
    }

    /**
     * 查询用户的所有策略
     *
     * @param owner 用户
     * @return 策略列表
     */
    public List<OwnerStrategy> listAllStrategies(String owner) {
        return ownerStrategyDAO.queryAllOwnerStrategies(owner);
    }

    /**
     * 保存用户的策略
     *
     * @param strategy 策略
     * @return 策略
     */
    @Transactional(rollbackFor = Exception.class)
    public OwnerStrategy saveStrategy(OwnerStrategy strategy) {
        if (strategy.getId() == null) {
            // 新增
            if (StringUtils.isBlank(strategy.getStrategyId())) {
                strategy.setStrategyId(UUID.randomUUID().toString().replace("-", ""));
            }
            if (strategy.getStartTime() == null) {
                strategy.setStartTime(new Date());
            }
            if (strategy.getStatus() == null) {
                strategy.setStatus(1);
            }
            if (strategy.getLotSize() == null) {
                strategy.setLotSize(100);
            }
            ownerStrategyDAO.insert(strategy);
        } else {
            // 更新
            ownerStrategyDAO.updateById(strategy);
        }
        // 返回更新后的策略，确保ext字段被正确处理
        return ownerStrategyDAO.queryStrategyById(strategy.getId());
    }

    /**
     * 更新用户的策略状态
     *
     * @param id     策略ID
     * @param status 状态
     * @return 是否更新成功
     */
    public boolean updateStrategyStatus(Long id, Integer status) {
        LambdaUpdateWrapper<OwnerStrategy> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(OwnerStrategy::getId, id);
        updateWrapper.set(OwnerStrategy::getStatus, status);
        return ownerStrategyDAO.update(null, updateWrapper) > 0;
    }

    /**
     * 查询用户的账户
     *
     * @param owner 用户
     * @return 账户列表
     */
    public List<OwnerAccount> listAccounts(String owner) {
        List<OwnerAccount> result = new ArrayList<>();
        OwnerAccount ownerAccount = ownerAccountDAO.queryOwner(owner);
        if (null != ownerAccount) {
            result.add(ownerAccount);
        }
        return result;
    }

    /**
     * 保存用户的账户
     *
     * @param account 账户
     * @return 账户
     */
    @Transactional(rollbackFor = Exception.class)
    public OwnerAccount saveAccount(OwnerAccount account) {
        if (account.getId() == null) {
            // 新增
            account.setCreateTime(new Date());
            // 确保ext字段不为null
            if (account.getExt() == null) {
                account.setExt(new HashMap<>());
            }
            ownerAccountDAO.insert(account);
        } else {
            // 更新
            // 确保ext字段不为null
            if (account.getExt() == null) {
                account.setExt(new HashMap<>());
            }
            ownerAccountDAO.updateById(account);
        }
        return account;
    }

    /**
     * 更新用户的账户状态
     *
     * @param id     账户ID
     * @param status 状态
     * @return 是否更新成功
     */
    public boolean updateAccountStatus(Long id, Integer status) {
        // 由于OwnerAccount没有status字段，我们直接通过id删除账户
        return ownerAccountDAO.deleteById(id) > 0;
    }
}
