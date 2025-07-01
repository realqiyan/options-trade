# ${security.code}(${security.market})到期日为${strikeTime}的期权链

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