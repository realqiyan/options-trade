package me.dingtou.options.service.mcp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import me.dingtou.options.manager.OwnerManager;
import me.dingtou.options.manager.TradeManager;
import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.model.OwnerPosition;
import me.dingtou.options.model.OwnerSummary;
import me.dingtou.options.service.AuthService;
import me.dingtou.options.service.SummaryService;
import me.dingtou.options.util.TemplateRenderer;

@Service
public class OwnerQueryMcpService {

    @Autowired
    private AuthService authService;

    @Autowired
    private SummaryService summaryService;

    @Autowired
    private TradeManager tradeManager;

    @Autowired
    private OwnerManager ownerManager;


    @Tool(description = "查询用户的所有策略信息和未平仓的期权订单，策略信息包含：底层标的、策略组合Delta、策略盈亏、策略持有股票情况等。")
    public String queryOwnerStrategy(@ToolParam(required = true, description = "用户Token") String ownerCode) {
        String owner = authService.decodeOwner(ownerCode);
        if (null == owner) {
            return "用户编码信息不正确或已经过期";
        }
        OwnerSummary ownerSummary = summaryService.queryOwnerSummary(owner);

         // 准备模板数据
        Map<String, Object> data = new HashMap<>();
        data.put("ownerSummary", ownerSummary);

        // 渲染模板
        return TemplateRenderer.render("mcp_owner_strategy.ftl", data);
    }


    @Tool(description = "查询用户所以持仓明细，包含用户所持有的股票和期权信息的代码、名称、持仓数量、可卖数量、成本价、当前价。")
    public String queryOwnerPosition(@ToolParam(required = true, description = "用户Token") String ownerCode) {
        String owner = authService.decodeOwner(ownerCode);
        if (null == owner) {
            return "用户编码信息不正确或已经过期";
        }
         OwnerAccount account = ownerManager.queryOwnerAccount(owner);
        List<OwnerPosition> ownerPositionList = tradeManager.queryOwnerPosition(account, null);

         // 准备模板数据
        Map<String, Object> data = new HashMap<>();
        data.put("positions", ownerPositionList);

        // 渲染模板
        return TemplateRenderer.render("mcp_owner_position.ftl", data);
    }


}
