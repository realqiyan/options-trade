# 财报日历查询结果-股票代码：${symbol}

财报日历列表：
<#if earningsCalendars?has_content>
<#list earningsCalendars as calendar>
- 公司名称：${calendar.name!"未知"}
  财报日期：${calendar.earningsDate?string("yyyy-MM-dd")}
  财报时间：${calendar.time!"未知"}
  预期每股收益：${calendar.epsForecast!"未知"}
  去年每股收益：${calendar.lastYearEps!"未知"}
</#list>
<#else>
暂无财报日历信息
</#if>