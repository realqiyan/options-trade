当前日期${currentDate}。<#if summary??>策略ID:${summary.strategy.strategyId}，期权策略Code:${summary.strategy.strategyCode}，当前期权策略是：${summary.getOptionsStrategy().getTitle() }。</#if>
当前${securityQuote.security.toString()}股票价格是${securityPrice}。<#if vixIndicator?? && vixIndicator.currentVix??>，当前VIX指数是${vixIndicator.currentVix.value}。</#if>
我打算卖`交易期权标的`里的期权标的赚取期权权利金，并且我只卖单腿期权，可以是Sell Put或Sell Call。
备选的期权距离到期日${optionsChain.dte()}天，请查询策略详情，基于策略详情和市场环境分析`交易期权标的`里的标的，给我这些期权标的交易建议。