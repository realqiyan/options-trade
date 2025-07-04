# ${security.code}(${security.market})技术指标

## 标的行情
| 指标 | 值 |
| ---- | ---- |
| 最新价 | ${stockIndicator.securityQuote.lastDone} |
| 周波动区间 | ${stockIndicator.weekPriceRange} |
| 月波动区间 | ${stockIndicator.monthPriceRange} |

## K线数据
<#if stockIndicator.candlesticks?? && stockIndicator.candlesticks?size gt 0>
### 近期K线
| 日期 | 开盘 | 收盘 | 最高 | 最低 | 成交量 | 成交额 |
| ---- | ---- | ---- | ---- | ---- | ---- | ---- |
<#list stockIndicator.candlesticks as candlestick>
| ${(candlestick.timestamp?replace(",", "")?number?long * 1000)?number_to_datetime?string("yyyy-MM-dd")} | ${candlestick.open} | ${candlestick.close} | ${candlestick.high} | ${candlestick.low} | ${candlestick.volume} | ${candlestick.turnover} |
</#list>
</#if>

## 技术指标
<#list stockIndicator.indicatorMap?keys as key>
### ${key}
| 日期 | 值 |
| ---- | ---- |
<#list stockIndicator.indicatorMap[key] as item>
| ${item.date} | ${item.value} |
</#list>
</#list>
