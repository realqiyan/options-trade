package me.dingtou.options.manager;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.dao.OwnerKnowledgeDAO;
import me.dingtou.options.model.OwnerKnowledge;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Date;
import java.util.List;

/**
 * 知识库管理器
 *
 * @author ai
 */
@Slf4j
@Component
public class KnowledgeManager {

    @Autowired
    private OwnerKnowledgeDAO ownerKnowledgeDAO;

    /**
     * 查询所有知识库
     *
     * @param owner 所有者
     * @return 知识库列表
     */
    public List<OwnerKnowledge> listKnowledges(String owner) {
        return ownerKnowledgeDAO.queryByOwner(owner);
    }

    /**
     * 根据类型查询知识库
     *
     * @param owner 所有者
     * @param type  类型
     * @return 知识库列表
     */
    public List<OwnerKnowledge> listKnowledgesByType(String owner, Integer type) {
        return ownerKnowledgeDAO.queryByOwnerAndType(owner, type);
    }

    /**
     * 保存知识库
     *
     * @param knowledge 知识库
     * @return 保存后的知识库
     */
    public OwnerKnowledge saveKnowledge(OwnerKnowledge knowledge) {
        Date now = new Date();
        if (knowledge.getId() == null) {
            // 新增
            knowledge.setCreateTime(now);
            knowledge.setUpdateTime(now);
            ownerKnowledgeDAO.insert(knowledge);
        } else {
            // 更新
            knowledge.setUpdateTime(now);
            ownerKnowledgeDAO.updateById(knowledge);
        }
        return knowledge;
    }

    /**
     * 更新知识库状态
     *
     * @param id     ID
     * @param status 状态
     * @return 是否更新成功
     */
    public boolean updateKnowledgeStatus(Long id, Integer status) {
        LambdaUpdateWrapper<OwnerKnowledge> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(OwnerKnowledge::getId, id)
                .set(OwnerKnowledge::getStatus, status)
                .set(OwnerKnowledge::getUpdateTime, new Date());
        return ownerKnowledgeDAO.update(null, updateWrapper) > 0;
    }

}