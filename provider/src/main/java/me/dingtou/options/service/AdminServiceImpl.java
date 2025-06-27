package me.dingtou.options.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.dao.OwnerAccountDAO;
import me.dingtou.options.dao.OwnerSecurityDAO;
import me.dingtou.options.dao.OwnerStrategyDAO;
import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.model.OwnerSecurity;
import me.dingtou.options.model.OwnerStrategy;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.HashMap;

/**
 * 管理服务实现类
 *
 * @author qiyan
 */
@Slf4j
@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private OwnerSecurityDAO ownerSecurityDAO;

    @Autowired
    private OwnerStrategyDAO ownerStrategyDAO;

    @Autowired
    private OwnerAccountDAO ownerAccountDAO;

    @Override
    public List<OwnerSecurity> listSecurities(String owner) {
        LambdaQueryWrapper<OwnerSecurity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StringUtils.isNotBlank(owner), OwnerSecurity::getOwner, owner);
        queryWrapper.orderByDesc(OwnerSecurity::getCreateTime);
        List<OwnerSecurity> securities = ownerSecurityDAO.selectList(queryWrapper);
        return securities;
    }

    @Override
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

    @Override
    public boolean updateSecurityStatus(Long id, Integer status) {
        LambdaUpdateWrapper<OwnerSecurity> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(OwnerSecurity::getId, id);
        updateWrapper.set(OwnerSecurity::getStatus, status);
        return ownerSecurityDAO.update(null, updateWrapper) > 0;
    }

    @Override
    public List<OwnerStrategy> listStrategies(String owner) {
        return ownerStrategyDAO.queryOwnerStrategies(owner);
    }

    @Override
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

    @Override
    public boolean updateStrategyStatus(Long id, Integer status) {
        LambdaUpdateWrapper<OwnerStrategy> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(OwnerStrategy::getId, id);
        updateWrapper.set(OwnerStrategy::getStatus, status);
        return ownerStrategyDAO.update(null, updateWrapper) > 0;
    }

    @Override
    public List<OwnerAccount> listAccounts(String owner) {
        List<OwnerAccount> result = new ArrayList<>();
        OwnerAccount ownerAccount = ownerAccountDAO.queryOwner(owner);
        if (null != ownerAccount) {
            result.add(ownerAccount);
        }
        return result;
    }

    @Override
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

    @Override
    public boolean updateAccountStatus(Long id, Integer status) {
        // 由于OwnerAccount没有status字段，我们直接通过id删除账户
        return ownerAccountDAO.deleteById(id) > 0;
    }
}