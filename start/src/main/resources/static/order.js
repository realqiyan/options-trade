// JS
var currentStrategyId;;

//owner: $("#owner").val(),
function tradeModify(action, order){
    layer.prompt({title: '确认操作', value: action}, function(value, index, elem){
        if(value === ''){
            return elem.focus();
        }
        $.ajax({
              url: "/trade/modify",
              method: 'POST',
              data: {
                password: $("#totp").val(),
                action: action,
                order: order,
              },
              success: function( response ) {
                layer.msg('执行完成 result:'+ JSON.stringify(response));
                loadStrategyOrder(currentStrategyId);
              }
            });
        layer.close(index);
    });
}

//owner: $("#owner").val(),
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

function cancel(order){
    tradeModify('cancel', order);
}

function deleteOrder(order){
    tradeModify('delete', order);
}

function tradeClose(order, orderBook){
    var closePrice = order.price * 0.2;
    layer.prompt({title: '请输入买入价格（ask:'+orderBook.askList+' bid:'+orderBook.bidList+'）',value:closePrice}, function(value, index, elem){
        if(value === ''){
            return elem.focus();
        }
        // 下单
        var price = util.escape(value);
        $.ajax({
          url: "/trade/close",
          method: 'POST',
          data: {
            password: $("#totp").val(),
            owner: order.owner,
            price: price,
            order: JSON.stringify(order),
          },
          success: function( response ) {
            var result = response.data;
            layer.msg('交易完成 result:'+ result.platformOrderId);
            loadStrategyOrder(currentStrategyId);
          }
        });
        layer.close(index);
    });
}

function closePosition(order){
    var orderObj = JSON.parse(order);
    $.ajax({
         url: "/options/orderbook/get",
         method: 'GET',
         data: {
           code: orderObj.code,
           market: orderObj.market,
           time: new Date().getTime()
         },
         success: function( response ) {
            var result = response.data;
            tradeClose(orderObj, result);
         }
       });
}

function sideMapping(side){
    const sideMap = {
        '1': '买',
        '2': '卖',
        '3': '卖空',
        '4': '买回',
    };
    return sideMap[side] || '未知';
}

function statusMapping(status) {
    const statusMap = {
        '-1': '未知', // 未知状态
        '1': '待提交', // 等待提交
        '2': '提交中', // 提交中
        '5': '已提交', // 已提交，等待成交
        '10': '部分成交', // 部分成交
        '11': '全部成交', // 全部已成
        '14': '部分撤单', // 部分成交，剩余部分已撤单
        '15': '已撤单', // 全部已撤单，无成交
        '21': '下单失败', // 下单失败，服务拒绝
        '22': '已失效', // 已失效
        '23': '已删除', // 已删除，无成交的订单才能删除
        '24': '成交撤销' // 成交被撤销
    };
    return statusMap[status] || '未知';
}

function extractDate(dateString) {
    // 使用 split 方法分割字符串"2025-02-21 00:00:00"
    const parts = dateString.split(' ')[0];
    return parts;
}

function renderTable(orderList){
    if(!orderList){
        return;
    }

    var convertedData = orderList.map(item => {
        return {
            "LAY_CHECKED": item.ext && "true" == item.ext.isClose ? false : true,
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
            "status": item.status,
            "statusStr": statusMapping(item.status+''),
            "curDTE": item.ext ? item.ext.curDTE : null ,
            "isClose": item.ext ? item.ext.isClose : null,
            "totalIncome": item.ext ? item.ext.totalIncome : null,
            "curPrice": item.ext ? item.ext.curPrice : null,
            "profitRatio": item.ext && item.ext.profitRatio ? item.ext.profitRatio + '%' : null,
        };
    });

    layui.use('table', function(){
      var table = layui.table;
      var inst = table.render({
        elem: '#result',
        cols: [[
          {field: 'curDTE', title: '关注', width: 50, align: 'center', templet: '#TPL-colorStatus'},
          //{field: 'strategyId', title: '策略ID', width: 280, sort: true},
          //{field: 'underlyingCode', title: '股票', width: 80},
          {field: 'code', title: '证券代码', width: 180},
          {field: 'side', title: '类型', width: 80},
          {field: 'price', title: '价格', width: 85},
          {field: 'quantity', title: '数量', width: 80},
          {field: 'totalIncome', title: '收入', width: 100},
          {field: 'orderFee', title: '订单费用', width: 100},
          {field: 'strikeTime', title: '行权时间', width: 120, sort: true},
          {field: 'tradeTime', title: '交易时间', width: 165, sort: true},
          {field: 'statusStr', title: '状态', width: 100},
          {field: 'platformOrderId', title: '订单号', width: 180},
          {field: 'platformOrderIdEx', title: '订单号Ex', width: 200},
          {field: 'platformFillId', title: '成交单', width: 180},
          {field: 'subOrder', title: '是否子单', width: 100},
          {field: 'isClose', title: '是否平仓', width: 100},
          {field: 'curPrice', title: '现价', width: 80},
          {field: 'profitRatio', title: '盈亏', width: 100},
          {field: 'order', title: '操作', width: 150, templet: '<div>{{# if("true" != d.isClose){ }}'+
          '{{# if(["-1","1","2","5"].includes(d.status) ){ }}<a class="layui-btn layui-btn-primary layui-btn-xs" onclick="cancel(\'{{= d.order}}\')" lay-event="cancel">取消</a>{{#  } }}'+
          '{{# if(["11"].includes(d.status) ){ }}<a class="layui-btn layui-btn-primary layui-btn-xs" onclick="closePosition(\'{{= d.order}}\')" lay-event="closePosition">平仓</a>{{#  } }}'+
          '{{# } }}'+
          '{{# if(["15"].includes(d.status) ){ }}<a class="layui-btn layui-btn-primary layui-btn-xs" onclick="deleteOrder(\'{{= d.order}}\')" lay-event="delete">删除</a>{{#  } }}'+
          '</div>'},
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
          field: 'tradeTime',
          type: 'desc'
        },
        page: false,
        limits: [100, 200, 500],
        limit: 100
      });
    });

    render();

}

function loadStrategyOrder(strategyId){
    currentStrategyId = strategyId;
    $.ajax({
          url: "/options/strategy/get",
          data: {
            strategyId: strategyId
          },
          success: function( response ) {
            var result = response.data;
            var getTpl = summary.innerHTML;
            var view = document.getElementById('title');
            laytpl(getTpl).render(result, function(html){
              view.innerHTML = html;
            });
            renderTable(result.strategyOrders);
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
        var result = response.data;
        var output = document.getElementById("security");
        output.innerHTML = "";
        for(var i=0; i<result.strategyList.length; i++) {
            var obj = result.strategyList[i];
            if(i == 0){
               currentStrategyId = obj.strategyId
            }
            //<dd><a href="javascript:;">loading...</a></dd>
            output.innerHTML += '<dd onclick="loadStrategyOrder(\''+obj.strategyId+'\')"><a href="javascript:;">'+obj.strategyName+'</a></dd>';
        }
        render();
        loadStrategyOrder(currentStrategyId);
      }
    });
}

reloadData();
