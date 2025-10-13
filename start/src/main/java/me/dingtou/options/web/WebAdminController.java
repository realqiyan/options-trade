package me.dingtou.options.web;

import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.model.OwnerKnowledge;
import me.dingtou.options.model.OwnerSecurity;
import me.dingtou.options.model.OwnerStrategy;
import me.dingtou.options.service.AdminService;
import me.dingtou.options.web.model.WebResult;
import me.dingtou.options.web.util.SessionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理控制器
 *
 * @author qiyan
 */
@Slf4j
@RestController
@RequestMapping("/admin")
public class WebAdminController {

    @Autowired
    private AdminService adminService;

    /**
     * 获取所有用户期权标的
     *
     * @return 用户期权标的列表
     */
    @RequestMapping(value = "/security/list", method = RequestMethod.GET)
    public WebResult<List<OwnerSecurity>> listSecurities() {
        String owner = SessionUtils.getCurrentOwner();
        List<OwnerSecurity> securities = adminService.listSecurities(owner);
        return WebResult.success(securities);
    }

    /**
     * 保存用户期权标的
     *
     * @param security 用户期权标的
     * @return 保存后的用户期权标的
     */
    @RequestMapping(value = "/security/save", method = RequestMethod.POST)
    public WebResult<OwnerSecurity> saveSecurity(@RequestBody OwnerSecurity security) {
        String owner = SessionUtils.getCurrentOwner();
        security.setOwner(owner);
        OwnerSecurity savedSecurity = adminService.saveSecurity(security);
        return WebResult.success(savedSecurity);
    }

    /**
     * 更新用户期权标的状态
     *
     * @param id     标的ID
     * @param status 状态
     * @return 是否更新成功
     */
    @RequestMapping(value = "/security/status", method = RequestMethod.POST)
    public WebResult<Boolean> updateSecurityStatus(@RequestParam("id") Long id,
            @RequestParam("status") Integer status) {
        boolean result = adminService.updateSecurityStatus(id, status);
        return WebResult.success(result);
    }

    /**
     * 获取所有用户期权策略
     *
     * @return 用户期权策略列表
     */
    @RequestMapping(value = "/strategy/list", method = RequestMethod.GET)
    public WebResult<List<OwnerStrategy>> listStrategies() {
        String owner = SessionUtils.getCurrentOwner();
        List<OwnerStrategy> strategies = adminService.listStrategies(owner);
        return WebResult.success(strategies);
    }

    /**
     * 保存用户期权策略
     *
     * @param strategy 用户期权策略
     * @return 保存后的用户期权策略
     */
    @RequestMapping(value = "/strategy/save", method = RequestMethod.POST)
    public WebResult<OwnerStrategy> saveStrategy(@RequestBody OwnerStrategy strategy) {
        String owner = SessionUtils.getCurrentOwner();
        strategy.setOwner(owner);
        OwnerStrategy savedStrategy = adminService.saveStrategy(strategy);
        return WebResult.success(savedStrategy);
    }

    /**
     * 更新用户期权策略状态
     *
     * @param id     策略ID
     * @param status 状态
     * @return 是否更新成功
     */
    @RequestMapping(value = "/strategy/status", method = RequestMethod.POST)
    public WebResult<Boolean> updateStrategyStatus(@RequestParam("id") Long id,
            @RequestParam("status") Integer status) {
        boolean result = adminService.updateStrategyStatus(id, status);
        return WebResult.success(result);
    }

    /**
     * 获取所有用户账户
     *
     * @return 用户账户列表
     */
    @RequestMapping(value = "/account/list", method = RequestMethod.GET)
    public WebResult<List<OwnerAccount>> listAccounts() {
        String owner = SessionUtils.getCurrentOwner();
        List<OwnerAccount> accounts = adminService.listAccounts(owner);
        return WebResult.success(accounts);
    }

    /**
     * 保存用户账户
     *
     * @param account 用户账户
     * @return 保存后的用户账户
     */
    @RequestMapping(value = "/account/save", method = RequestMethod.POST)
    public WebResult<OwnerAccount> saveAccount(@RequestBody OwnerAccount account) {
        String owner = SessionUtils.getCurrentOwner();
        account.setOwner(owner);
        OwnerAccount savedAccount = adminService.saveAccount(account);
        return WebResult.success(savedAccount);
    }

    /**
     * 删除用户账户
     *
     * @param id 账户ID
     * @return 是否删除成功
     */
    @RequestMapping(value = "/account/delete", method = RequestMethod.POST)
    public WebResult<Boolean> deleteAccount(@RequestParam("id") Long id) {
        boolean result = adminService.updateAccountStatus(id, 0);
        return WebResult.success(result);
    }

    /**
     * 获取所有知识库
     *
     * @return 知识库列表
     */
    @RequestMapping(value = "/knowledge/list", method = RequestMethod.GET)
    public WebResult<List<OwnerKnowledge>> listKnowledges() {
        String owner = SessionUtils.getCurrentOwner();
        List<OwnerKnowledge> knowledges = adminService.listKnowledges(owner);
        return WebResult.success(knowledges);
    }

    /**
     * 保存知识库
     *
     * @param knowledge 知识库
     * @return 保存后的知识库
     */
    @RequestMapping(value = "/knowledge/save", method = RequestMethod.POST)
    public WebResult<OwnerKnowledge> saveKnowledge(@RequestBody OwnerKnowledge knowledge) {
        String owner = SessionUtils.getCurrentOwner();
        knowledge.setOwner(owner);
        try {
            OwnerKnowledge savedKnowledge = adminService.saveKnowledge(knowledge);
            return WebResult.success(savedKnowledge);
        } catch (Exception e) {
            return WebResult.failure(e.getMessage());
        }
    }

    /**
     * 更新知识库状态
     *
     * @param id     知识库ID
     * @param status 状态
     * @return 是否更新成功
     */
    @RequestMapping(value = "/knowledge/status", method = RequestMethod.POST)
    public WebResult<Boolean> updateKnowledgeStatus(@RequestParam("id") Long id,
            @RequestParam("status") Integer status) {
        try {
            boolean result = adminService.updateKnowledgeStatus(id, status);
            return WebResult.success(result);
        } catch (Exception e) {
            return WebResult.failure(e.getMessage());
        }
    }

    /**
     * 物理删除知识库
     *
     * @param id 知识库ID
     * @return 是否删除成功
     */
    @RequestMapping(value = "/knowledge/delete", method = RequestMethod.POST)
    public WebResult<Boolean> deleteKnowledge(@RequestParam("id") Long id) {
        try {
            boolean result = adminService.deleteKnowledge(id);
            return WebResult.success(result);
        } catch (Exception e) {
            return WebResult.failure(e.getMessage());
        }
    }

    /**
     * 根据类型查询知识库
     *
     * @param type 类型
     * @return 知识库列表
     */
    @RequestMapping(value = "/knowledge/listByType", method = RequestMethod.GET)
    public WebResult<List<OwnerKnowledge>> listKnowledgesByType(@RequestParam("type") Integer type) {
        String owner = SessionUtils.getCurrentOwner();
        List<OwnerKnowledge> knowledges = adminService.listKnowledgesByType(owner, type);
        return WebResult.success(knowledges);
    }
}