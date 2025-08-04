# ${security.code}(${security.market}) 近${count}${periodName} ${periodName}K线数据

| 日期 | 开盘价 | 收盘价 | 最高价 | 最低价 | 成交量 | 成交额 |
| --- | --- | --- | --- | --- | --- | --- |
<#list candlesticks as candle>
| ${(candle.timestamp * 1000)?number_to_datetime?string("yyyy-MM-dd")} | ${candle.open} | ${candle.close} | ${candle.high} | ${candle.low} | ${candle.volume} | ${candle.turnover} |
</#list>
