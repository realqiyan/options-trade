package me.dingtou.options.service.mcp;

import java.util.*;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import me.dingtou.options.manager.OwnerManager;
import me.dingtou.options.manager.TradeManager;
import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.model.OwnerPosition;
import me.dingtou.options.model.OwnerSummary;
import me.dingtou.options.model.StrategySummary;
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

    @Tool(description = "查询用户所有运行中的期权策略和未平仓订单。返回策略明细表格（包含策略ID、名称、标的、当前股价等信息）和未平仓订单表格（包含策略ID、订单ID、标的、期权代码、方向、数量、行权价、价格等信息）。")
    public String queryAllStrategy(@ToolParam(required = true, description = "用户Token") String ownerCode) {
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

    @Tool(description = "查询用户指定股票或期权的持仓信息。返回持仓明细表格，包含证券代码、证券名称、持仓数量、可卖数量、成本价、当前价等信息。注意：期权持仓数为负数表示卖出期权合约。")
    public String queryPositionByCode(@ToolParam(required = true, description = "用户Token") String ownerCode,
            @ToolParam(required = true, description = "股票或期权代码，如'BABA'、'AAPL'等") String code,
            @ToolParam(required = true, description = "市场代码：1表示港股，11表示美股") Integer market) {
        String owner = authService.decodeOwner(ownerCode);
        if (null == owner) {
            return "用户编码信息不正确或已经过期";
        }
        OwnerAccount account = ownerManager.queryOwnerAccount(owner);
        List<OwnerPosition> ownerPositionList = tradeManager.queryOwnerPosition(account, List.of(code));
        // 准备模板数据
        Map<String, Object> data = new HashMap<>();
        data.put("positions", ownerPositionList);

        // 渲染模板
        return TemplateRenderer.render("mcp_owner_position.ftl", data);
    }

    @Tool(description = "查询用户所有持仓明细，包含股票和期权信息汇总。返回完整持仓表格，包含证券代码、证券名称、持仓数量、可卖数量、成本价、当前价等信息。注意：期权持仓数为负数表示卖出期权合约。")
    public String queryAllPosition(@ToolParam(required = true, description = "用户Token") String ownerCode) {
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

    @Tool(description = "查询指定策略的详细信息和所有历史订单。返回策略汇总信息（包含策略ID、名称、等价持股数、Delta、盈利、持股情况等关键指标）和订单明细表格（包含标的代码、证券代码、类型、价格、数量、收益、费用、行权时间、交易时间、状态、订单号、是否平仓等详细信息）。")
    public String queryStrategyDetailAndOrders(@ToolParam(required = true, description = "用户Token") String ownerCode,
            @ToolParam(required = true, description = "策略ID") String strategyId) {
        String owner = authService.decodeOwner(ownerCode);
        if (null == owner) {
            return "用户编码信息不正确或已经过期";
        }

        StrategySummary strategySummary = summaryService.queryStrategySummary(owner, strategyId);

        if (null == strategySummary) {
            return "未找到指定策略";
        }

        // 准备模板数据
        Map<String, Object> data = new HashMap<>();
        data.put("strategySummary", strategySummary);
        data.put("includeStrategyRule", false);
        data.put("orders", strategySummary.getStrategyOrders());

        // 渲染模板
        return TemplateRenderer.render("mcp_owner_strategy_orders.ftl", data);
    }

}
