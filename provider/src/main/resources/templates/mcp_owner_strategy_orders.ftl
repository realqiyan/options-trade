# ${strategySummary.strategy.strategyName!''} 策略详情

## 策略信息
- 策略ID: ${strategySummary.strategy.strategyId!''}
- 期权策略Code: ${ strategySummary.strategy.strategyCode!'' }
- 期权策略: ${strategySummary.getOptionsStrategy().getTitle() }
- 策略名称: ${strategySummary.strategy.strategyName!''}
- 等价持股数: ${strategySummary.strategyDelta!''}
- 策略delta（归一化）: ${strategySummary.avgDelta!''}
- 策略盈利: $${strategySummary.allIncome!''}
- 期权盈利: $${strategySummary.allOptionsIncome!''}
- 持有股票: ${strategySummary.holdStockNum!''}
- 当前股价: $${strategySummary.currentStockPrice!''}
- 股票支出: $${strategySummary.totalStockCost!''} (成本: $${strategySummary.averageStockCost!''})
- 持股盈亏: $${strategySummary.holdStockProfit!''}
- 期权已到账收入: $${strategySummary.allOptionsIncome!''} (已扣除手续费$${strategySummary.totalFee!''})
- 期权未到期收入: $${strategySummary.unrealizedOptionsIncome!''}
- PUT订单保证金占用: $${strategySummary.putMarginOccupied!''}

<#if orders?? && orders?size gt 0>
## 策略订单明细
| 标的代码 | 证券代码 | 类型 | 价格 | 数量 | 订单收益 | 订单费用 | 行权时间 | 交易时间 | 状态 | 订单号 | 是否平仓 |
|---------|---------|------|------|------|---------|---------|---------|---------|------|--------|---------|
<#list orders as order>
| ${order.underlyingCode!''} | ${order.code!''} | <#assign sideMap = {"1":"买", "2":"卖", "3":"卖空", "4":"买回"}>${sideMap[order.side?string]!"未知"} | ${order.price!''} | ${order.quantity!0} | ${order.ext.totalIncome!''} | ${order.orderFee!''} | ${(order.strikeTime?string('yyyy-MM-dd'))!''} | ${(order.tradeTime?string('yyyy-MM-dd HH:mm:ss'))!''} | <#assign statusMap = {"-1":"未知", "1":"待提交", "2":"提交中", "5":"已提交", "10":"部分成交", "11":"全部成交", "14":"部分撤单", "15":"已撤单", "21":"下单失败", "22":"已失效", "23":"已删除", "24":"成交撤销", "25":"提前指派"}>${statusMap[order.status?string]!"未知"} | ${order.platformOrderId!''} | ${order.ext.isClose!'false'} |
</#list>
<#else>
暂无订单
</#if>

<#if includeStrategyRule?? && includeStrategyRule>
## ${strategySummary.getOptionsStrategy().getTitle() }规则

${strategySummary.getOptionsStrategy().getContent()}
</#if>
