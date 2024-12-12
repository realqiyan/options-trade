package me.dingtou.options.manager;

import me.dingtou.options.constant.Status;
import me.dingtou.options.dao.OwnerOrderDAO;
import me.dingtou.options.dataobject.OwnerOrder;
import me.dingtou.options.gateway.OptionsTradeGateway;
import me.dingtou.options.model.Account;
import me.dingtou.options.model.Options;
import me.dingtou.options.model.Order;
import me.dingtou.options.model.Security;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
     * @param account  交易账户信息
     * @param side     交易方向，买或卖
     * @param quantity 交易数量
     * @param price    交易价格
     * @param options  交易选项配置
     * @return 返回执行后的订单对象
     */
    @Transactional(rollbackFor = Exception.class)
    public Order trade(Account account, int side, Long quantity, BigDecimal price, Options options) {
        // 获取基础配置中的证券信息
        Security security = options.getBasic().getSecurity();

        // 创建并初始化订单对象
        OwnerOrder ownerOrder = new OwnerOrder();
        ownerOrder.setOwner(account.getOwner());
        ownerOrder.setAccountId(account.getAccountId());
        ownerOrder.setMarket(security.getMarket());
        ownerOrder.setCode(security.getCode());
        ownerOrder.setQuantity(quantity);
        ownerOrder.setPrice(price);
        ownerOrder.setSide(side);
        ownerOrder.setStatus(Status.PROCESSING.getCode());
        ownerOrder.setTradeTime(new Date());

        // 将订单信息插入数据库
        ownerOrderDAO.insert(ownerOrder);

        // 创建并初始化Order对象
        Order order = new Order();
        order.setAccount(account);
        order.setSecurity(security);
        order.setQuantity(quantity);
        order.setPrice(price);
        order.setSide(side);

        // 通过交易网关执行交易操作
        order = optionsTradeGateway.trade(order);

        // 更新订单的交易ID和交易市场信息
        ownerOrder.setOrderId(order.getOrderId());
        ownerOrder.setTradeMarket(order.getTradeMarket());

        // 更新数据库中的订单信息
        ownerOrderDAO.updateById(ownerOrder);

        // 返回执行后的订单对象
        return order;
    }
}
