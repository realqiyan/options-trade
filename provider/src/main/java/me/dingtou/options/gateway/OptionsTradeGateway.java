package me.dingtou.options.gateway;

import me.dingtou.options.model.Owner;
import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.model.OwnerOrder;
import me.dingtou.options.model.OwnerPosition;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 期权交易
 *
 * @author qiyan
 */
public interface OptionsTradeGateway {

    /**
     * 交易
     *
     * @param account 账号
     * @param order   订单
     * @return 外部订单号
     */
    OwnerOrder trade(OwnerAccount account, OwnerOrder order);

    /**
     * 取消订单
     *
     * @param account 账号
     * @param order   订单
     * @return 订单
     */
    OwnerOrder cancel(OwnerAccount account, OwnerOrder order);


    /**
     * 删除订单
     *
     * @param account 账号
     * @param order   订单
     */
    Boolean delete(OwnerAccount account, OwnerOrder order);

    /**
     * 计算订单总费用
     *
     * @param account 用户账号
     * @param orders  订单
     * @return 订单总费用
     */
    Map<String, BigDecimal> totalFee(OwnerAccount account, List<OwnerOrder> orders);

    /**
     * 同步订单
     *
     * @param owner 用户账号
     * @return 订单列表
     */
    List<OwnerOrder> pullOrder(Owner owner);

    /**
     * 拉取成交单
     *
     * @param owner 用户账号
     * @return 订单列表
     */
    List<OwnerOrder> pullOrderFill(Owner owner);

    /**
     * 获取持仓
     * @param owner 用户账号
     * @return 持仓列表
     */
    List<OwnerPosition> getPosition(OwnerAccount account);

    /**
     * 订阅订单推送
     *
     * @param allOwner 用户列表
     * @param callback 回调
     */
    void subscribeOrderPush(List<Owner> allOwner, Function<OwnerOrder, Void> callback);




}
