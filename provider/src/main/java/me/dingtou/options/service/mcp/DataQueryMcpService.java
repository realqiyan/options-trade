package me.dingtou.options.service.mcp;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.constant.CandlestickPeriod;
import me.dingtou.options.constant.OptionsFilterType;
import me.dingtou.options.manager.IndicatorManager;
import me.dingtou.options.manager.OptionsQueryManager;
import me.dingtou.options.manager.OwnerManager;
import me.dingtou.options.manager.TradeManager;
import me.dingtou.options.model.IndicatorDataFrame;
import me.dingtou.options.model.OptionsChain;
import me.dingtou.options.model.OptionsRealtimeData;
import me.dingtou.options.model.OptionsStrikeDate;
import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.model.Security;
import me.dingtou.options.model.SecurityCandlestick;
import me.dingtou.options.model.SecurityOrderBook;
import me.dingtou.options.model.StockIndicator;
import me.dingtou.options.model.VixIndicator;
import me.dingtou.options.model.EarningsCalendar;
import me.dingtou.options.service.AuthService;
import me.dingtou.options.service.EarningsCalendarService;
import me.dingtou.options.util.IndicatorDataFrameUtil;
import me.dingtou.options.util.TemplateRenderer;

@Service
@Slf4j
public class DataQueryMcpService {

    @Autowired
    private AuthService authService;

    @Autowired
    private OptionsQueryManager optionsQueryManager;

    @Autowired
    private TradeManager tradeManager;

    @Autowired
    private OwnerManager ownerManager;

    @Autowired
    private IndicatorManager indicatorManager;

    @Autowired
    private EarningsCalendarService earningsCalendarService;

    @Tool(description = "查询期权到期日列表。根据股票代码和市场代码查询该股票对应的所有期权到期日。返回结果包括股票代码、市场代码和到期日列表，每个到期日包含具体的日期和相关的期权信息。")
    public String queryOptionsExpDate(@ToolParam(required = true, description = "用户Token") String ownerCode,
            @ToolParam(required = true, description = "股票代码，如AAPL、TSLA等") String code,
            @ToolParam(required = true, description = "市场代码：1表示港股，11表示美股") Integer market) {
        String encodeOwner = authService.decodeOwner(ownerCode);
        if (null == encodeOwner) {
            return "用户编码信息不正确或已经过期";
        }
        try {
            List<OptionsStrikeDate> expDates = optionsQueryManager.queryOptionsExpDate(code, market);
            // 准备模板数据
            Map<String, Object> data = new HashMap<>();
            data.put("security", Security.of(code, market));
            data.put("expDates", expDates);
            // 渲染模板
            return TemplateRenderer.render("mcp_options_exp_date.ftl", data);
        } catch (Exception e) {
            log.error("查询期权到期日失败，code={}, market={}", code, market, e);
            return "查询期权到期日失败，请稍后再试。";
        }
    }

    @Tool(description = "查询指定到期日的期权链数据。根据股票代码、市场代码、到期日、期权类型和交易类型查询期权详细信息。使用前请先使用queryOptionsExpDate工具获取有效的到期日。返回结果包括期权代码、类型(Call/Put)、行权价、当前价格、隐含波动率、希腊字母(Delta、Theta、Gamma)、未平仓合约数、当天交易量等完整信息。注意：当交易类型为SELL时，系统会自动调整Delta和Theta值的符号。")
    public String queryOptionsChain(@ToolParam(required = true, description = "用户Token") String ownerCode,
            @ToolParam(required = true, description = "股票代码，如AAPL、TSLA等") String code,
            @ToolParam(required = true, description = "市场代码：1表示港股，11表示美股") Integer market,
            @ToolParam(required = true, description = "期权到期日，格式为YYYY-MM-DD，如2025-06-27。请先使用queryOptionsExpDate工具获取有效日期") String strikeDate,
            @ToolParam(required = true, description = "期权类型过滤：ALL(全部期权)、PUT(看跌期权)或CALL(看涨期权)") String filterType,
            @ToolParam(required = true, description = "交易类型：SELL(卖出)或BUY(买入)。SELL模式下Delta和Theta值会取负数") String tradeType) {
        String owner = authService.decodeOwner(ownerCode);
        if (null == owner) {
            return "用户编码信息不正确或已经过期";
        }
        try {
            OwnerAccount account = ownerManager.queryOwnerAccount(owner);

            OptionsChain optionsChain = optionsQueryManager.queryOptionsChain(account, Security.of(code, market),
                    strikeDate,
                    false,
                    OptionsFilterType.of(filterType));

            if (null == optionsChain || null == optionsChain.getOptionsList()) {
                return "期权链无结果";
            }
            if ("SELL".equalsIgnoreCase(tradeType)) {
                optionsChain.getOptionsList().forEach(options -> {
                    OptionsRealtimeData realtimeData = options.getRealtimeData();
                    if (null != realtimeData) {
                        realtimeData.setDelta(realtimeData.getDelta().multiply(BigDecimal.valueOf(-1)));
                        realtimeData.setTheta(realtimeData.getTheta().multiply(BigDecimal.valueOf(-1)));
                    }
                });
            }

            // 准备模板数据
            Map<String, Object> data = new HashMap<>();
            data.put("security", optionsChain.getSecurity());
            data.put("strikeTime", optionsChain.getStrikeTime());
            data.put("optionsList", optionsChain.getOptionsList());

            // 渲染模板
            return TemplateRenderer.render("mcp_options_chain.ftl", data);
        } catch (Exception e) {
            log.error("查询期权链失败，code={}, market={}, strikeDate={}", code, market, strikeDate, e);
            return String.format("查询 %s %s 的期权链失败，请先确认已经查询期权到期日。", code, strikeDate);
        }
    }

    @Tool(description = "查询股票K线数据（日K或周K）。根据股票代码、市场代码、K线类型和数量查询股票历史K线数据。返回结果包括指定数量的K线数据，每条K线包含日期、开盘价、收盘价、最高价、最低价、成交量和成交额。最多可查询90条K线数据。")
    public String queryStockCandlesticks(@ToolParam(required = true, description = "用户Token") String ownerCode,
            @ToolParam(required = true, description = "股票代码，如AAPL、TSLA等") String code,
            @ToolParam(required = true, description = "市场代码：1表示港股，11表示美股") Integer market,
            @ToolParam(required = true, description = "K线类型：1000表示日K线，2000表示周K线") Integer periodCode,
            @ToolParam(required = true, description = "K线数量，最多可查询90条数据") Integer count) {
        String owner = authService.decodeOwner(ownerCode);
        if (null == owner) {
            return "用户编码信息不正确或已经过期";
        }
        try {
            Security security = Security.of(code, market);
            OwnerAccount account = ownerManager.queryOwnerAccount(owner);
            CandlestickPeriod period = CandlestickPeriod.of(periodCode);
            SecurityCandlestick candlesticks = indicatorManager.getCandlesticks(account, security, period, count);
            Map<String, Object> data = new HashMap<>();
            data.put("security", security);
            data.put("periodName", period.getName());
            data.put("count", count);
            data.put("candlesticks", candlesticks.getCandlesticks());
            // 渲染模板
            return TemplateRenderer.render("mcp_stock_candlesticks.ftl", data);
        } catch (Exception e) {
            log.error("查询股票K线数据失败，code={}, market={}, periodCode={}, count={}", code, market, periodCode, count, e);
            return "查询股票K线数据失败，请稍后再试。";
        }
    }

    @Tool(description = "查询股票技术指标数据。根据股票代码、市场代码、指标周期和数量查询股票的技术分析指标，包括MA(移动平均线)、BOLL(布林带)、MACD和RSI等常用指标。返回结果包含指定数量的历史数据，每条数据包括日期、布林带下轨/中轨/上轨、EMA20/EMA5/EMA50指数移动平均线、MACD指标(DEA/DIF)、RSI相对强弱指数等。最多可查询90条数据。")
    public String queryStockIndicator(@ToolParam(required = true, description = "用户Token") String ownerCode,
            @ToolParam(required = true, description = "股票代码，如AAPL、TSLA等") String code,
            @ToolParam(required = true, description = "市场代码：1表示港股，11表示美股") Integer market,
            @ToolParam(required = true, description = "指标周期：1000表示日线指标，2000表示周线指标") Integer periodCode,
            @ToolParam(required = true, description = "指标数量，最多可查询90条数据") Integer count) {
        String owner = authService.decodeOwner(ownerCode);
        if (null == owner) {
            return "用户编码信息不正确或已经过期";
        }

        try {
            Security security = Security.of(code, market);
            OwnerAccount account = ownerManager.queryOwnerAccount(owner);
            CandlestickPeriod period = CandlestickPeriod.of(periodCode);
            StockIndicator stockIndicator = indicatorManager.calculateStockIndicator(account, security, period, count);
            IndicatorDataFrame stockIndicatorDataFrame = IndicatorDataFrameUtil.createDataFrame(stockIndicator, count);
            Map<String, Object> data = new HashMap<>();
            data.put("security", security);
            data.put("periodName", period.getName());
            data.put("count", count);
            data.put("stockIndicator", stockIndicator);
            data.put("stockIndicatorDataFrame", stockIndicatorDataFrame);
            // 渲染模板
            return TemplateRenderer.render("mcp_stock_indicator.ftl", data);
        } catch (Exception e) {
            log.error("查询技术指标失败，code={}, market={}", code, market, e);
            return "查询技术指标失败，请稍后再试。";
        }
    }

    @Tool(description = "查询VIX恐慌指数指标。VIX(Volatility Index)是芝加哥期权交易所(CBOE)推出的市场波动性指数，常被称为恐慌指数，用于衡量标普500指数未来30天的预期波动率。返回结果包括当前VIX值、日期、日变动百分比，以及标普500指数的当前值和日期，帮助投资者评估市场情绪和风险水平。")
    public String queryVixIndicator() {
        VixIndicator vixIndicator = indicatorManager.queryCurrentVix();
        if (null == vixIndicator) {
            log.error("查询VIX恐慌指数指标失败");
            return "查询VIX恐慌指数指标失败，请稍后再试。";
        }
        Map<String, Object> data = new HashMap<>();
        data.put("vixIndicator", vixIndicator);
        // 渲染模板
        String result = TemplateRenderer.render("mcp_vix_indicator.ftl", data);
        return result;
    }

    @Tool(description = "查询期权买卖盘报价数据。根据期权代码和市场代码查询当前期权的买卖盘详细信息，包括不同价位的买卖挂单数量和价格。返回结果包括期权代码、市场代码以及完整的买卖盘价格列表，帮助投资者了解当前市场的流动性和价格深度。")
    public String queryOrderBook(@ToolParam(required = true, description = "用户Token") String ownerCode,
            @ToolParam(required = true, description = "期权代码，如AAPL250620C150等") String code,
            @ToolParam(required = true, description = "市场代码：1表示港股，11表示美股") Integer market) {

        String owner = authService.decodeOwner(ownerCode);
        if (null == owner) {
            return "用户编码信息不正确或已经过期";
        }
        try {
            SecurityOrderBook orderBook = tradeManager.querySecurityOrderBook(code, market);
            // 准备模板数据
            Map<String, Object> data = new HashMap<>();
            data.put("security", Security.of(code, market));
            data.put("orderBook", orderBook);
            // 渲染模板
            return TemplateRenderer.render("mcp_order_book.ftl", data);
        } catch (Exception e) {
            log.error("查询期权报价失败，code={}, market={}", code, market, e);
            return "查询期权报价失败，请稍后再试。";
        }
    }

    @Tool(description = "查询股票实时价格。根据股票代码和市场代码查询股票的当前最新价格。返回结果为股票的实时价格数值，可用于快速获取股票当前市场价值。")
    public String queryStockRealPrice(@ToolParam(required = true, description = "用户Token") String ownerCode,
            @ToolParam(required = true, description = "股票代码，如AAPL、TSLA等") String code,
            @ToolParam(required = true, description = "市场代码：1表示港股，11表示美股") Integer market) {
        String owner = authService.decodeOwner(ownerCode);
        if (null == owner) {
            return "用户编码信息不正确或已经过期";
        }
        try {
            OwnerAccount account = ownerManager.queryOwnerAccount(owner);
            return indicatorManager.queryStockPrice(account, Security.of(code, market)).toPlainString();
        } catch (Exception e) {
            log.error("查询股票当前价格失败，code={}, market={}", code, market, e);
            return "查询股票当前价格失败，请稍后再试。";
        }
    }

    @Tool(description = "查询股票财报日历信息。根据股票代码查询该公司过去和未来的财报发布计划，包括财报日期、预期每股收益、实际每股收益(如已发布)等关键财务数据。返回结果包括股票代码、公司名称、财报日期、预期每股收益、实际每股收益、发布时间等信息，帮助投资者跟踪公司财务表现和规划投资策略。注意：此功能不需要用户Token。")
    public String queryEarningsCalendar(@ToolParam(required = true, description = "股票代码，如AAPL、TSLA、BABA等，不区分大小写") String code) {
        if (StringUtils.isBlank(code)) {
            return "股票代码不能为空";
        }

        try {
            List<EarningsCalendar> earningsCalendars = earningsCalendarService.getEarningsCalendarBySymbol(code);

            // 准备模板数据
            Map<String, Object> data = new HashMap<>();
            data.put("symbol", code);
            data.put("earningsCalendars", earningsCalendars);
            // 渲染模板
            return TemplateRenderer.render("mcp_earnings_calendar.ftl", data);
        } catch (Exception e) {
            log.error("查询财报日历失败，code={}", code, e);
            return "查询财报日历失败，请稍后再试。";
        }
    }

}
