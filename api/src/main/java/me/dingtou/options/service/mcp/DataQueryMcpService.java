package me.dingtou.options.service.mcp;

/**
 * 数据查询MCP服务
 *
 * @author qiyan
 */
public interface DataQueryMcpService {

    /**
     * 查询期权到期日列表
     *
     * @param code   股票代码
     * @param market 市场代码
     * 
     * @return 期权到期日列表
     */
    String queryOptionsExpDate(String code,
            Integer market);

    /**
     * 查询期权链
     *
     * @param code       股票代码
     * @param market     市场代码
     * @param strikeDate 期权到期日
     * 
     * @return 期权链
     */
    String queryOptionsChain(String code,
            Integer marke,
            String strikeDate);

    /**
     * 查询市场实时盘口
     *
     * @param code   股票代码
     * @param market 市场代码
     * @return 实时盘口
     */
    String queryOrderBook(String code,
            Integer market);

}
