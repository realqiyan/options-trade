# 用户策略汇总

<#if ownerSummary.strategySummaries?? && ownerSummary.strategySummaries?size gt 0>
## 策略明细
<#list ownerSummary.strategySummaries as item>
### ${ item.strategy.strategyName }
- 策略ID: ${ item.strategy.strategyId } (${ item.strategy.strategyCode })
- 策略Delta: ${ item.strategyDelta }(看多比例:${ item.avgDelta })
- 策略盈利: $${ item.allIncome }
- 期权盈利: $${ item.allOptionsIncome }
- 持有股票 : ${ item.holdStockNum }
- 当前股价: $${ item.currentStockPrice }
- 股票支出: $${ item.totalStockCost } (成本: $${ item.averageStockCost })
- 持股盈亏: $${ item.holdStockProfit }
- 希腊字母: Delta:${ item.optionsDelta }｜Gamma:${ item.optionsGamma }｜Theta:${ item.optionsTheta }
- 当前股价: $${ item.currentStockPrice }
- 期权已到账收入: $${ item.allOptionsIncome } (已扣除手续费$${ item.totalFee })
- 期权未到期收入: $${ item.unrealizedOptionsIncome }
- 策略持有股票: ${ item.holdStockNum } 股
- 策略股票支出: $${ item.totalStockCost }
- 平均成本: $${ item.averageStockCost }
- 持股盈亏: $${ item.holdStockProfit }
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
