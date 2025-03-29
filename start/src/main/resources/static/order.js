// JS
var currentStrategyId;
var assistantWindow;

//owner: $("#owner").val(),
function tradeModify(action, orderId){
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
                orderId: orderId,
              },
              success: function( response ) {
                layer.msg(response.success ? '操作成功' : response.message);
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

function tradeClose(orderId, orderPrice, orderBook){
    // 创建更丰富的表单界面
    var formContent = `
        <div class="layui-form" style="padding: 20px;">
            <div class="layui-form-item">
                <label class="layui-form-label">市场价格</label>
                <div class="layui-input-block">
                    <span class="layui-badge-rim">Ask: ${orderBook.askList}</span>
                    <span class="layui-badge-rim" style="margin-left: 10px;">Bid: ${orderBook.bidList}</span>
                </div>
            </div>
            <div class="layui-form-item">
                <label class="layui-form-label">原始价格</label>
                <div class="layui-input-block">
                    <input type="text" class="layui-input layui-bg-gray" value="${orderPrice}" readonly>
                </div>
            </div>
            <div class="layui-form-item">
                <label class="layui-form-label">平仓价格</label>
                <div class="layui-input-block">
                    <input type="number" id="closePrice" name="closePrice" class="layui-input" value="${Math.min((orderPrice * 0.2).toFixed(2), orderBook.askList)}" step="0.01">
                </div>
            </div>
            <div class="layui-form-item">
                <label class="layui-form-label">盈利比例</label>
                <div class="layui-input-inline" style="width: 120px;">
                    <input type="number" id="profitRatio" name="profitRatio" class="layui-input" value="80" min="0" max="100">
                </div>
                <div class="layui-form-mid layui-word-aux">%（设置后自动计算价格）</div>
            </div>
            <div class="layui-form-item">
                <label class="layui-form-label">截止时间</label>
                <div class="layui-input-block">
                    <input type="text" id="cannelOrderTime" name="cannelOrderTime" class="layui-input" placeholder="选择平仓单截止时间">
                </div>
            </div>
        </div>
    `;

    layer.open({
        type: 1,
        title: '确认平仓',
        area: ['500px', '450px'],
        content: formContent,
        btn: ['确认平仓', '取消'],
        success: function(layero, index) {
            // 监听盈利比例变化
            var $profitRatio = layero.find('#profitRatio');
            var $closePrice = layero.find('#closePrice');
            
            $profitRatio.on('input', function() {
                var ratio = parseFloat($(this).val()) / 100;
                if (!isNaN(ratio)) {
                    // 按照盈利比例计算价格: 原价 * (1-盈利比例)
                    var calculatedPrice = (orderPrice * (1-ratio)).toFixed(2);
                    $closePrice.val(calculatedPrice);
                }
            });
            
            // 计算初始平仓价格
            $profitRatio.trigger('input');
            
            // 初始化日期时间选择器
            layui.laydate.render({
                elem: '#cannelOrderTime',
                type: 'datetime',
                min: 0,
                format: 'yyyy-MM-dd HH:mm:ss'
            });
            
            // 重新渲染表单元素
            layui.form.render();
        },
        yes: function(index, layero) {
            // 获取表单数据
            var price = layero.find('#closePrice').val();
            var cannelOrderTime = layero.find('#cannelOrderTime').val();
            
            if (!price) {
                layer.msg('请输入平仓价格');
                return;
            }
            
            // 准备请求数据
            var requestData = {
                password: $("#totp").val(),
                price: price,
                orderId: orderId
            };
            
            // 如果设置了截止时间，添加到请求中
            if (cannelOrderTime) {
                requestData.cannelTime = cannelOrderTime;
            }
            
            // 下单
            $.ajax({
                url: "/trade/close",
                method: 'POST',
                data: requestData,
                success: function(response) {
                    layer.msg(response.success ? '操作成功' : response.message);
                    loadStrategyOrder(currentStrategyId);
                    layer.close(index);
                }
            });
        }
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
            tradeClose(orderObj.id, orderObj.price, result);
         }
       });
}

function assistant(prompt, title) {
    localStorage.setItem("title", title || "期权订单分析");
    localStorage.setItem("prompt", prompt);
    if(!assistantWindow || assistantWindow.closed){
        assistantWindow = window.open("assistant.html", "assistant");
    }else{
        assistantWindow.focus();
    }
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
            "ext": item.ext,
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
          {field: 'order', title: '操作', width: 150, templet: '#TPL-orderOp'},
          {field: 'platformOrderId', title: '订单号', width: 180},
          {field: 'platformOrderIdEx', title: '订单号Ex', width: 200},
          {field: 'platformFillId', title: '成交单', width: 180},
          {field: 'subOrder', title: '是否子单', width: 100},
          {field: 'isClose', title: '是否平仓', width: 100},
          {field: 'curPrice', title: '现价', width: 80},
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
          field: 'tradeTime',
          type: 'desc'
        },
        page: false,
        limits: [100, 200, 500],
        limit: 100
      });

      // 监听工具条事件
      table.on('tool(result)', function(obj) {
        var data = obj.data;
        var event = obj.event;
        var tr = obj.tr;
        
        if (event === 'delete') {
            tradeModify('delete', data.id);
        } else if (event === 'cancel') {
            tradeModify('cancel', data.id);
        } else if (event === 'closePosition') {
            closePosition(data.order);
        } else if (event === 'assistant') {
            if(data.ext && data.ext.prompt) {
                assistant(data.ext.prompt, data.code + " 期权订单分析");
            } else {
                layer.msg('没有可分析的数据');
            }
        }
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
      url: "/owner/get",
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
