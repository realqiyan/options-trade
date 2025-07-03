package me.dingtou.options.service.mcp;

/**
 * 数据查询MCP服务
 *
 * @author qiyan
 */
public interface DataQueryMcpService {

        /**
         * 查询股票价格
         *
         * @param ownerCode 用户加密编码
         * @param code      股票代码
         * @param market    市场代码
         * 
         * @return 股票价格
         */
        String queryStockRealPrice(String ownerCode,
                        String code,
                        Integer market);

        /**
         * 查询期权到期日列表
         *
         * @param ownerCode 用户加密编码
         * @param code      股票代码
         * @param market    市场代码
         * 
         * @return 期权到期日列表
         */
        String queryOptionsExpDate(String ownerCode,
                        String code,
                        Integer market);

        /**
         * 查询期权链
         *
         * @param ownerCode  用户加密编码
         * @param code       股票代码
         * @param market     市场代码
         * @param strikeDate 期权到期日
         * 
         * @return 期权链
         */
        String queryOptionsChain(String ownerCode,
                        String code,
                        Integer marke,
                        String strikeDate);

        /**
         * 查询恐慌指数和标普500指数
         * 
         * @return 恐慌指数和标普500指数
         */
        String queryVixIndicator();

        /**
         * 查询股票指标
         *
         * @param ownerCode 用户加密编码
         * @param code      股票代码
         * @param market    市场代码
         * 
         * @return 股票指标
         */
        String queryStockIndicator(String ownerCode,
                        String code,
                        Integer marke);

        /**
         * 查询市场实时盘口
         *
         * @param ownerCode 用户加密编码
         * @param code      股票代码
         * @param market    市场代码
         * @return 实时盘口
         */
        String queryOrderBook(String ownerCode,
                        String code,
                        Integer market);

}
