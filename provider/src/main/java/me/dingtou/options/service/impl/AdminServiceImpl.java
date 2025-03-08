package me.dingtou.options.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.dao.OwnerSecurityDAO;
import me.dingtou.options.dao.OwnerStrategyDAO;
import me.dingtou.options.model.OwnerSecurity;
import me.dingtou.options.model.OwnerStrategy;
import me.dingtou.options.service.AdminService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.UUID;

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

    @Override
    public List<OwnerSecurity> listSecurities(String owner) {
        LambdaQueryWrapper<OwnerSecurity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StringUtils.isNotBlank(owner), OwnerSecurity::getOwner, owner);
        queryWrapper.orderByDesc(OwnerSecurity::getCreateTime);
        return ownerSecurityDAO.selectList(queryWrapper);
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
        LambdaQueryWrapper<OwnerStrategy> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StringUtils.isNotBlank(owner), OwnerStrategy::getOwner, owner);
        queryWrapper.orderByDesc(OwnerStrategy::getStartTime);
        return ownerStrategyDAO.selectList(queryWrapper);
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
        return strategy;
    }

    @Override
    public boolean updateStrategyStatus(Long id, Integer status) {
        LambdaUpdateWrapper<OwnerStrategy> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(OwnerStrategy::getId, id);
        updateWrapper.set(OwnerStrategy::getStatus, status);
        return ownerStrategyDAO.update(null, updateWrapper) > 0;
    }
} 