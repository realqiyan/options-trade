package me.dingtou.options.service;

import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.manager.OwnerConfigManager;
import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.model.OwnerSecurity;
import me.dingtou.options.model.OwnerStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * 管理服务实现类
 *
 * @author qiyan
 */
@Slf4j
@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private OwnerConfigManager ownerConfigManager;

    @Override
    public List<OwnerSecurity> listSecurities(String owner) {
        return ownerConfigManager.listSecurities(owner);
    }

    @Override
    public OwnerSecurity saveSecurity(OwnerSecurity security) {
        return ownerConfigManager.saveSecurity(security);
    }

    @Override
    public boolean updateSecurityStatus(Long id, Integer status) {
        return ownerConfigManager.updateSecurityStatus(id, status);
    }

    @Override
    public List<OwnerStrategy> listStrategies(String owner) {
        return ownerConfigManager.listAllStrategies(owner);
    }

    @Override
    public OwnerStrategy saveStrategy(OwnerStrategy strategy) {
        return ownerConfigManager.saveStrategy(strategy);
    }

    @Override
    public boolean updateStrategyStatus(Long id, Integer status) {
        return ownerConfigManager.updateStrategyStatus(id, status);
    }

    @Override
    public List<OwnerAccount> listAccounts(String owner) {
        return ownerConfigManager.listAccounts(owner);
    }

    @Override
    public OwnerAccount saveAccount(OwnerAccount account) {
        return ownerConfigManager.saveAccount(account);
    }

    @Override
    public boolean updateAccountStatus(Long id, Integer status) {
        return ownerConfigManager.updateAccountStatus(id, status);
    }
}