package me.dingtou.options.manager;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.dao.OwnerKnowledgeDAO;
import me.dingtou.options.model.OwnerKnowledge;
import me.dingtou.options.model.OwnerKnowledge.KnowledgeStatus;
import me.dingtou.options.model.OwnerKnowledge.KnowledgeType;
import me.dingtou.options.util.TemplateRenderer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * 知识库管理器
 *
 * @author ai
 */
@Slf4j
@Component
public class KnowledgeManager {

    private static final List<OwnerKnowledge> DEFAULT_STRATEGIES = new ArrayList<>(3);

    @Autowired
    private OwnerKnowledgeDAO ownerKnowledgeDAO;

    /**
     * 查询所有知识库
     *
     * @param owner 所有者
     * @return 知识库列表
     */
    public List<OwnerKnowledge> listKnowledges(String owner) {
        List<OwnerKnowledge> knowledges = new ArrayList<>();
        knowledges.addAll(getDefaultStrategies(owner));
        knowledges.addAll(ownerKnowledgeDAO.queryByType(owner, KnowledgeType.RULES.getCode()));
        return knowledges;
    }

    /**
     * 根据类型查询知识库
     *
     * @param owner 所有者
     * @param type  类型
     * @return 知识库列表
     */
    public List<OwnerKnowledge> listKnowledgesByType(String owner, Integer type) {
        List<OwnerKnowledge> knowledges = new ArrayList<>();
        if (KnowledgeType.OPTIONS_STRATEGY.getCode().equals(type)) {
            knowledges.addAll(getDefaultStrategies(owner));
        } else {
            knowledges.addAll(ownerKnowledgeDAO.queryByType(owner, type));
        }
        return knowledges;
    }

    /**
     * 保存知识库
     *
     * @param knowledge 知识库
     * @return 保存后的知识库
     */
    public OwnerKnowledge saveKnowledge(OwnerKnowledge knowledge) {
        if (KnowledgeType.OPTIONS_STRATEGY.getCode().equals(knowledge.getType())) {
            throw new IllegalArgumentException("策略知识不允许更新");
        }
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
        if (null == id || id == Long.MIN_VALUE) {
            throw new IllegalArgumentException("默认策略不允许更新状态");
        }
        LambdaUpdateWrapper<OwnerKnowledge> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(OwnerKnowledge::getId, id)
                .set(OwnerKnowledge::getStatus, status)
                .set(OwnerKnowledge::getUpdateTime, new Date());
        return ownerKnowledgeDAO.update(null, updateWrapper) > 0;
    }

    /**
     * 物理删除知识库
     *
     * @param id ID
     * @return 是否删除成功
     */
    public boolean deleteKnowledge(Long id) {
        if (null == id || id == Long.MIN_VALUE) {
            throw new IllegalArgumentException("默认策略不允许删除");
        }
        return ownerKnowledgeDAO.deleteById(id) > 0;
    }

    /**
     * 根据所有者和编码查询策略
     *
     * @param owner 所有者
     * @param code  策略编码
     * @return 策略
     */
    public OwnerKnowledge getStrategyByOwnerAndCode(String owner, String code) {
        return getDefaultStrategies(owner).stream()
                .filter(strategy -> strategy.getCode().equals(code))
                .findFirst()
                .orElse(null);
    }

    /**
     * 查询默认策略
     *
     * @return 默认策略列表
     */
    public List<OwnerKnowledge> getDefaultStrategies(String owner) {

        if (DEFAULT_STRATEGIES.isEmpty()) {
            synchronized (DEFAULT_STRATEGIES) {
                if (!DEFAULT_STRATEGIES.isEmpty()) {
                    return DEFAULT_STRATEGIES;
                }
                DEFAULT_STRATEGIES.add(createDefaultStrategy(owner, "cc_strategy", "备兑看涨策略(Covered Call Strategy)"));
                DEFAULT_STRATEGIES.add(createDefaultStrategy(owner, "wheel_strategy", "车轮策略 (Wheel Strategy)"));
                DEFAULT_STRATEGIES.add(createDefaultStrategy(owner, "default", "默认卖期权策略(Default Strategy)"));
            }
        }
        return DEFAULT_STRATEGIES;
    }

    /**
     * 创建默认策略
     * 
     * @param owner 所有者
     * @param code  编码
     * @param title 标题
     * @return 策略
     */
    private OwnerKnowledge createDefaultStrategy(String owner, String code, String title) {
        OwnerKnowledge strategy = new OwnerKnowledge();
        strategy.setOwner(owner);
        strategy.setId(Long.MIN_VALUE);
        strategy.setType(KnowledgeType.OPTIONS_STRATEGY.getCode());
        strategy.setCode(code);
        strategy.setTitle(title);
        strategy.setContent(TemplateRenderer.render("default_strategy_" + code + ".ftl", new HashMap<>()));
        strategy.setStatus(KnowledgeStatus.ENABLED.getCode());
        return strategy;
    }

}