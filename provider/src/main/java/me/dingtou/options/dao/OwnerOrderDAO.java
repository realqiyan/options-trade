package me.dingtou.options.dao;

import java.util.List;

import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.type.JdbcType;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.dingtou.options.model.OwnerOrder;

/**
 * OwnerOrderDAO
 *
 * @author qiyan
 */
public interface OwnerOrderDAO extends BaseMapper<OwnerOrder> {

    /**
     * 查询owner订单
     * 
     * @param owner 账号
     * @return owner订单
     */
    @Select("SELECT * FROM owner_order WHERE owner = #{owner}")
    @Results({
            @Result(property = "ext", column = "ext", jdbcType = JdbcType.VARCHAR, typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    })
    List<OwnerOrder> queryOwnerOrder(String owner);

    /**
     * 查询owner订单
     * 
     * @param owner 账号
     * @param id    订单ID
     * @return owner订单
     */
    @Select("SELECT * FROM owner_order WHERE owner = #{owner} and id = #{id}")
    @Results({
            @Result(property = "ext", column = "ext", jdbcType = JdbcType.VARCHAR, typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    })
    OwnerOrder queryOwnerOrderById(String owner, Long id);

    /**
     * 查询owner策略订单
     * 
     * @param owner      账号
     * @param strategyId 策略ID
     * @return owner策略订单
     */
    @Select("SELECT * FROM owner_order WHERE owner = #{owner} and strategy_id = #{strategyId}")
    @Results({
            @Result(property = "ext", column = "ext", jdbcType = JdbcType.VARCHAR, typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    })
    List<OwnerOrder> queryOwnerStrategyOrder(String owner, String strategyId);

    /**
     * 查询owner草稿订单
     * 
     * @param owner 账号
     * @return owner草稿订单
     */
    @Select("SELECT * FROM owner_order WHERE owner = #{owner} and strategy_id is null")
    @Results({
            @Result(property = "ext", column = "ext", jdbcType = JdbcType.VARCHAR, typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    })
    List<OwnerOrder> queryOwnerDraftOrder(String owner);

    /**
     * 查询owner平台订单
     * 
     * @param owner           账号
     * @param platformOrderId 平台订单ID
     * @return owner平台订单
     */
    @Select("SELECT * FROM owner_order WHERE owner = #{owner} and platform_order_id = #{platformOrderId}")
    @Results({
            @Result(property = "ext", column = "ext", jdbcType = JdbcType.VARCHAR, typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    })
    List<OwnerOrder> queryOwnerPlatformOrder(String owner, String platformOrderId);
}
