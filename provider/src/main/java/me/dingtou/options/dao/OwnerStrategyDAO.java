package me.dingtou.options.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.dingtou.options.model.OwnerStrategy;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.type.JdbcType;

import java.util.List;

/**
 * OwnerStrategyDAO
 *
 * @author qiyan
 */
public interface OwnerStrategyDAO extends BaseMapper<OwnerStrategy> {
    
    /**
     * 查询Owner的策略列表
     *
     * @param owner owner
     * @return 策略列表
     */
    @Select("SELECT * FROM owner_strategy WHERE owner = #{owner} and status = 1 order by strategy_name")
    @Results({
        @Result(property = "ext", column = "ext", jdbcType = JdbcType.VARCHAR, typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    })
    List<OwnerStrategy> queryOwnerStrategies(String owner);
    
    /**
     * 根据ID查询策略
     *
     * @param id 策略ID
     * @return 策略
     */
    @Select("SELECT * FROM owner_strategy WHERE id = #{id}")
    @Results({
        @Result(property = "ext", column = "ext", jdbcType = JdbcType.VARCHAR, typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    })
    OwnerStrategy queryStrategyById(Long id);


    /**
     * 根据ID查询策略
     *
     * @param id 策略ID
     * @return 策略
     */
    @Select("SELECT * FROM owner_strategy WHERE strategy_id = #{strategyId}")
    @Results({
            @Result(property = "ext", column = "ext", jdbcType = JdbcType.VARCHAR, typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    })
    OwnerStrategy queryStrategyByStrategyId(String strategyId);

}
