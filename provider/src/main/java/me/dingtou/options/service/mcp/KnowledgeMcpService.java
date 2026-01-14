package me.dingtou.options.service.mcp;

import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import me.dingtou.options.manager.KnowledgeManager;
import me.dingtou.options.model.OwnerKnowledge;
import me.dingtou.options.model.OwnerKnowledge.KnowledgeType;
import me.dingtou.options.util.TemplateRenderer;

@Service
public class KnowledgeMcpService extends BaseMcpService {

    @Autowired
    private KnowledgeManager knowledgeManager;

    @Tool(description = "查询期权交易策略规则。策略规则包含具体的交易规则、delta要求、行权日期选择要求、技术指标要求等详细信息。")
    @PreAuthorize("isAuthenticated()")
    public String queryStrategyRule(
            @ToolParam(required = false, description = "期权策略编码，可选值：cc_strategy(备兑看涨策略)、wheel_strategy(车轮策略)、default(默认卖期权策略)，不指定时返回所有策略。") String strategyCode,
            @ToolParam(required = false, description = "数据格式：json、markdown") String format) {
        String owner = getOwner();
        if (null == owner) {
            return "用户编码信息不正确或已经过期";
        }
        List<OwnerKnowledge> knowledges = knowledgeManager.listKnowledgesByType(owner,
                KnowledgeType.OPTIONS_STRATEGY.getCode());
        if (StringUtils.isNotBlank(strategyCode)) {
            knowledges = knowledges.stream().filter(e -> strategyCode.equals(e.getCode())).toList();
        }

        Map<String, Object> data = new HashMap<>();
        data.put("strategies", knowledges);
        if (isJson(format)) {
            return jsonString(data);
        }
        // 渲染模板
        return TemplateRenderer.render("mcp_strategy_rule.ftl", data);
    }

}
