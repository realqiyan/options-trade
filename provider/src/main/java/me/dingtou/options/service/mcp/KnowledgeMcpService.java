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

    @Tool(description = "查询期权交易策略规则。系统提供三种默认策略：1) 备兑看涨策略(Covered Call) - 适用于持有标的股票并卖出看涨期权的场景，包含首次开仓规则、策略delta监控与调整规则、到期日调整规则等；2) 车轮策略(Wheel Strategy) - 包含股票趋势确认、市场安全评估、技术指标分析、持仓管理等完整流程；3) 默认卖期权策略 - 适用于单腿期权卖出的基础策略。每个策略都包含具体的交易规则、delta要求、行权日期选择要求、技术指标要求等详细信息。当指定strategyCode参数时，返回指定策略的详细规则；不指定时，返回所有可用策略的规则列表。")
    public String queryStrategyRule(@ToolParam(required = true, description = "用户Token") String ownerCode,
            @ToolParam(required = false, description = "期权策略编码，可选值：cc_strategy(备兑看涨策略)、wheel_strategy(车轮策略)、default(默认卖期权策略)。不指定时返回所有策略") String strategyCode) {
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
