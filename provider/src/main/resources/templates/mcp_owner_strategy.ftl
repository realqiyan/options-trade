# 用户策略汇总

<#if ownerSummary.strategySummaries?? && ownerSummary.strategySummaries?size gt 0>
## 期权策略明细
<#list ownerSummary.strategySummaries as item>
### ${ item.strategy.strategyName }
- 策略ID: ${ item.strategy.strategyId }
- 策略代码: ${ item.strategy.strategyCode }
- 策略标的: ${ item.strategy.code }
- 当前股价: $${ item.currentStockPrice }

</#list>
</#if>

## 策略未平仓订单
<#if ownerSummary.unrealizedOrders?? && ownerSummary.unrealizedOrders?size gt 0>
| 策略ID | 订单ID | 底层标的 | 期权代码 | 方向 | 数量 | 行权价 | 价格 |
|-------|--------|--------|---------|------|----|-------|-------|
<#list ownerSummary.unrealizedOrders as order>
| ${order.strategyId!''} | ${order.platformOrderId!''} | ${order.underlyingCode!''} | ${order.code!''} | ${order.side!''} | ${order.quantity!0} | ${order.ext.strikePrice!0} | ${order.price!''} |
</#list>
<#else>
暂无未平仓订单
</#if>
