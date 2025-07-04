# ${security.code}(${security.market})到期日为${strikeTime}的期权链

| 代码 | 期权类型 | 行权价 | 当前价格 | 隐含波动率 | Delta | Theta | Gamma | 未平仓合约数 | 当天交易量 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
<#list optionsList as option>
| ${option.basic.security.code} | ${(option.optionExData.type==1)?string("Call","Put")} | ${option.optionExData.strikePrice} | ${option.realtimeData.curPrice} | ${option.realtimeData.impliedVolatility} | ${option.realtimeData.delta} | ${option.realtimeData.theta} | ${option.realtimeData.gamma} | ${option.realtimeData.openInterest} | ${option.realtimeData.volume} |
</#list>
