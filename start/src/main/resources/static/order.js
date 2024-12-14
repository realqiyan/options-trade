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

        var convertedData = orderList.map(item => {
                    return {
                        "itemObj": item,
                        "code": item.security.code,
                        "side": item.side,
                        "price": item.price,
                        "quantity": item.quantity,
                        "tradeTime": item.tradeTime,
                        "platform": item.platform,
                        "accountId": item.account.accountId,
                        "status": item.status,
                    };
                });

                layui.use('table', function(){
                  var table = layui.table;
                  var inst = table.render({
                    elem: '#result',
                    cols: [[
                      {field: 'accountId', title: 'AccountId', width: 200},
                      {field: 'code', title: 'Code', width: 200},
                      {field: 'side', title: 'Side', width: 120},
                      {field: 'price', title: 'Price', width: 120},
                      {field: 'quantity', title: 'Quantity', width: 120},
                      {field: 'tradeTime', title: 'TradeTime', width: 200},
                      {field: 'platform', title: 'Platform', width: 120},
                      {field: 'status', title: 'Status', width: 120},
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
