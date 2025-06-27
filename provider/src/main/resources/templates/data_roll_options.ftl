<#if optionsRealtimeDataList?? && (optionsRealtimeDataList?size > 0)>
<options>

# Roll备选期权列表
| 期权代码 | 期权类型 | 行权价 | 当前价格 | Delta | Gamma | Theta | Vega | 隐含波动率 | 溢价 | 未平仓数量 | 成交量 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
<#list optionsRealtimeDataList as data>
<#if data?? && data.security??>
  <#assign optionType = "">
  <#if data.security.code?contains("C")>
    <#assign optionType = "Call">
  <#elseif data.security.code?contains("P")>
    <#assign optionType = "Put">
  </#if>
  <#assign strikePrice = "">
  <#assign code = data.security.code>
  <#assign cIndex = code?last_index_of("C")>
  <#assign pIndex = code?last_index_of("P")>
  <#assign strikeIndex = (cIndex > pIndex)?then(cIndex, pIndex)>
  <#if strikeIndex gt -1>
    <#assign strikePrice = code?substring(strikeIndex + 1)>
    <#assign strikePrice = (strikePrice?number / 1000)?string("0.##")>
  </#if>
| ${data.security.code} | ${optionType} | ${strikePrice} | ${data.curPrice!'-'} | ${data.delta!'-'} | ${data.gamma!'-'} | ${data.theta!'-'} | ${data.vega!'-'} | ${(data.impliedVolatility!'-')?string + '%'} | ${(data.premium!'-')?string + '%'} | ${data.openInterest!'-'} | ${data.volume!'-'} |
</#if>
</#list>

</options>
</#if>
