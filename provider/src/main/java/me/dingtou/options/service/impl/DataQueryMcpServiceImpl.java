package me.dingtou.options.service.impl;

import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import me.dingtou.options.manager.OptionsManager;
import me.dingtou.options.manager.OwnerManager;
import me.dingtou.options.manager.TradeManager;
import me.dingtou.options.model.OptionsChain;
import me.dingtou.options.model.OptionsStrikeDate;
import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.model.Security;
import me.dingtou.options.model.SecurityOrderBook;
import me.dingtou.options.service.mcp.DataQueryMcpService;

@Service
public class DataQueryMcpServiceImpl implements DataQueryMcpService {

    @Autowired
    private OptionsManager optionsManager;

    @Autowired
    private TradeManager tradeManager;

    @Autowired
    private OwnerManager ownerManager;

    @Tool(description = "查询期权到期日，根据股票代码和市场代码查询股票对应的期权到期日。")
    @Override
    public List<OptionsStrikeDate> queryOptionsExpDate(@ToolParam(required = true, description = "股票代码") String code,
            @ToolParam(required = true, description = "市场代码 1:港股 11:美股") Integer market) {
        return optionsManager.queryOptionsExpDate(code, market);
    }

    @Tool(description = "查询指定日期期权链，根据股票代码、市场代码、到期日查询股票对应的期权到期日的期权链。")
    @Override
    public OptionsChain queryOptionsChain(@ToolParam(required = true, description = "股票代码") String code,
            @ToolParam(required = true, description = "市场代码 1:港股 11:美股") Integer marke,
            @ToolParam(required = true, description = "期权到期日 2025-06-27") String strikeDate) {
        OwnerAccount account = ownerManager.queryOwnerAccount("qiyan");
        return optionsManager.queryOptionsChain(account, Security.of(code, marke), strikeDate);
    }

    @Tool(description = "查询指定期权代码的报价，根据期权代码、市场代码查询当前期权买卖报价。")
    @Override
    public SecurityOrderBook queryOrderBook(@ToolParam(required = true, description = "期权代码") String code,
            @ToolParam(required = true, description = "市场代码 1:港股 11:美股") Integer market) {
        return tradeManager.querySecurityOrderBook(code, market);
    }

}
