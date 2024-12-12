//JS
var $ = layui.$;
var element = layui.element;
var util = layui.util;

var currentCode;
var currentMarket;

function render(){
    layui.use(['element', 'layer', 'util'], function(){
      var element = layui.element;
      var layer = layui.layer;
      var util = layui.util;
      var $ = layui.$;
      element.render('nav');
    });
}


function loadOptionsExpDate(code, market){
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
      success: function( result ) {
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
      success: function( result ) {
        document.getElementById("title").innerHTML=currentCode + '(' + result.securityQuote.lastDone + ') - ' + result.strikeTime + '(' + optionExpiryDateDistance + ')';

        var convertedData = result.optionList.map(item => {
            return {
                "strikePrice": item.call?item.call.optionExData.strikePrice:item.put.optionExData.strikePrice,
                "call": item.call?JSON.stringify(item.call):null,
                "put": item.put?JSON.stringify(item.put):null,
                "putDelta": item.put && item.put.realtimeData?item.put.realtimeData.delta:'-',
                "callDelta": item.call && item.call.realtimeData?item.call.realtimeData.delta:'-',
                "putGamma": item.put && item.put.realtimeData?item.put.realtimeData.gamma:'-',
                "callGamma": item.call && item.call.realtimeData?item.call.realtimeData.gamma:'-',
                "putTheta": item.put && item.put.realtimeData?item.put.realtimeData.theta:'-',
                "callTheta": item.call && item.call.realtimeData?item.call.realtimeData.theta:'-',
                "putCurPrice": item.put && item.put.realtimeData?item.put.realtimeData.curPrice:'-',
                "callCurPrice": item.call && item.call.realtimeData?item.call.realtimeData.curPrice:'-',
                "putSellAnnualYield": item.put && item.put.strategyData?item.put.strategyData.sellAnnualYield + '%' : '-',
                "callSellAnnualYield": item.call && item.call.strategyData?item.call.strategyData.sellAnnualYield + '%' : '-',
            };
        });

        layui.use('table', function(){
          var table = layui.table;
          var inst = table.render({
            elem: '#result',
            cols: [[
              {field: 'callGamma', title: 'Gamma', width: 85},
              {field: 'callTheta', title: 'Theta', width: 85},
              {field: 'callDelta', title: 'Delta', width: 85},
              {field: 'callCurPrice', title: 'Price', width: 85},
              {field: 'call', title: '卖', width: 20, templet: '{{#  if(d.call){ }}<div><a class="layui-btn layui-btn-primary layui-btn-xs" onclick="sell({{= d.call }})" lay-event="sell">卖</a></div>{{#  } }}'},
              {field: 'callSellAnnualYield', title: '年化', width: 100},
              {field: 'strikePrice', title: '行权价', width: 100, sort: true},
              {field: 'putSellAnnualYield', title: '年化', width: 100},
              {field: 'put', title: '卖', width: 20, templet: '{{#  if(d.put){ }}<div><a class="layui-btn layui-btn-primary layui-btn-xs" onclick="sell({{= d.put }})" lay-event="sell">卖</a></div>{{#  } }}'},
              {field: 'putCurPrice', title: 'Price', width: 85},
              {field: 'putDelta', title: 'Delta', width: 85},
              {field: 'putTheta', title: 'Theta', width: 85},
              {field: 'putGamma', title: 'Gamma', width: 85}
            ]],
            data: convertedData,
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
function sell(options){
    layer.prompt({title: '请输入卖出份数', value: 1}, function(value, index, elem){
        if(value === ''){
            return elem.focus();
        }
        var quantity = util.escape(value);
        layer.close(index);
        layer.prompt({title: '请输入卖出价格', value: options.realtimeData.curPrice}, function(value, index, elem){
            if(value === ''){
                return elem.focus();
            }
            // 下单
            var price = util.escape(value);
            layer.msg('卖出价格:'+ price);
            $.ajax({
              url: "/options/sell",
              method: 'POST',
              data: {
                owner: $("#owner").val(),
                account: $("#account").val(),
                quantity: quantity,
                price: price,
                options: JSON.stringify(options),
              },
              success: function( result ) {
                $("#owner").val(result.owner);
                var output = document.getElementById("security");
                output.innerHTML = "";
                for(var i=0; i<result.securityList.length; i++) {
                    var obj = result.securityList[i];
                    output.innerHTML += '<li class="layui-nav-item" onclick="loadOptionsExpDate(\''+obj.code+'\',\''+obj.market+'\')"><a href="javascript:;">'+obj.code+'</a></li>'
                }

                render();
              }
            });
            layer.close(index);
        });
    });
}

function reloadData(){
    $.ajax({
      url: "/options/owner/get",
      data: {
        time: new Date().getTime()
      },
      success: function( result ) {
        $("#owner").val(result.owner);
        var accountOutput = document.getElementById("account");
        accountOutput.innerHTML = "";
        for(var i=0; i<result.accountList.length; i++) {
            var obj = result.accountList[i];
            accountOutput.innerHTML += '<option value=\''+JSON.stringify(obj)+'\'>'+obj.accountId+'</option>'
        }
        var output = document.getElementById("security");
        output.innerHTML = "";
        for(var i=0; i<result.securityList.length; i++) {
            var obj = result.securityList[i];
            output.innerHTML += '<li class="layui-nav-item" onclick="loadOptionsExpDate(\''+obj.code+'\',\''+obj.market+'\')"><a href="javascript:;">'+obj.code+'</a></li>'
        }
        render();
      }
    });
}

reloadData();
