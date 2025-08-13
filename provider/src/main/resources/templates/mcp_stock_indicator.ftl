# ${security.code}(${security.market}) 近${count}${periodName}技术指标

## 标的行情

| 指标 | 值 |
| ---- | ---- |
| 最新价 | ${(stockIndicator.securityQuote.lastDone)!} |

## 技术指标

| <#list stockIndicatorDataFrame.columns as column>${column}<#if column_has_next> | </#if></#list> |
| <#list stockIndicatorDataFrame.columns as column>---<#if column_has_next> | </#if></#list> |
<#list stockIndicatorDataFrame.rows as row>
| <#list stockIndicatorDataFrame.columns as column>${row[column]!""}<#if column_has_next> | </#if></#list> |
</#list>
