package me.dingtou.options.service;

import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.manager.KnowledgeManager;
import me.dingtou.options.manager.OwnerManager;
import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.model.OwnerKnowledge;
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
    private OwnerManager ownerManager;

    @Autowired
    private KnowledgeManager knowledgeManager;

    @Override
    public List<OwnerSecurity> listSecurities(String owner) {
        return ownerManager.listSecurities(owner);
    }

    @Override
    public OwnerSecurity saveSecurity(OwnerSecurity security) {
        return ownerManager.saveSecurity(security);
    }

    @Override
    public boolean updateSecurityStatus(Long id, Integer status) {
        return ownerManager.updateSecurityStatus(id, status);
    }

    @Override
    public List<OwnerStrategy> listStrategies(String owner) {
        return ownerManager.listAllStrategies(owner);
    }

    @Override
    public OwnerStrategy saveStrategy(OwnerStrategy strategy) {
        return ownerManager.saveStrategy(strategy);
    }

    @Override
    public boolean updateStrategyStatus(Long id, Integer status) {
        return ownerManager.updateStrategyStatus(id, status);
    }

    @Override
    public List<OwnerAccount> listAccounts(String owner) {
        return ownerManager.listAccounts(owner);
    }

    @Override
    public OwnerAccount saveAccount(OwnerAccount account) {
        return ownerManager.saveAccount(account);
    }

    @Override
    public boolean updateAccountStatus(Long id, Integer status) {
        return ownerManager.updateAccountStatus(id, status);
    }
    
    @Override
    public List<OwnerKnowledge> listKnowledges(String owner) {
        return knowledgeManager.listKnowledges(owner);
    }
    
    @Override
    public OwnerKnowledge saveKnowledge(OwnerKnowledge knowledge) {
        return knowledgeManager.saveKnowledge(knowledge);
    }
    
    @Override
    public boolean updateKnowledgeStatus(Long id, Integer status) {
        return knowledgeManager.updateKnowledgeStatus(id, status);
    }
}
