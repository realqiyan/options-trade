# 期权策略
<#if strategies??>
<#list strategies as strategy>
## ${strategy.title}

${strategy.content}

</#list>
<#else>
<#include "config_default_strategies.ftl" encoding="UTF-8" parse=true>
</#if>
