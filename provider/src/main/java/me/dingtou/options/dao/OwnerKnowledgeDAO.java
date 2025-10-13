package me.dingtou.options.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.dingtou.options.model.OwnerKnowledge;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 知识库DAO接口
 *
 * @author ai
 */
public interface OwnerKnowledgeDAO extends BaseMapper<OwnerKnowledge> {

    /**
     * 根据所有者查询知识库列表
     *
     * @param owner 所有者
     * @return 知识库列表
     */
    @Select("SELECT * FROM owner_knowledge WHERE owner = #{owner}")
    List<OwnerKnowledge> queryByOwner(@Param("owner") String owner);

    /**
     * 根据所有者和类型查询知识库列表
     *
     * @param owner 所有者
     * @param type  类型
     * @return 知识库列表
     */
    @Select("SELECT * FROM owner_knowledge WHERE owner = #{owner} AND type = #{type}")
    List<OwnerKnowledge> queryByType(@Param("owner") String owner, @Param("type") Integer type);

    /**
     * 根据所有者和编码查询知识库
     *
     * @param owner 所有者
     * @param type  类型
     * @param code  编码
     * @return 知识库
     */
    @Select("SELECT * FROM owner_knowledge WHERE owner = #{owner} AND type = #{type} AND code = #{code}")
    OwnerKnowledge queryByTypeAndCode(@Param("owner") String owner,
            @Param("type") Integer type,
            @Param("code") String code);

}