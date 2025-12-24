package me.dingtou.options.service;

import me.dingtou.options.model.OwnerFlowSummary;

import java.util.List;

/**
 * 资金流水服务接口
 *
 * @author qiyan
 */
public interface FlowSummaryService {

    /**
     * 同步指定日期的资金流水
     *
     * @param owner       所有者
     * @param platform    平台
     * @param clearingDate 清算月份(yyyy-MM)
     * @return 同步的流水数量
     */
    int syncFlowSummary(String owner, String clearingMonth);

    /**
     * 查询资金流水列表
     *
     * @param owner     所有者
     * @param startDate 开始日期(yyyy-MM-dd)
     * @param endDate   结束日期(yyyy-MM-dd)
     * @return 资金流水列表
     */
    List<OwnerFlowSummary> listFlowSummary(String owner, String startDate, String endDate);

    /**
     * 根据平台查询资金流水列表
     *
     * @param owner    所有者
     * @param platform 平台
     * @return 资金流水列表
     */
    List<OwnerFlowSummary> listFlowSummaryByPlatform(String owner, String platform);

    /**
     * 根据清算日期查询资金流水
     *
     * @param owner       所有者
     * @param clearingDate 清算日期
     * @return 资金流水列表
     */
    List<OwnerFlowSummary> getFlowSummary(String owner, String clearingDate);

    /**
     * 根据ID查询资金流水详情
     *
     * @param id 流水ID
     * @return 资金流水详情
     */
    OwnerFlowSummary getFlowSummaryById(Long id);

    /**
     * 根据cashflowId查询资金流水
     *
     * @param owner     所有者
     * @param cashflowId 流水ID
     * @return 资金流水
     */
    OwnerFlowSummary getFlowSummaryByCashflowId(String owner, Long cashflowId);
}