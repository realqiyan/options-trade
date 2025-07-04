# ${security.code}(${security.market})技术指标

<#if stockIndicator??>
## 标的行情
| 指标 | 值 |
| ---- | ---- |
| 最新价 | ${(stockIndicator.securityQuote.lastDone)!} |
| 周波动幅度 | ${(stockIndicator.weekPriceRange)!} |
| 月波动幅度 | ${(stockIndicator.monthPriceRange)!} |

## K线数据
| 日期 | 开盘价 | 收盘价 | 最高价 | 最低价 | 成交量 | 成交额 |
| --- | --- | --- | --- | --- | --- | --- |
<#list stockIndicator.candlesticks as candle>
| ${(candle.timestamp * 1000)?number_to_datetime?string("yyyy-MM-dd")} | ${candle.open} | ${candle.close} | ${candle.high} | ${candle.low} | ${candle.volume} | ${candle.turnover} |
</#list>
</#if>

<#if stockIndicatorDataFrame??>
## 技术指标
| <#list stockIndicatorDataFrame.columns as column>${column}<#if column_has_next> | </#if></#list> |
| <#list stockIndicatorDataFrame.columns as column>---<#if column_has_next> | </#if></#list> |
<#list stockIndicatorDataFrame.rows as row>
| <#list stockIndicatorDataFrame.columns as column>${row[column]!""}<#if column_has_next> | </#if></#list> |
</#list>
</#if>
