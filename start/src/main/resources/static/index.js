//JS
var currentStrategyId;
var currentCode;
var currentMarket;

function loadOptionsExpDate(strategyId, code, market){
    currentStrategyId = strategyId;
    currentCode = code;
    currentMarket = market;
    console.log('loadOptionsExpDate code:'+ code+' market:'+ market);
    $.ajax({
      url: "/options/strike/list",
      data: {
        code: code,
        market: market,
        time: new Date().getTime()
      },
      success: function( response ) {
        var result = response.data;
        var output = document.getElementById("strike-list");
        output.innerHTML = "";
        var moreHtml = ""
        for(var i=0; i<result.length; i++) {
            var obj = result[i];
            if (i < 8){
                output.innerHTML += '<li class="layui-nav-item layui-hide-xs" onclick="loadOptionsChain(\''+obj.strikeTime+'\',\''+obj.strikeTimestamp+'\',\''+obj.optionExpiryDateDistance+'\')"><a href="javascript:;">'+obj.strikeTime+'('+obj.optionExpiryDateDistance+')</a></li>'
            }else{
                moreHtml += '<dd onclick="loadOptionsChain(\''+obj.strikeTime+'\',\''+obj.strikeTimestamp+'\',\''+obj.optionExpiryDateDistance+'\')"><a href="javascript:;">'+obj.strikeTime+'('+obj.optionExpiryDateDistance+')</a></dd>'
            }
        }
        output.innerHTML += '<li class="layui-nav-item"><a href="javascript:;">More</a><dl class="layui-nav-child">' + moreHtml + '</dl></li>';
        render();
      }
    });
}
// /options/chain/get?code=BABA&market=11&time=1733652854662&strikeTime=2024-12-13&strikeTimestamp=1734066000&optionExpiryDateDistance=5
function loadOptionsChain(strikeTime, strikeTimestamp, optionExpiryDateDistance){
    console.log('loadOptionsChain strikeTime:'+ strikeTime+' optionExpiryDateDistance:'+ optionExpiryDateDistance);
    document.getElementById("title").innerHTML = "loading...";
    $.ajax({
      url: "/options/chain/get",
      data: {
        code: currentCode,
        market: currentMarket,
        strikeTime: strikeTime,
        strikeTimestamp: strikeTimestamp,
        optionExpiryDateDistance: optionExpiryDateDistance,
        time: new Date().getTime()
      },
      success: function( response ) {
        var result = response.data;
        document.getElementById("title").innerHTML=currentCode + '(' + result.securityQuote.lastDone + ') - ' + result.strikeTime + '(' + optionExpiryDateDistance + ')';
        // document.getElementById("vix").innerHTML= 'VIX(' + result.vixQuote.lastDone + ')';

        var convertedData = result.optionList.map(item => {
            return {
                "LAY_CHECKED": item.call&&item.call.strategyData?item.call.strategyData.recommend:(item.put&&item.put.strategyData?item.put.strategyData.recommend:false),
                "callObj": item.call,
                "putObj": item.put,
                "group": item.call?item.call.basic.name.match(/^([^ ]+)/)[1]:item.put.basic.name.match(/^([^ ]+)/)[1],
                "strikePrice": item.call?item.call.optionExData.strikePrice:item.put.optionExData.strikePrice,
                "call": item.call?JSON.stringify(item.call):null,
                "put": item.put?JSON.stringify(item.put):null,
                "putCurPrice": item.put && item.put.realtimeData?item.put.realtimeData.curPrice:'-',
                "callCurPrice": item.call && item.call.realtimeData?item.call.realtimeData.curPrice:'-',
                "putSellAnnualYield": item.put && item.put.strategyData?item.put.strategyData.sellAnnualYield + '%' : '-',
                "callSellAnnualYield": item.call && item.call.strategyData?item.call.strategyData.sellAnnualYield + '%' : '-',
                "putRange": item.put && item.put.strategyData?item.put.strategyData.range + '%' : '-',
                "callRange": item.call && item.call.strategyData?item.call.strategyData.range + '%' : '-',
                "putSellRecommend": item.put && item.put.strategyData?item.put.strategyData.recommend : false,
                "callSellRecommend": item.call && item.call.strategyData?item.call.strategyData.recommend : false,
            };
        });

        layui.use('table', function(){
          var table = layui.table;
          var inst = table.render({
            elem: '#result',
            cols: [[
              {field: 'callRange', title: '涨跌幅', width: 85},
              {title: '交易参考信息', width: 350, rowspan: 3, templet: '#id-table-call-info'},
              {field: 'callCurPrice', title: '价格', width: 85},
              {field: 'call', title: '卖', width: 20, templet: '{{#  if(d.call){ }}<div><a title="{{= d.callObj.basic.name }}" class="layui-btn layui-btn-primary layui-btn-xs" onclick="sell({{= d.call }})" lay-event="sell">卖</a></div>{{#  } }}'},
              {field: 'callSellAnnualYield', title: '年化', width: 85},
              {field: 'strikePrice', title: '行权价', width: 90, sort: true},
              {field: 'putSellAnnualYield', title: '年化', width: 85},
              {field: 'put', title: '卖', width: 20, templet: '{{#  if(d.put){ }}<div><a title="{{= d.putObj.basic.name }}" class="layui-btn layui-btn-primary layui-btn-xs" onclick="sell({{= d.put }})" lay-event="sell">卖</a></div>{{#  } }}'},
              {field: 'putCurPrice', title: '价格', width: 85},
              {title: '交易参考信息', width: 350, templet: '#id-table-put-info'},
              {field: 'group', title: 'Group', width: 80},
            ]],
            data: convertedData,
            toolbar: true,
            height: 'full-260',
            lineStyle: 'height: 100%;',
            defaultToolbar: [
              'filter', // 列筛选
              'exports', // 导出
              'print' // 打印
            ],
            //skin: 'line',
            //even: true,
            initSort: {
              field: 'strikePrice',
              type: 'asc'
            },
            page: false,
            limits: [100, 200, 500],
            limit: 100
          });
        });

      }
    });
}

//owner: $("#owner").val(),
function trade(side, options, orderBook){
    layer.prompt({title: '请输入卖出份数', value: 1}, function(value, index, elem){
        if(value === ''){
            return elem.focus();
        }
        var quantity = util.escape(value);
        layer.close(index);
        layer.prompt({title: '请输入卖出价格（ask:'+orderBook.askList+' bid:'+orderBook.bidList+'）', value: options.realtimeData.curPrice}, function(value, index, elem){
            if(value === ''){
                return elem.focus();
            }
            // 下单
            var price = util.escape(value);
            $.ajax({
              url: "/trade/submit",
              method: 'POST',
              data: {
                owner: $("#owner").val(),
                password: $("#password").val(),
                side: side,
                strategyId: currentStrategyId,
                quantity: quantity,
                price: price,
                options: JSON.stringify(options),
              },
              success: function( response ) {
                layer.msg('交易完成 result:'+ JSON.stringify(response));
              }
            });
            layer.close(index);
        });
    });
}

function sell(options){
    $.ajax({
         url: "/options/orderbook/get",
         method: 'GET',
         data: {
           code: options.basic.security.code,
           market: options.basic.security.market,
           time: new Date().getTime()
         },
         success: function( response ) {
            trade(2, options, response.data);
         }
       });
}

function reloadData(){
    $.ajax({
      url: "/options/owner/get",
      data: {
        time: new Date().getTime()
      },
      success: function( response ) {
        var data = response.data;
        $("#owner").val(data.owner);

        var output = document.getElementById("security");
        output.innerHTML = "";
        for(var i=0; i<data.strategyList.length; i++) {
            var obj = data.strategyList[i];
            //<dd><a href="javascript:;">loading...</a></dd>
            output.innerHTML += '<dd onclick="loadOptionsExpDate(\''+obj.strategyId+'\',\''+obj.code+'\',\''+obj.market+'\')"><a href="javascript:;">'+obj.strategyName+'</a></dd>'
        }
        render();
      }
    });
}

reloadData();
