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
                action: action,
                order: order,
              },
              success: function( result ) {
                layer.msg('执行完成 result:'+ JSON.stringify(result));
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
            time: new Date().getTime()
          },
          success: function( result ) {
            layer.msg('同步完成！');
            reloadData();
          }
        });
}


function cancel(order){
    tradeModify('cancel', order);
}

function tradeClose(side, order, orderBook){
    layer.prompt({title: '请输入买入份数', value: order.quantity}, function(value, index, elem){
        if(value === ''){
            return elem.focus();
        }
        var quantity = util.escape(value);
        layer.close(index);
        layer.prompt({title: '请输入买入价格（ask:'+orderBook.askList+' bid:'+orderBook.bidList+'）'}, function(value, index, elem){
            if(value === ''){
                return elem.focus();
            }
            // 下单
            var price = util.escape(value);
            layer.msg('卖出价格:'+ price);
            $.ajax({
              url: "/trade/close",
              method: 'POST',
              data: {
                owner: order.owner,
                side: side,
                strategyId: order.strategyId,
                quantity: quantity,
                price: price,
                order: JSON.stringify(order),
              },
              success: function( result ) {
                layer.msg('交易完成 result:'+ result.platformOrderId);
              }
            });
            layer.close(index);
        });
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
         success: function( result ) {
            tradeClose(1, orderObj, result);
         }
       });
}

function sideMapping(side){
    const sideMap = {
        '1': '买',
        '2': '卖',
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
                        "order": JSON.stringify(item),
                        "id": item.id,
                        "strategyId": item.strategyId,
                        "platformOrderId": item.platformOrderId,
                        "underlyingCode": item.underlyingCode,
                        "code": item.code,
                        "side": sideMapping(item.side),
                        "price": item.price,
                        "quantity": item.quantity,
                        "tradeTime": item.tradeTime,
                        "platform": item.platform,
                        "accountId": item.accountId,
                        "strikeTime": item.strikeTime,
                        "status": item.status,
                        "statusStr": statusMapping(item.status+''),
                        "curPrice": item.ext ? item.ext.curPrice : null,
                        "profitRatio": item.ext ? item.ext.profitRatio + '%' : null,
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
                      {field: 'platformOrderId', title: '订单号', width: 180},
                      {field: 'underlyingCode', title: '股票', width: 80},
                      {field: 'code', title: '期权', width: 180},
                      {field: 'side', title: '买卖', width: 80},
                      {field: 'price', title: '价格', width: 80},
                      {field: 'quantity', title: '数量', width: 80},
                      {field: 'tradeTime', title: '交易时间', width: 160},
                      {field: 'strikeTime', title: '行权时间', width: 160},
                      {field: 'platform', title: '平台', width: 80},
                      {field: 'statusStr', title: '状态', width: 100},
                      {field: 'curPrice', title: '现价', width: 80},
                      {field: 'profitRatio', title: '盈亏', width: 80},
                      {field: 'order', title: '操作', width: 200, templet: '<div>'+
                      '{{# if(["-1","1","2","5"].includes(d.status) ){ }}<a class="layui-btn layui-btn-primary layui-btn-xs" onclick="cancel(\'{{= d.order}}\')" lay-event="cancel">取消</a>{{#  } }}'+
                      '{{# if(["11"].includes(d.status) ){ }}<a class="layui-btn layui-btn-primary layui-btn-xs" onclick="closePosition(\'{{= d.order}}\')" lay-event="closePosition">平仓</a>{{#  } }}'+
                      '</div>'},
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
