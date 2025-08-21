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

    @Tool(description = "根据期权策略Code查询期权策略规则。期权策略规则包含期权交易的具体要求和规则，例如：delta要求、行权日期选择要求、技术指标要求等，是咨询期权策略时必须依赖的重要信息。（指定期权策略Code时查询指定的期权策略规则详情，不指定时查询所有期权策略规则。）")
    public String queryStrategyRule(@ToolParam(required = true, description = "用户Token") String ownerCode,
            @ToolParam(required = false, description = "期权策略Code") String strategyCode) {
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
