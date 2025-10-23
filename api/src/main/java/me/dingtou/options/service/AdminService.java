package me.dingtou.options.service;

import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.model.OwnerKnowledge;
import me.dingtou.options.model.OwnerSecurity;
import me.dingtou.options.model.OwnerStrategy;
import me.dingtou.options.model.PageResult;

import java.util.List;

/**
 * 管理服务接口
 *
 * @author qiyan
 */
public interface AdminService {

    /**
     * 获取所有用户期权标的
     *
     * @param owner 所有者
     * @param page  页码
     * @param size  每页大小
     * @return 用户期权标的分页结果
     */
    PageResult<OwnerSecurity> listSecurities(String owner, Integer page, Integer size);

    /**
     * 保存用户期权标的
     *
     * @param security 用户期权标的
     * @return 保存后的用户期权标的
     */
    OwnerSecurity saveSecurity(OwnerSecurity security);

    /**
     * 更新用户期权标的状态
     *
     * @param id     标的ID
     * @param status 状态
     * @return 是否更新成功
     */
    boolean updateSecurityStatus(Long id, Integer status);

    /**
     * 获取所有用户期权策略
     *
     * @param owner 所有者
     * @param page  页码
     * @param size  每页大小
     * @return 用户期权策略分页结果
     */
    PageResult<OwnerStrategy> listStrategies(String owner, Integer page, Integer size);

    /**
     * 保存用户期权策略
     *
     * @param strategy 用户期权策略
     * @return 保存后的用户期权策略
     */
    OwnerStrategy saveStrategy(OwnerStrategy strategy);

    /**
     * 更新用户期权策略状态
     *
     * @param id     策略ID
     * @param status 状态
     * @return 是否更新成功
     */
    boolean updateStrategyStatus(Long id, Integer status);
    
    /**
     * 获取所有用户账户
     *
     * @param owner 所有者
     * @param page  页码
     * @param size  每页大小
     * @return 用户账户分页结果
     */
    PageResult<OwnerAccount> listAccounts(String owner, Integer page, Integer size);
    
    /**
     * 保存用户账户
     *
     * @param account 用户账户
     * @return 保存后的用户账户
     */
    OwnerAccount saveAccount(OwnerAccount account);
    
    /**
     * 更新用户账户状态
     *
     * @param id     账户ID
     * @param status 状态
     * @return 是否更新成功
     */
    boolean updateAccountStatus(Long id, Integer status);
    
    /**
     * 获取所有知识库
     *
     * @param owner 所有者
     * @param page  页码
     * @param size  每页大小
     * @return 知识库分页结果
     */
    PageResult<OwnerKnowledge> listKnowledges(String owner, Integer page, Integer size);
    
    /**
     * 保存知识库
     *
     * @param knowledge 知识库
     * @return 保存后的知识库
     */
    OwnerKnowledge saveKnowledge(OwnerKnowledge knowledge);
    
    /**
     * 更新知识库状态
     *
     * @param id     知识库ID
     * @param status 状态
     * @return 是否更新成功
     */
    boolean updateKnowledgeStatus(Long id, Integer status);
    
    /**
     * 物理删除知识库
     *
     * @param id 知识库ID
     * @return 是否删除成功
     */
    boolean deleteKnowledge(Long id);
    
    /**
     * 根据类型查询知识库
     *
     * @param owner 所有者
     * @param type  类型
     * @param page  页码
     * @param size  每页大小
     * @return 知识库分页结果
     */
    PageResult<OwnerKnowledge> listKnowledgesByType(String owner, Integer type, Integer page, Integer size);
}