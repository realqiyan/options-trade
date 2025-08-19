当前日期${currentDate}。<#if summary??>策略ID:${summary.strategy.strategyId}，期权策略Code:${summary.strategy.strategyCode}，当前期权策略是：${summary.getOptionsStrategy().getTitle() }。</#if>
当前${securityQuote.security.toString()}股票价格是${securityPrice}。<#if vixIndicator?? && vixIndicator.currentVix??>，当前VIX指数是${vixIndicator.currentVix.value}。</#if>
我打算长期持有${securityQuote.security.toString()}，计划通过Covered Call Strateg策略对冲组合风险，整体策略持仓组合每股Delta目标0.25到0.75之间。
备选的期权距离到期日${optionsChain.dte()}天，请查询策略详情，基于策略详情和市场环境分析`交易期权标的`里的标的，给我这些期权标的交易建议。