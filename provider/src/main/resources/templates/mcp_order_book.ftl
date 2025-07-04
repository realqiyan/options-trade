# 期权报价

期权代码：${security.code}

买盘：
<#list orderBook.bidList as bid>
- 价格：${bid}
</#list>

卖盘：
<#list orderBook.askList as ask>
- 价格：${ask}
</#list>
