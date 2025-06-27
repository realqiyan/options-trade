<indicators>

<#if dataFrame??>
# 近${period}${periodName}${securityQuote.security.toString()}技术指标
| <#list dataFrame.columns as column>${column}<#if column_has_next> | </#if></#list> |
| <#list dataFrame.columns as column>---<#if column_has_next> | </#if></#list> |
<#list dataFrame.rows as row>
| <#list dataFrame.columns as column>${row[column]!""}<#if column_has_next> | </#if></#list> |
</#list>
</#if>

</indicators>
