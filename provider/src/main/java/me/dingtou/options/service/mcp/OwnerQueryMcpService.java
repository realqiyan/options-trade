package me.dingtou.options.service.mcp;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import me.dingtou.options.manager.OwnerManager;
import me.dingtou.options.manager.TradeManager;
import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.model.OwnerPosition;
import me.dingtou.options.model.OwnerSecurity;
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

    @Tool(description = "查询用户的所有运行中的策略信息和未平仓的期权订单。返回结果包括策略明细（策略ID、策略名称、策略标的、当前股价）和策略未平仓订单（策略ID、订单ID、底层标的、期权代码、方向、数量、行权价、价格）。")
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

    @Tool(description = "查询用户账户所有持仓明细，包含用户账户下所有策略所持有的股票和期权信息汇总。返回结果包括证券代码、证券名称、持仓数量、可卖数量、成本价、当前价。对于期权持仓，数量为负数时表示卖出期权合约。")
    public String queryPosition(@ToolParam(required = true, description = "用户Token") String ownerCode) {
        String owner = authService.decodeOwner(ownerCode);
        if (null == owner) {
            return "用户编码信息不正确或已经过期";
        }
        OwnerAccount account = ownerManager.queryOwnerAccount(owner);
        List<OwnerSecurity> securities = ownerManager.queryOwnerSecurity(owner);
        List<String> codes = securities.stream().map(OwnerSecurity::getCode).collect(Collectors.toList());
        List<OwnerPosition> ownerPositionList = tradeManager.queryOwnerPosition(account, codes);

        // 准备模板数据
        Map<String, Object> data = new HashMap<>();
        data.put("positions", ownerPositionList);

        // 渲染模板
        return TemplateRenderer.render("mcp_owner_position.ftl", data);
    }

    @Tool(description = "通过策略ID查询用户策略持仓，策略持仓包含汇总信息和所有订单。汇总信息包括：策略Delta、策略盈利、期权盈利、持有股票数量、当前股价、股票支出、持股盈亏、希腊字母等，订单明细包含：标的代码、证券代码、类型、价格、数量、订单收益、订单费用、行权时间、交易时间、状态、订单号、是否平仓等。")
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
