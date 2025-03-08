package me.dingtou.options.manager;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.constant.Status;
import me.dingtou.options.dao.OwnerAccountDAO;
import me.dingtou.options.dao.OwnerSecurityDAO;
import me.dingtou.options.gateway.SecurityQuoteGateway;
import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.model.OwnerSecurity;
import me.dingtou.options.model.Security;
import me.dingtou.options.model.SecurityQuote;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;


@Slf4j
@Component
public class PushDataManager {
    @Autowired
    private OwnerSecurityDAO ownerSecurityDAO;

    @Autowired
    private OwnerAccountDAO ownerAccountDAO;

    @Autowired
    private SecurityQuoteGateway securityQuoteGateway;

    /**
     * 订阅股票价格
     *
     * @param callback 回调
     */
    public void subscribeSecurityPrice(String owner, Function<SecurityQuote, Void> callback) {
        OwnerAccount ownerAccount = ownerAccountDAO.queryOwner(owner);
        QueryWrapper<OwnerSecurity> query = new QueryWrapper<>();
        query.eq("owner", owner).eq("status", Status.VALID.getCode());
        List<OwnerSecurity> ownerSecurities = ownerSecurityDAO.selectList(query);
        List<Security> securities = new ArrayList<>();
        for (OwnerSecurity security : ownerSecurities) {
            securities.add(Security.of(security.getCode(), security.getMarket()));
        }
        securityQuoteGateway.subscribeQuote(ownerAccount, securities, callback);
    }
}
