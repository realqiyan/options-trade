package me.dingtou.options.manager;

import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.dao.OwnerFlowSummaryDAO;
import me.dingtou.options.gateway.OptionsTradeGateway;
import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.model.OwnerFlowSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 资金流水管理器
 *
 * @author qiyan
 */
@Component
@Slf4j
public class OwnerFlowSummaryManager {

    @Autowired
    private OptionsTradeGateway optionsTradeGateway;

    @Autowired
    private OwnerFlowSummaryDAO ownerFlowSummaryDAO;

    /**
     * 同步指定日期的资金流水
     *
     * @param account      账号
     * @param platform     平台
     * @param clearingDate 清算日期(yyyy-MM-dd)
     * @return 同步的流水数量
     */
    public int syncFlowSummary(OwnerAccount account, String clearingDate) {

        try {
            // 从外部系统获取资金流水
            List<OwnerFlowSummary> flowSummaries = optionsTradeGateway.getFlowSummary(account, clearingDate);
            if (flowSummaries == null || flowSummaries.isEmpty()) {
                log.info("没有获取到资金流水数据，owner: {}, platform: {}, clearingDate: {}",
                        account.getOwner(),
                        account.getPlatform(),
                        clearingDate);
                return 0;
            }

            int count = 0;
            for (OwnerFlowSummary flowSummary : flowSummaries) {
                // 检查是否已存在
                OwnerFlowSummary existing = ownerFlowSummaryDAO.queryByCashflowId(account.getOwner(),
                        flowSummary.getCashflowId());
                if (existing == null) {
                    // 新增
                    ownerFlowSummaryDAO.insert(flowSummary);
                    count++;
                } else {
                    // 更新
                    flowSummary.setId(existing.getId());
                    ownerFlowSummaryDAO.updateById(flowSummary);
                    count++;
                }
            }

            log.info("同步资金流水完成，owner: {}, platform: {}, clearingDate: {}, 数量: {}",
                    account.getOwner(),
                    account.getPlatform(),
                    clearingDate,
                    count);
            return count;
        } catch (Exception e) {
            log.error("同步资金流水失败，owner: {}, platform: {}, clearingDate: {}",
                    account.getOwner(),
                    account.getPlatform(),
                    clearingDate,
                    e);
            return 0;
        }
    }

    /**
     * 查询资金流水列表
     *
     * @param owner 所有者
     * @return 资金流水列表
     */
    public List<OwnerFlowSummary> listFlowSummary(String owner) {
        return ownerFlowSummaryDAO.queryOwnerFlowSummary(owner);
    }

    /**
     * 根据平台查询资金流水列表
     *
     * @param owner    所有者
     * @param platform 平台
     * @return 资金流水列表
     */
    public List<OwnerFlowSummary> listFlowSummaryByPlatform(String owner, String platform) {
        return ownerFlowSummaryDAO.queryOwnerFlowSummaryByPlatform(owner, platform);
    }

    /**
     * 根据清算日期查询资金流水
     *
     * @param owner        所有者
     * @param clearingDate 清算日期
     * @return 资金流水列表
     */
    public List<OwnerFlowSummary> getFlowSummary(String owner, String clearingDate) {
        return ownerFlowSummaryDAO.getFlowSummary(owner, clearingDate);
    }

    /**
     * 根据ID查询资金流水详情
     *
     * @param id 流水ID
     * @return 资金流水详情
     */
    public OwnerFlowSummary getFlowSummaryById(Long id) {
        return ownerFlowSummaryDAO.selectById(id);
    }

    /**
     * 根据cashflowId查询资金流水
     *
     * @param owner      所有者
     * @param cashflowId 流水ID
     * @return 资金流水
     */
    public OwnerFlowSummary getFlowSummaryByCashflowId(String owner, Long cashflowId) {
        return ownerFlowSummaryDAO.queryByCashflowId(owner, cashflowId);
    }
}