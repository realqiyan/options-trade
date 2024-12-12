package me.dingtou.options.manager;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import me.dingtou.options.dao.OwnerAccountDAO;
import me.dingtou.options.dao.OwnerOrderDAO;
import me.dingtou.options.dao.OwnerSecurityDAO;
import me.dingtou.options.dataobject.OwnerAccount;
import me.dingtou.options.dataobject.OwnerOrder;
import me.dingtou.options.dataobject.OwnerSecurity;
import me.dingtou.options.model.Account;
import me.dingtou.options.model.Order;
import me.dingtou.options.model.Owner;
import me.dingtou.options.model.Security;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class OwnerManager {

    @Autowired
    private OwnerSecurityDAO ownerSecurityDAO;

    @Autowired
    private OwnerOrderDAO ownerOrderDAO;

    @Autowired
    private OwnerAccountDAO ownerAccountDAO;


    public Owner queryOwner(String owner) {
        Owner ownerObj = new Owner();
        ownerObj.setOwner(owner);
        QueryWrapper<OwnerSecurity> querySecurity = new QueryWrapper<>();
        querySecurity.eq("owner", owner);
        List<OwnerSecurity> ownerSecurityList = ownerSecurityDAO.selectList(querySecurity);
        if (null != ownerSecurityList && !ownerSecurityList.isEmpty()) {
            List<Security> securityList = new ArrayList<>();
            ownerObj.setSecurityList(securityList);
            for (OwnerSecurity ownerSecurity : ownerSecurityList) {
                securityList.add(Security.of(ownerSecurity.getCode(), ownerSecurity.getMarket()));
            }
        }
        QueryWrapper<OwnerOrder> queryOrder = new QueryWrapper<OwnerOrder>();
        queryOrder.eq("owner", owner);
        List<OwnerOrder> ownerOrderList = ownerOrderDAO.selectList(queryOrder);
        if (null != ownerOrderList && !ownerOrderList.isEmpty()) {
            List<Order> orderList = new ArrayList<>();
            ownerObj.setOrderList(orderList);
            for (OwnerOrder ownerOrder : ownerOrderList) {
                Order order = new Order();
                order.setSecurity(Security.of(ownerOrder.getCode(), ownerOrder.getMarket()));
                order.setSide(ownerOrder.getSide());
                order.setPrice(ownerOrder.getPrice());
                order.setQuantity(ownerOrder.getQuantity());
                orderList.add(order);
            }
        }

        QueryWrapper<OwnerAccount> queryAccount = new QueryWrapper<OwnerAccount>();
        queryOrder.eq("owner", owner);
        List<OwnerAccount> ownerAccountList = ownerAccountDAO.selectList(queryAccount);
        if (null != ownerAccountList && !ownerAccountList.isEmpty()) {
            List<Account> accountList = new ArrayList<>();
            ownerObj.setAccountList(accountList);
            for (OwnerAccount ownerAccount : ownerAccountList) {
                Account account = new Account();
                account.setOwner(ownerAccount.getOwner());
                account.setAccountId(ownerAccount.getAccountId());
                account.setPlatform(ownerAccount.getPlatform());
                account.setExt(ownerAccount.getExt());
                accountList.add(account);
            }
        }

        return ownerObj;
    }
}
