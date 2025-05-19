// JS

var currentOwnerData;

function filterStrategyById(jsonArray, strategyId) {
  return jsonArray.filter(item => item.strategyId === strategyId);
}

function sync(){
    $.ajax({
          url: "/trade/sync",
          method: 'GET',
          data: {
            password: $("#totp").val(),
            time: new Date().getTime()
          },
          success: function( result ) {
            layer.msg(result.success ? '同步成功' : result.message);
            loadStrategyOrder(currentStrategyId);
          }
        });
}

function renderTable(orderList){
    if(!orderList){
        return;
    }

    var convertedData = orderList.map(item => {
        return {
            "order": JSON.stringify(item),
            "id": item.id,
            "strategyId": item.strategyId,
            "platformOrderId": item.platformOrderId,
            "platformOrderIdEx": item.platformOrderIdEx,
            "platformFillId": item.platformFillId,
            "underlyingCode": item.underlyingCode,
            "code": item.code,
            "side": sideMapping(item.side),
            "price": item.price,
            "quantity": item.quantity,
            "tradeTime": item.tradeTime,
            "orderFee": item.orderFee,
            "accountId": item.accountId,
            "strikeTime": extractDate(item.strikeTime),
            "subOrder": item.subOrder,
            "statusStr": statusMapping(item.status+''),
        };
    });

    layui.use('table', function(){
      var table = layui.table;
      var inst = table.render({
        elem: '#result',
        cols: [[
          {type: 'checkbox', fixed: 'left'},
          {field: 'id', title: 'ID', width: 180},
          {field: 'strategyId', title: '策略ID', width: 180},
          {field: 'underlyingCode', title: '股票', width: 80, sort: true},
          {field: 'code', title: '证券代码', width: 180},
          {field: 'side', title: '类型', width: 80},
          {field: 'price', title: '价格', width: 85},
          {field: 'quantity', title: '数量', width: 80},
          {field: 'orderFee', title: '订单费用', width: 100},
          {field: 'strikeTime', title: '行权时间', width: 120},
          {field: 'tradeTime', title: '交易时间', width: 165},
          {field: 'statusStr', title: '状态', width: 100},
          {field: 'platformOrderId', title: '订单号', width: 180},
          {field: 'platformOrderIdEx', title: '订单号Ex', width: 200},
          {field: 'platformFillId', title: '成交单', width: 180},
          {field: 'subOrder', title: '是否子单', width: 100},
        ]],
        data: convertedData,
        toolbar: '#toolbar',
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
          field: 'tradeTime',
          type: 'desc'
        },
        page: false,
        limits: [100, 200, 500],
        limit: 100
      });

    // 工具栏事件
    table.on('toolbar(result)', function(obj){
      var id = obj.config.id;
      var checkStatus = table.checkStatus(id);
      var othis = lay(this);
      switch(obj.event){
        case 'update':
          var data = checkStatus.data;
          update(data);
          break;
      };
    });
    });

    render();

}

function query(){
    $.ajax({
     url: "/trade/order/draft",
     method: 'GET',
     data: {
         password: $("#totp").val(),
         time: new Date().getTime()
     },
     success: function(response) {
        layer.msg(response.success ? '查询成功' : response.message);
        renderTable(response.data);
     },
   });
}

function sync(){
    $.ajax({
          url: "/trade/sync",
          method: 'GET',
          data: {
            password: $("#totp").val(),
            time: new Date().getTime()
          },
          success: function( result ) {
            layer.msg(result.success ? '同步成功' : result.message);
            query();
          }
        });
}

function update(orders){
    var strategyId = $("#strategyId").val();

    if(!strategyId){
        layer.msg('请先选择策略！');
        return;
    }
    var selectStrategyArr = filterStrategyById(currentOwnerData.strategyList, strategyId);
    if(!selectStrategyArr || selectStrategyArr.length != 1){
        layer.msg('无法匹配策略！');
        return;
    }

    layer.confirm('确认关联策略：'+selectStrategyArr[0].strategyName, {
        btn: ['确定', '取消'] //按钮
      }, function(){
        // 确认
        console.log(strategyId);
        console.log(orders);

        var orderIds = orders.map(item => item.id);
        $.ajax({
          url: "/trade/update",
          method: 'POST',
          data: {
            password: $("#totp").val(),
            strategyId: strategyId,
            orderIds: orderIds,
          },
          success: function( response ) {
            if(response.success){
                layer.msg('执行完成:' + response.success);
                query();
            }else{
                layer.msg('执行失败:' + response.message);
            }
          }
        });




   }, function(){
        // 取消
        layer.msg('操作已经取消');
   });
}



function reloadData(){
    $.ajax({
      url: "/owner/get",
      data: {
        time: new Date().getTime()
      },
      success: function( response ) {
        currentOwnerData = response.data;
        $("#owner").val(currentOwnerData.owner);

        laytpl(currentStrategy.innerHTML).render({list:currentOwnerData.strategyList}, function(html){
          document.getElementById('strategyIdZone').innerHTML = html;
          var form = layui.form;
          form.render();
        });

        render();
        sync();
      }
    });
}

reloadData();
