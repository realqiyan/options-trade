<obs>
[${toolRequest.name}({<#if toolRequest.arguments?? && toolRequest.arguments?size gt 0><#list toolRequest.arguments?keys as key>"${key}"": "${toolRequest.arguments[key]}"<#if key_has_next>, </#if></#list></#if>})]:
${toolResult}

信息更新时间：${time}
</obs>

