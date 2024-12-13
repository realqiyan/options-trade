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
                    };
                });

                layui.use('table', function(){
                  var table = layui.table;
                  var inst = table.render({
                    elem: '#result',
                    cols: [[
                      {field: 'code', title: 'Code', width: 200},
                      {field: 'side', title: 'Side', width: 120},
                      {field: 'price', title: 'Price', width: 120},
                      {field: 'quantity', title: 'Quantity', width: 120},
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
