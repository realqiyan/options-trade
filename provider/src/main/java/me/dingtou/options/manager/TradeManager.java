package me.dingtou.options.manager;

import me.dingtou.options.constant.OrderStatus;
import me.dingtou.options.dao.OwnerOrderDAO;
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
import java.util.Date;

@Component
public class TradeManager {

    @Autowired
    private OwnerOrderDAO ownerOrderDAO;

    @Autowired
    private OptionsTradeGateway optionsTradeGateway;

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
        OwnerOrder ownerOrder = new OwnerOrder();
        ownerOrder.setStrategyId(ownerStrategy.getStrategyId());
        ownerOrder.setUnderlyingCode(ownerStrategy.getCode());
        ownerOrder.setPlatform(ownerStrategy.getPlatform());
        ownerOrder.setOwner(ownerStrategy.getOwner());
        ownerOrder.setMarket(security.getMarket());
        ownerOrder.setAccountId(ownerStrategy.getAccountId());
        ownerOrder.setTradeTime(new Date());
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

        // 将订单信息插入数据库
        ownerOrderDAO.insert(ownerOrder);

        // 通过交易网关执行交易操作
        String platformOrderId = optionsTradeGateway.trade(ownerOrder);
        ownerOrder.setPlatformOrderId(platformOrderId);
        ownerOrder.setStatus(OrderStatus.SUBMITTED.getCode());
        // 更新数据库中的订单信息
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
        OwnerOrder ownerOrder = new OwnerOrder();
        ownerOrder.setStrategyId(ownerStrategy.getStrategyId());
        ownerOrder.setUnderlyingCode(ownerStrategy.getCode());
        ownerOrder.setPlatform(ownerStrategy.getPlatform());
        ownerOrder.setOwner(ownerStrategy.getOwner());
        ownerOrder.setAccountId(ownerStrategy.getAccountId());
        ownerOrder.setMarket(hisOrder.getMarket());
        ownerOrder.setTradeTime(new Date());
        ownerOrder.setStrikeTime(hisOrder.getStrikeTime());
        ownerOrder.setCode(hisOrder.getCode());
        ownerOrder.setQuantity(quantity);
        ownerOrder.setPrice(price);
        ownerOrder.setSide(side);
        ownerOrder.setStatus(OrderStatus.WAITING_SUBMIT.getCode());

        // 将订单信息插入数据库
        ownerOrderDAO.insert(ownerOrder);

        // 通过交易网关执行交易操作
        String platformOrderId = optionsTradeGateway.trade(ownerOrder);
        ownerOrder.setPlatformOrderId(platformOrderId);
        ownerOrder.setStatus(OrderStatus.SUBMITTED.getCode());
        // 更新数据库中的订单信息
        ownerOrderDAO.updateById(ownerOrder);
        return ownerOrder;
    }

    @Transactional(rollbackFor = Exception.class)
    public OwnerOrder cancel(OwnerOrder dbOrder) {
        optionsTradeGateway.cancel(dbOrder);
        dbOrder.setStatus(OrderStatus.CANCELLED_ALL.getCode());
        int update = ownerOrderDAO.updateById(dbOrder);
        if (update != 1) {
            throw new RuntimeException("cancel order error");
        }
        return dbOrder;
    }


}
