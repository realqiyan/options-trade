//JS
var $ = layui.$;
var element = layui.element;
var util = layui.util;

function render(){
    layui.use(['element', 'layer', 'util'], function(){
      var element = layui.element;
      var layer = layui.layer;
      var util = layui.util;
      var $ = layui.$;
      element.render('nav');
    });
}

function reloadData(){
    $.ajax({
      url: "/options/owner/get",
      data: {
        time: new Date().getTime()
      },
      success: function( result ) {

        var orderList = result.orderList;
        if(!orderList){
            return;
        }

        var convertedData = orderList.map(item => {
                    return {
                        "itemObj": item,
                        "id": item.id,
                        "strategyId": item.strategyId,
                        "underlyingCode": item.underlyingCode,
                        "code": item.code,
                        "side": item.side,
                        "price": item.price,
                        "quantity": item.quantity,
                        "tradeTime": item.tradeTime,
                        "platform": item.platform,
                        "accountId": item.accountId,
                        "strikeTime": item.strikeTime,
                        "status": item.status,
                    };
                });

                layui.use('table', function(){
                  var table = layui.table;
                  var inst = table.render({
                    elem: '#result',
                    cols: [[
                      {field: 'strategyId', title: '策略ID', width: 280, sort: true},
                      {field: 'id', title: 'ID', width: 120},
                      {field: 'accountId', title: '账号', width: 180},
                      {field: 'underlyingCode', title: '股票', width: 80},
                      {field: 'code', title: '期权', width: 180},
                      {field: 'side', title: '买卖', width: 80},
                      {field: 'price', title: '价格', width: 80},
                      {field: 'quantity', title: '数量', width: 80},
                      {field: 'tradeTime', title: '交易时间', width: 160},
                      {field: 'strikeTime', title: '行权时间', width: 160},
                      {field: 'platform', title: '平台', width: 80},
                      {field: 'status', title: '状态', width: 80},
                    ]],
                    data: convertedData,
                    //skin: 'line',
                    //even: true,
                    initSort: {
                      field: 'code',
                      type: 'asc'
                    },
                    page: false,
                    limits: [100, 200, 500],
                    limit: 100
                  });
                });

        render();
      }
    });
}

reloadData();
