# 用户持仓汇总

<#if positions?? && positions?size gt 0>
| 证券代码 | 证券名称 | 持仓数量 | 可卖数量 | 成本价 | 当前价 | 市场 |
|---------|---------|---------|---------|-------|-------|-------|
<#list positions as item>
| ${item.securityCode!} | ${item.securityName!} | ${item.quantity!} | ${item.canSellQty!} | ${item.costPrice!} | ${item.currentPrice!} | ${item.market!} |
</#list>
* 备注：期权持仓数为负数时，代表卖出期权合约。
<#else>
暂无持仓数据
</#if>

## 市场说明
* 1 - 香港市场 - 价格币种港币
* 11 - 美国市场 - 价格币种美元
