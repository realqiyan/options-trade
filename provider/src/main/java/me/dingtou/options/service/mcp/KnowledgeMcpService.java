package me.dingtou.options.service.mcp;

import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import me.dingtou.options.manager.KnowledgeManager;
import me.dingtou.options.model.OwnerKnowledge;
import me.dingtou.options.model.OwnerKnowledge.KnowledgeType;
import me.dingtou.options.service.AuthService;
import me.dingtou.options.util.TemplateRenderer;

@Service
public class KnowledgeMcpService {

    @Autowired
    private AuthService authService;

    @Autowired
    private KnowledgeManager knowledgeManager;

    @Tool(description = "查询期权策略规则。返回指定策略或所有策略的规则详情，包括delta要求、行权日期选择要求、技术指标要求等交易规则。")
    public String queryStrategyRule(@ToolParam(required = true, description = "用户Token") String ownerCode,
            @ToolParam(required = false, description = "期权策略Code，不指定则返回所有策略规则") String strategyCode) {
        String owner = authService.decodeOwner(ownerCode);
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
        // 渲染模板
        return TemplateRenderer.render("mcp_strategy_rule.ftl", data);
    }

}
