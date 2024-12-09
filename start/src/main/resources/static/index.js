var $ = layui.$;
var currentCode;
var currentMarket;
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

        for(var i=0; i<result.length; i++) {
            var obj = result[i];
            output.innerHTML += '<li class="layui-nav-item layui-hide-xs" onclick="loadOptionsChain(\''+obj.strikeTime+'\',\''+obj.strikeTimestamp+'\',\''+obj.optionExpiryDateDistance+'\')"><a href="javascript:;">'+obj.strikeTime+'('+obj.optionExpiryDateDistance+')</a></li>'
        }
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
        document.getElementById("title").innerHTML=result.strikeTime;

        var convertedData = result.optionList.map(item => {
            return {
                "strikePrice": item.call.optionExData.strikePrice,
                "callDelta": item.call.realtimeData?item.call.realtimeData.delta:0,
                "callCurPrice": item.call.realtimeData?item.call.realtimeData.curPrice:0,
                "putDelta": item.put.realtimeData?item.put.realtimeData.delta:0,
                "putCurPrice": item.put.realtimeData?item.put.realtimeData.curPrice:0,
            };
        });

        layui.use('table', function(){
          var table = layui.table;
          var inst = table.render({
            elem: '#result',
            cols: [[
              {field: 'callDelta', title: 'CallDelta', width: 120},
              {field: 'callCurPrice', title: 'CallPrice', width: 120},
              {field: 'strikePrice', title: '行权价', width: 120, sort: true},
              {field: 'putCurPrice', title: 'PutPrice', width: 120},
              {field: 'putDelta', title: 'PutDelta', width: 120}
            ]],
            data: convertedData,
            //skin: 'line',
            //even: true,
            page: false,
            limits: [100, 200, 500],
            limit: 100
          });
        });

      }
    });
}

function reloadData(){
    $.ajax({
      url: "/underlying/list",
      data: {
        owner: $("#owner").val(),
        time: new Date().getTime()
      },
      success: function( result ) {
        var output = document.getElementById("underlying");
        output.innerHTML = "";

        for(var i=0; i<result.length; i++) {
            var obj = result[i];
            output.innerHTML += '<li class="layui-nav-item" onclick="loadOptionsExpDate(\''+obj.code+'\',\''+obj.market+'\')"><a href="javascript:;">'+obj.code+'</a></li>'
        }
      }
    });
}

reloadData();