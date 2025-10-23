package me.dingtou.options.service;

import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.manager.KnowledgeManager;
import me.dingtou.options.manager.OwnerManager;
import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.model.OwnerKnowledge;
import me.dingtou.options.model.OwnerSecurity;
import me.dingtou.options.model.OwnerStrategy;
import me.dingtou.options.model.PageResult;
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
    public PageResult<OwnerSecurity> listSecurities(String owner, Integer page, Integer size) {
        // 设置默认值
        if (page == null || page < 1) {
            page = 1;
        }
        if (size == null || size < 1) {
            size = 50;
        }
        
        // 获取所有数据
        List<OwnerSecurity> allSecurities = ownerManager.listSecurities(owner);
        
        // 计算总数
        Long total = (long) allSecurities.size();
        
        // 计算分页
        int startIndex = (page - 1) * size;
        int endIndex = Math.min(startIndex + size, allSecurities.size());
        
        // 获取当前页数据
        List<OwnerSecurity> pageData = allSecurities.subList(startIndex, endIndex);
        
        return PageResult.of(pageData, total, page, size);
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
    public PageResult<OwnerStrategy> listStrategies(String owner, Integer page, Integer size) {
        // 设置默认值
        if (page == null || page < 1) {
            page = 1;
        }
        if (size == null || size < 1) {
            size = 50;
        }
        
        // 获取所有数据
        List<OwnerStrategy> allStrategies = ownerManager.listAllStrategies(owner);
        
        // 计算总数
        Long total = (long) allStrategies.size();
        
        // 计算分页
        int startIndex = (page - 1) * size;
        int endIndex = Math.min(startIndex + size, allStrategies.size());
        
        // 获取当前页数据
        List<OwnerStrategy> pageData = allStrategies.subList(startIndex, endIndex);
        
        return PageResult.of(pageData, total, page, size);
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
    public PageResult<OwnerAccount> listAccounts(String owner, Integer page, Integer size) {
        // 设置默认值
        if (page == null || page < 1) {
            page = 1;
        }
        if (size == null || size < 1) {
            size = 50;
        }
        
        // 获取所有数据
        List<OwnerAccount> allAccounts = ownerManager.listAccounts(owner);
        
        // 计算总数
        Long total = (long) allAccounts.size();
        
        // 计算分页
        int startIndex = (page - 1) * size;
        int endIndex = Math.min(startIndex + size, allAccounts.size());
        
        // 获取当前页数据
        List<OwnerAccount> pageData = allAccounts.subList(startIndex, endIndex);
        
        return PageResult.of(pageData, total, page, size);
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
    public PageResult<OwnerKnowledge> listKnowledges(String owner, Integer page, Integer size) {
        // 设置默认值
        if (page == null || page < 1) {
            page = 1;
        }
        if (size == null || size < 1) {
            size = 50;
        }
        
        // 获取所有数据
        List<OwnerKnowledge> allKnowledges = knowledgeManager.listKnowledges(owner);
        
        // 计算总数
        Long total = (long) allKnowledges.size();
        
        // 计算分页
        int startIndex = (page - 1) * size;
        int endIndex = Math.min(startIndex + size, allKnowledges.size());
        
        // 获取当前页数据
        List<OwnerKnowledge> pageData = allKnowledges.subList(startIndex, endIndex);
        
        return PageResult.of(pageData, total, page, size);
    }
    
    @Override
    public OwnerKnowledge saveKnowledge(OwnerKnowledge knowledge) {
        return knowledgeManager.saveKnowledge(knowledge);
    }
    
    @Override
    public boolean updateKnowledgeStatus(Long id, Integer status) {
        return knowledgeManager.updateKnowledgeStatus(id, status);
    }
    
    @Override
    public boolean deleteKnowledge(Long id) {
        return knowledgeManager.deleteKnowledge(id);
    }
    
    @Override
    public PageResult<OwnerKnowledge> listKnowledgesByType(String owner, Integer type, Integer page, Integer size) {
        // 设置默认值
        if (page == null || page < 1) {
            page = 1;
        }
        if (size == null || size < 1) {
            size = 50;
        }
        
        // 获取所有数据
        List<OwnerKnowledge> allKnowledges = knowledgeManager.listKnowledgesByType(owner, type);
        
        // 计算总数
        Long total = (long) allKnowledges.size();
        
        // 计算分页
        int startIndex = (page - 1) * size;
        int endIndex = Math.min(startIndex + size, allKnowledges.size());
        
        // 获取当前页数据
        List<OwnerKnowledge> pageData = allKnowledges.subList(startIndex, endIndex);
        
        return PageResult.of(pageData, total, page, size);
    }
}
