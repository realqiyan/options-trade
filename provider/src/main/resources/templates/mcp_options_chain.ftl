# ${security.code}(${security.market}) ${strikeTime}期权链

## 标的行情
- **最新价**: ${stockIndicator.securityQuote.lastDone}
- **周波动区间**: ${stockIndicator.weekPriceRange}
- **月波动区间**: ${stockIndicator.monthPriceRange}

## 期权列表
<#list optionsList as option>
### ${option.basic.name}
- **标的代码**: ${option.basic.security.code}
- **类型**: <#if option.optionExData.type == 1>认购<#else>认沽</#if>
- **行权价**: ${option.optionExData.strikePrice}
- **最新价**: ${option.realtimeData.curPrice}
- **持仓量**: ${option.realtimeData.openInterest}
- **成交量**: ${option.realtimeData.volume}
- **隐含波动率**: ${option.realtimeData.impliedVolatility}%
- **Delta**: ${option.realtimeData.delta}
- **Gamma**: ${option.realtimeData.gamma}
- **Theta**: ${option.realtimeData.theta}
- **Vega**: ${option.realtimeData.vega}
- **Rho**: ${option.realtimeData.rho}
</#list>

## VIX指标
- **当前VIX值**: ${vixIndicator.currentVix.value} (${vixIndicator.currentVix.date})
- **日变动**: ${vixIndicator.vixDailyChange}
- **标普500**: ${vixIndicator.currentSp500.value} (${vixIndicator.currentSp500.date})

## 技术指标
<#list stockIndicator.indicatorMap?keys as key>
### ${key}
<#list stockIndicator.indicatorMap[key] as item>
- ${item.date}: ${item.value}
</#list>
</#list>
