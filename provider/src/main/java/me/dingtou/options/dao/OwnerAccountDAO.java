package me.dingtou.options.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.dingtou.options.model.OwnerAccount;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.type.JdbcType;


/**
 * OwnerAccountDAO
 *
 * @author qiyan
 */
public interface OwnerAccountDAO extends BaseMapper<OwnerAccount> {

    /**
     * 查询Owner
     * 
     * owner是唯一建
     * 
     * @param owner owner
     * @return OwnerAccount
     */
    @Select("SELECT * FROM owner_account WHERE owner = #{owner} and status = 1")
    @Results({
        @Result(property = "ext", column = "ext", jdbcType = JdbcType.VARCHAR, typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    })
    OwnerAccount queryOwner(String owner);
}
