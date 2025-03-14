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
    
    @Select("SELECT * FROM owner_order WHERE owner = #{owner}")
    @Results({
        @Result(property = "ext", column = "ext", jdbcType = JdbcType.VARCHAR, typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    })
    List<OwnerOrder> selectByOwner(String owner);
}
