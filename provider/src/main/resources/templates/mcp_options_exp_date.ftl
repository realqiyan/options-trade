# 期权到期日查询结果

股票代码：${security.code}，市场：${security.market}

到期日列表：
<#list expDates as expDate>
- ${expDate.strikeTime} - (${expDate.tag})
</#list>
