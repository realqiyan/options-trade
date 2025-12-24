package me.dingtou.options.dao;

import java.util.List;

import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.dingtou.options.model.OwnerFlowSummary;

public interface OwnerFlowSummaryDAO extends BaseMapper<OwnerFlowSummary> {

        @Select("SELECT * FROM owner_flow_summary WHERE owner = #{owner}")
        @Results({
                        @Result(property = "cashflowId", column = "cashflow_id"),
                        @Result(property = "clearingDate", column = "clearing_date"),
                        @Result(property = "settlementDate", column = "settlement_date"),
                        @Result(property = "currency", column = "currency"),
                        @Result(property = "cashflowType", column = "cashflow_type"),
                        @Result(property = "cashflowDirection", column = "cashflow_direction"),
                        @Result(property = "cashflowAmount", column = "cashflow_amount"),
                        @Result(property = "cashflowRemark", column = "cashflow_remark"),
                        @Result(property = "platform", column = "platform")
        })
        List<OwnerFlowSummary> queryOwnerFlowSummary(String owner);

        @Select("SELECT * FROM owner_flow_summary WHERE owner = #{owner} AND platform = #{platform}")
        @Results({
                        @Result(property = "cashflowId", column = "cashflow_id"),
                        @Result(property = "clearingDate", column = "clearing_date"),
                        @Result(property = "settlementDate", column = "settlement_date"),
                        @Result(property = "currency", column = "currency"),
                        @Result(property = "cashflowType", column = "cashflow_type"),
                        @Result(property = "cashflowDirection", column = "cashflow_direction"),
                        @Result(property = "cashflowAmount", column = "cashflow_amount"),
                        @Result(property = "cashflowRemark", column = "cashflow_remark"),
                        @Result(property = "platform", column = "platform")
        })
        List<OwnerFlowSummary> queryOwnerFlowSummaryByPlatform(String owner, String platform);

        @Select("SELECT * FROM owner_flow_summary WHERE owner = #{owner} AND cashflow_id = #{cashflowId}")
        @Results({
                        @Result(property = "cashflowId", column = "cashflow_id"),
                        @Result(property = "clearingDate", column = "clearing_date"),
                        @Result(property = "settlementDate", column = "settlement_date"),
                        @Result(property = "currency", column = "currency"),
                        @Result(property = "cashflowType", column = "cashflow_type"),
                        @Result(property = "cashflowDirection", column = "cashflow_direction"),
                        @Result(property = "cashflowAmount", column = "cashflow_amount"),
                        @Result(property = "cashflowRemark", column = "cashflow_remark"),
                        @Result(property = "platform", column = "platform")
        })
        OwnerFlowSummary queryByCashflowId(String owner, Long cashflowId);

        @Select("SELECT * FROM owner_flow_summary WHERE owner = #{owner} AND clearing_date = #{clearingDate}")
        @Results({
                        @Result(property = "cashflowId", column = "cashflow_id"),
                        @Result(property = "clearingDate", column = "clearing_date"),
                        @Result(property = "settlementDate", column = "settlement_date"),
                        @Result(property = "currency", column = "currency"),
                        @Result(property = "cashflowType", column = "cashflow_type"),
                        @Result(property = "cashflowDirection", column = "cashflow_direction"),
                        @Result(property = "cashflowAmount", column = "cashflow_amount"),
                        @Result(property = "cashflowRemark", column = "cashflow_remark"),
                        @Result(property = "platform", column = "platform")
        })
        List<OwnerFlowSummary> getFlowSummary(String owner, String clearingDate);

}
