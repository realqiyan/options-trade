package me.dingtou.options.service.mcp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import me.dingtou.options.context.SessionContext;
import me.dingtou.options.manager.IndicatorManager;
import me.dingtou.options.manager.OptionsManager;
import me.dingtou.options.manager.OwnerManager;
import me.dingtou.options.manager.TradeManager;
import me.dingtou.options.model.OptionsChain;
import me.dingtou.options.model.OptionsStrikeDate;
import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.model.Security;
import me.dingtou.options.model.SecurityOrderBook;
import me.dingtou.options.util.TemplateRenderer;

@Service
public class DataQueryMcpServiceImpl implements DataQueryMcpService {

    @Autowired
    private OptionsManager optionsManager;

    @Autowired
    private TradeManager tradeManager;

    @Autowired
    private OwnerManager ownerManager;

    @Autowired
    private IndicatorManager indicatorManager;

    @Tool(description = "查询期权到期日，根据股票代码和市场代码查询股票对应的期权到期日。")
    @Override
    public String queryOptionsExpDate(@ToolParam(required = true, description = "股票代码") String code,
            @ToolParam(required = true, description = "市场代码 1:港股 11:美股") Integer market) {
        List<OptionsStrikeDate> expDates = optionsManager.queryOptionsExpDate(code, market);
        // 准备模板数据
        Map<String, Object> data = new HashMap<>();
        data.put("security", Security.of(code, market));
        data.put("expDates", expDates);
        // 渲染模板
        return TemplateRenderer.render("mcp_options_exp_date.ftl", data);
    }

    @Tool(description = "查询指定日期期权链和股票相关技术指标，根据股票代码、市场代码、到期日查询股票对应的期权到期日的期权链以及股票技术指标（K线、EMA、BOLL、MACD、RSI）。")
    @Override
    public String queryOptionsChain(@ToolParam(required = true, description = "股票代码") String code,
            @ToolParam(required = true, description = "市场代码 1:港股 11:美股") Integer market,
            @ToolParam(required = true, description = "期权到期日 2025-06-27") String strikeDate) {
        OwnerAccount account = ownerManager.queryOwnerAccount(SessionContext.getOwner());
        OptionsChain optionsChain = optionsManager.queryOptionsChain(account, Security.of(code, market), strikeDate);

        // 准备模板数据
        Map<String, Object> data = new HashMap<>();
        data.put("security", optionsChain.getSecurity());
        data.put("strikeTime", optionsChain.getStrikeTime());
        data.put("optionsList", optionsChain.getOptionsList());
        data.put("vixIndicator", optionsChain.getVixIndicator());
        data.put("stockIndicator", optionsChain.getStockIndicator());

        // 渲染模板
        return TemplateRenderer.render("mcp_options_chain.ftl", data);
    }

    @Tool(description = "查询指定期权代码的报价，根据期权代码、市场代码查询当前期权买卖报价。")
    @Override
    public String queryOrderBook(@ToolParam(required = true, description = "期权代码") String code,
            @ToolParam(required = true, description = "市场代码 1:港股 11:美股") Integer market) {
        SecurityOrderBook orderBook = tradeManager.querySecurityOrderBook(code, market);
        // 准备模板数据
        Map<String, Object> data = new HashMap<>();
        data.put("security", Security.of(code, market));
        data.put("orderBook", orderBook);
        // 渲染模板
        return TemplateRenderer.render("mcp_order_book.ftl", data);
    }

    @Tool(description = "查询指股票代码的当前价格，根据股票代码、市场代码查询当前股票价格。")
    @Override
    public String queryStockRealPrice(@ToolParam(required = true, description = "期权代码") String code,
            @ToolParam(required = true, description = "市场代码 1:港股 11:美股") Integer market) {
        OwnerAccount account = ownerManager.queryOwnerAccount(SessionContext.getOwner());
        return indicatorManager.queryStockPrice(account, Security.of(code, market)).toPlainString();
    }

}
