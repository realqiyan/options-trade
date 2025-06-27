# ${security.code}(${security.market})技术指标和${strikeTime}期权链

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
| ${candlestick.timestamp?replace(",", "")?number?long?number_to_datetime?string("yyyy-MM-dd")} | ${candlestick.open} | ${candlestick.close} | ${candlestick.high} | ${candlestick.low} | ${candlestick.volume} | ${candlestick.turnover} |
</#list>
</#if>

## VIX指标
| 指标 | 值 |
| ---- | ---- |
| 当前VIX值 | ${vixIndicator.currentVix.value} (${vixIndicator.currentVix.date}) |
| 日变动 | ${vixIndicator.vixDailyChange} |
| 标普500 | ${vixIndicator.currentSp500.value} (${vixIndicator.currentSp500.date}) |

## 技术指标
<#list stockIndicator.indicatorMap?keys as key>
### ${key}
| 日期 | 值 |
| ---- | ---- |
<#list stockIndicator.indicatorMap[key] as item>
| ${item.date} | ${item.value} |
</#list>
</#list>

## ${strikeTime}期权列表
<#list optionsList as option>
### ${option.basic.name}
| 指标 | 值 |
| ---- | ---- |
| 标的代码 | ${option.basic.security.code} |
| 类型 | <#if option.optionExData.type == 1>认购<#else>认沽</#if> |
| 行权价 | ${option.optionExData.strikePrice} |
| 最新价 | ${option.realtimeData.curPrice} |
| 持仓量 | ${option.realtimeData.openInterest} |
| 成交量 | ${option.realtimeData.volume} |
| 隐含波动率 | ${option.realtimeData.impliedVolatility}% |
| Delta | ${option.realtimeData.delta} |
| Gamma | ${option.realtimeData.gamma} |
| Theta | ${option.realtimeData.theta} |
| Vega | ${option.realtimeData.vega} |
| Rho | ${option.realtimeData.rho} |
</#list>