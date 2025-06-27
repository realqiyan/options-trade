<options>

# 交易期权标的
| 代码 | 期权类型 | 行权价 | 当前价格 | 隐含波动率 | Delta | Theta | Gamma | 未平仓合约数 | 当天交易量 | 预估年化收益率 | 距离行权价涨跌幅 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
<#list optionsList as option>
| ${option.basic.security.code} | ${(option.optionExData.type==1)?string("Call","Put")} | ${option.optionExData.strikePrice} | ${option.realtimeData.curPrice} | ${option.realtimeData.impliedVolatility} | ${option.realtimeData.delta} | ${option.realtimeData.theta} | ${option.realtimeData.gamma} | ${option.realtimeData.openInterest} | ${option.realtimeData.volume} | ${option.strategyData.sellAnnualYield}% | ${option.strategyData.range}% |
</#list>

</options>
