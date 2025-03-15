package me.dingtou.options.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.dingtou.options.model.OwnerTradeTask;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.type.JdbcType;

import java.util.List;

/**
 * 交易任务DAO
 *
 * @author qiyan
 */
public interface OwnerTradeTaskDAO extends BaseMapper<OwnerTradeTask> {

    /**
     * 查询用户的交易任务
     *
     * @param owner 所有者
     * @return 交易任务列表
     */
    @Select("SELECT * FROM owner_trade_task WHERE owner = #{owner} ORDER BY create_time DESC")
    @Results({
        @Result(property = "ext", column = "ext", jdbcType = JdbcType.VARCHAR, typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    })
    List<OwnerTradeTask> queryTradeTask(@Param("owner") String owner);

    /**
     * 根据会话ID查询交易任务
     *
     * @param owner     所有者
     * @param sessionId 会话ID
     * @return 交易任务列表
     */
    @Select("SELECT * FROM owner_trade_task WHERE owner = #{owner} AND session_id = #{sessionId} ORDER BY create_time DESC")
    @Results({
        @Result(property = "ext", column = "ext", jdbcType = JdbcType.VARCHAR, typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    })
    List<OwnerTradeTask> queryTradeTaskBySessionId(@Param("owner") String owner, @Param("sessionId") String sessionId);

    /**
     * 根据消息ID查询交易任务
     *
     * @param owner     所有者
     * @param messageId 消息ID
     * @return 交易任务列表
     */
    @Select("SELECT * FROM owner_trade_task WHERE owner = #{owner} AND message_id = #{messageId} ORDER BY create_time DESC")
    @Results({
        @Result(property = "ext", column = "ext", jdbcType = JdbcType.VARCHAR, typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    })
    List<OwnerTradeTask> queryTradeTaskByMessageId(@Param("owner") String owner, @Param("messageId") String messageId);

    /**
     * 根据任务ID查询交易任务
     *
     * @param owner 所有者
     * @param id    任务ID
     * @return 交易任务
     */
    @Select("SELECT * FROM owner_trade_task WHERE owner = #{owner} AND id = #{id}")
    @Results({
        @Result(property = "ext", column = "ext", jdbcType = JdbcType.VARCHAR, typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    })
    OwnerTradeTask queryTradeTaskById(@Param("owner") String owner, @Param("id") Long id);

    /**
     * 查询待执行的交易任务
     *
     * @param owner 所有者
     * @return 交易任务列表
     */
    @Select("SELECT * FROM owner_trade_task WHERE owner = #{owner} AND status = 1 ORDER BY create_time ASC")
    @Results({
        @Result(property = "ext", column = "ext", jdbcType = JdbcType.VARCHAR, typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    })
    List<OwnerTradeTask> queryPendingTradeTask(@Param("owner") String owner);
} 