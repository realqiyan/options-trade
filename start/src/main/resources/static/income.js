// JS

function renderOrderTable(orderList){
    if(!orderList){
        return;
    }

    var convertedData = orderList.map(item => {
        return {
            "strikeTime": extractDate(item.strikeTime),
            "code": item.code,
            "side": sideMapping(item.side),
            "price": item.price,
            "quantity": item.quantity,
            "totalIncome": item.ext ? item.ext.totalIncome : null,
            "orderFee": item.orderFee,
            "tradeTime": item.tradeTime,
            "curPrice": item.ext ? item.ext.curPrice : null,
            "profitRatio": item.ext && item.ext.profitRatio ? item.ext.profitRatio + '%' : null,
        };
    });

    layui.use('table', function(){
      var table = layui.table;
      var inst = table.render({
        elem: '#orderTable',
        cols: [[
          {field: 'strikeTime', title: '行权时间', width: 130, sort: true},
          {field: 'tradeTime', title: '交易时间', width: 180, sort: true},
          {field: 'code', title: '证券代码', width: 180, sort: true},
          {field: 'side', title: '类型', width: 80},
          {field: 'quantity', title: '数量', width: 80},
          {field: 'price', title: '价格', width: 100},
          {field: 'totalIncome', title: '收入', width: 100},
          {field: 'curPrice', title: '现价', width: 100},
          {field: 'orderFee', title: '订单费用', width: 100},
          {field: 'profitRatio', title: '盈亏', width: 100},
        ]],
        data: convertedData,
        toolbar: true,
        // height: 'full-390',
        lineStyle: 'height: 100%;',
        defaultToolbar: [
          'filter', // 列筛选
          'exports', // 导出
          'print' // 打印
        ],
        //skin: 'line',
        //even: true,
        initSort: {
          field: 'strikeTime',
          type: 'asc'
        },
        page: false,
        limits: [100, 200, 500],
        limit: 100
      });
    });

    render();

}


function reloadData(){
    $.ajax({
      url: "/owner/summary",
      data: {
        time: new Date().getTime()
      },
      success: function( response ) {
        var result = response.data;

        var getTpl = summary.innerHTML;
        var view = document.getElementById('result');
        laytpl(getTpl).render(result, function(html){
          view.innerHTML = html;
        });
        renderOrderTable(result.unrealizedOrders);
        render();
      }
    });
}

reloadData();
