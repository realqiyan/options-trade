// JS
var currentStrategyId;
var assistantWindow;
var strategyList = []; // 存储策略列表

// URL参数解析函数
function getUrlParam(name) {
    var reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)");
    var r = window.location.search.substr(1).match(reg);
    if (r != null) return decodeURIComponent(r[2]); return null;
}

// 设置URL参数，不刷新页面
function setUrlParam(name, value) {
    var url = new URL(window.location.href);
    url.searchParams.set(name, value);
    window.history.pushState({}, '', url);
}

//owner: $("#owner").val(),
function tradeModify(action, orderId){
    layer.prompt({title: '确认操作', value: action}, function(value, index, elem){
        if(value === ''){
            return elem.focus();
        }
        
        var loadingIndex = layer.load(1, {shade: [0.1,'#fff']});
        $.ajax({
              url: "/trade/modify",
              method: 'POST',
              data: {
                password: $("#totp").val(),
                action: action,
                orderId: orderId,
              },
              success: function( response ) {
                layer.close(loadingIndex);
                layer.msg(response.success ? '操作成功' : response.message);
                if (response.success && currentStrategyId) {
                    loadStrategyOrder(currentStrategyId);
                }
                layer.close(index);
              },
              error: function() {
                layer.close(loadingIndex);
                layer.msg('操作失败，请重试');
              }
            });
    });
}

//owner: $("#owner").val(),
function sync(){
    if (!$("#totp").val()) {
        layer.msg('请输入验证码');
        return;
    }
    
    var loadingIndex = layer.load(1, {shade: [0.1,'#fff']});
    $.ajax({
          url: "/trade/sync",
          method: 'GET',
          data: {
            password: $("#totp").val(),
            time: new Date().getTime()
          },
          success: function( result ) {
            layer.close(loadingIndex);
            layer.msg(result.success ? '同步成功' : result.message);
            if (result.success && currentStrategyId) {
                loadStrategyOrder(currentStrategyId);
            }
          },
          error: function() {
            layer.close(loadingIndex);
            layer.msg('同步失败，请重试');
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
    try {
        var orderObj = JSON.parse(order);
        var loadingIndex = layer.load(1, {shade: [0.1,'#fff']});
        
        $.ajax({
             url: "/options/orderbook/get",
             method: 'GET',
             data: {
               code: orderObj.code,
               market: orderObj.market,
               time: new Date().getTime()
             },
             success: function( response ) {
                layer.close(loadingIndex);
                if (!response.success) {
                    layer.msg(response.message || '获取市场数据失败');
                    return;
                }
                var result = response.data;
                tradeClose(orderObj.id, orderObj.price, result);
             },
             error: function() {
                layer.close(loadingIndex);
                layer.msg('获取市场数据失败，请重试');
             }
           });
    } catch (e) {
        console.error('解析订单数据失败', e);
        layer.msg('订单数据无效，请刷新页面重试');
    }
}

function assistant(prompt, title) {
    localStorage.setItem("title", title || "期权订单分析");
    localStorage.setItem("prompt", prompt);
    if(!assistantWindow || assistantWindow.closed){
        assistantWindow = window.open("assistant.html?mode=agent", "_blank");
    }else{
        assistantWindow.focus();
    }
}

function renderTable(result){
    var orderList = result.strategyOrders;
    if(!orderList){
        return;
    }

    // 生成分组颜色映射
    var groupColors = {};
    var colorIndex = 0;
    var colors = ['#e6f7ff', '#f6ffed'];
    
    var convertedData = orderList.map(item => {
        // 获取分组ID，如果没有则使用默认分组
        var groupId = result.orderGroups[item.platformOrderId]?.groupId || 'default';
        
        // 为每个分组分配颜色
        if(!groupColors[groupId]) {
            groupColors[groupId] = colors[colorIndex % colors.length];
            colorIndex++;
        }
        
        return {
            "LAY_CHECKED": item.ext && "true" == item.ext.isClose ? false : true,
            "order": JSON.stringify(item),
            "id": item.id,
            "_groupId": groupId, // 用于样式设置
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
          {field: 'order', title: '操作', width: 280, templet: '#TPL-orderOp'},
          {field: 'curDTE', title: '关注', width: 50, align: 'center', templet: '#TPL-colorStatus'},
          {field: 'underlyingCode', title: '标的代码', width: 90, templet: function(d){
              return `<a href="analyze.html?code=${d.underlyingCode}&market=${d.market}&strikeTime=${d.strikeTime}" class="layui-btn layui-btn-primary layui-btn-xs">${d.underlyingCode}</a>`;
          }},
          {field: 'code', title: '证券代码', width: 180},
          {field: 'side', title: '类型', width: 80},
          {field: 'price', title: '价格', width: 85},
          {field: 'quantity', title: '数量', width: 80},
          {field: 'groupId', title: '分组收益', width: 130, templet: function(d){
              // 分组收益 title修改时需要同步修改done方法中名字，否则合并单元格会失败
              return `<div title="分组订单数:${result.orderGroups[d._groupId].orderCount}">收益:${result.orderGroups[d._groupId].totalIncome}<br/>费用:${result.orderGroups[d._groupId].totalOrderFee}</div>`;
          }},
          {field: 'totalIncome', title: '订单收益', width: 100},
          {field: 'manualIncome', title: '是否修正', width: 100, templet: function(d){
              return d.ext && d.ext.manualIncome ? '是' : '-';
          }},
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
        ]],
        data: convertedData,
        toolbar: true,
        lineStyle: 'height: 100%;',
        done: function(res, curr, count){
          // 设置分组行样式
          $('.layui-table-body tr').each(function(){
            var data = table.cache.result[$(this).data('index')];
            if(data && data._groupId) {
              $(this).css('background-color', groupColors[data._groupId]);
            }
          });

          // 动态查找"分组收益"列的索引
          var groupIncomeColIdx = -1;
          $('.layui-table-header th').each(function(idx){
            if ($(this).text().replace(/\s/g, '') === '分组收益') {
              groupIncomeColIdx = idx;
            }
          });
          if (groupIncomeColIdx === -1) return; // 没找到直接返回

          // 合并_groupId相同的"分组收益"列
          var $trs = $('.layui-table-body tr');
          var lastGroupId = null, groupStartIdx = 0, groupCount = 0;
          $trs.each(function(idx){
            var data = table.cache.result[$(this).data('index')];
            var groupId = data ? data._groupId : null;
            if(groupId !== lastGroupId && lastGroupId !== null){
              // 合并上一组
              if(groupCount > 1){
                var $firstTd = $trs.eq(groupStartIdx).find('td').eq(groupIncomeColIdx);
                $firstTd.attr('rowspan', groupCount);
                for(var i=1;i<groupCount;i++){
                  $trs.eq(groupStartIdx+i).find('td').eq(groupIncomeColIdx).hide();
                }
              }
              groupStartIdx = idx;
              groupCount = 1;
            }else{
              groupCount++;
            }
            lastGroupId = groupId;
          });
          // 合并最后一组
          if(groupCount > 1){
            var $firstTd = $trs.eq(groupStartIdx).find('td').eq(groupIncomeColIdx);
            $firstTd.attr('rowspan', groupCount);
            for(var i=1;i<groupCount;i++){
              $trs.eq(groupStartIdx+i).find('td').eq(groupIncomeColIdx).hide();
            }
          }
        },
        defaultToolbar: [
          'filter', 'exports', 'print'
        ],
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
        } else if (event === 'updateStatus') {
            updateOrderStatus(data.id);
        } else if (event === 'updateStrategy') {
            updateOrderStrategy(data.id);
        } else if (event === 'updateIncome') {
            updateOrderIncome(data.id, data.ext && data.ext.totalIncome ? data.ext.totalIncome : "0");
        }
      });
    });
}

// 新增：根据ID查找策略名称
function getStrategyNameById(strategyId) {
    if (!strategyList || strategyList.length === 0) return "未知策略";
    
    var strategy = strategyList.find(function(item) {
        return item.strategyId === strategyId;
    });

    return strategy ? strategy.strategyName : "未知策略";
}

function loadStrategyOrder(strategyId){
    // 更新当前策略ID并更新URL
    currentStrategyId = strategyId;
    setUrlParam('strategyId', strategyId);
    
    // 如果layui已经初始化，更新选中的Tab
    if (layui && layui.element) {
        var tabTitles = document.getElementById("strategyTabTitle");
        var tabs = tabTitles.getElementsByTagName('li');
        
        // 查找匹配的tab索引
        for (var i = 0; i < tabs.length; i++) {
            if (tabs[i].getAttribute('data-strategy-id') === strategyId) {
                layui.element.tabChange('strategyTab', i.toString());
                break;
            }
        }
    }
    
    $.ajax({
          url: "/options/strategy/get",
          data: {
            strategyId: strategyId
          },
          success: function( response ) {
            var result = response.data;
            if (!result) {
                layer.msg('获取策略数据失败');
                return;
            }
            
            // 使用laytpl渲染模板
            layui.laytpl(document.getElementById('summary').innerHTML).render(result, function(html){
                document.getElementById('title').innerHTML = html;
            });
            
            renderTable(result);
          },
          error: function() {
            layer.msg('获取策略数据失败');
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
        if (!result || !result.strategyList || result.strategyList.length === 0) {
            layer.msg('没有找到策略数据');
            return;
        }
        
        strategyList = result.strategyList;
        
        // 获取URL中的strategyId参数
        var urlStrategyId = getUrlParam('strategyId');
        var targetStrategyId = null;
        
        // 创建Tab标题
        var tabTitles = document.getElementById("strategyTabTitle");
        tabTitles.innerHTML = "";
        
        // 如果URL中有指定策略ID且在策略列表中存在，则使用URL中的，否则使用第一个策略
        if (urlStrategyId) {
            var strategyExists = strategyList.some(function(s) { 
                return s.strategyId === urlStrategyId; 
            });
            
            if (strategyExists) {
                targetStrategyId = urlStrategyId;
            }
        }
        
        // 如果没有找到有效的策略ID，使用第一个
        if (!targetStrategyId && strategyList.length > 0) {
            targetStrategyId = strategyList[0].strategyId;
        }
        
        currentStrategyId = targetStrategyId;
        
        // 初始化并渲染Tab
        layui.use('element', function(){
            var element = layui.element;
            
            // 清空Tab内容
            tabTitles.innerHTML = "";
            
            // 添加策略Tab
            for(var i = 0; i < strategyList.length; i++) {
                var obj = strategyList[i];
                var isActive = (obj.strategyId === targetStrategyId);
                
                // 添加Tab项（使用索引作为lay-id，但也保留策略ID作为data属性）
                tabTitles.innerHTML += '<li lay-id="' + i + '" data-strategy-id="' + obj.strategyId + '"' + 
                                      (isActive ? ' class="layui-this"' : '') + '>' + 
                                      obj.strategyName + '</li>';
            }
            
            // 重新渲染Tab
            element.render('tab');
            
            // 监听Tab切换事件
            element.on('tab(strategyTab)', function(data){
                var strategyId = tabTitles.getElementsByTagName('li')[data.index].getAttribute('data-strategy-id');
                if (strategyId !== currentStrategyId) {
                    loadStrategyOrder(strategyId);
                }
            });
            
            // 加载当前选中的策略订单
            if (targetStrategyId) {
                loadStrategyOrder(targetStrategyId);
            }
        });
      },
      error: function() {
        layer.msg('获取数据失败');
      }
    });
}

function updateOrderIncome(orderId, currentIncome) {
    layer.prompt({
        title: '请输入修正的收益值',
        value: currentIncome,
        formType: 0 // 0 表示文本输入
    }, function(value, index, elem) {
        if (value === '') {
            layer.msg('收益值不能为空');
            return elem.focus();
        }
        
        // 验证是否为有效数字
        if (isNaN(parseFloat(value))) {
            layer.msg('请输入有效的数字');
            return elem.focus();
        }
        
        var loadingIndex = layer.load(1, {shade: [0.1, '#fff']});
        $.ajax({
            url: "/trade/updateIncome",
            method: 'POST',
            data: {
                password: $("#totp").val(),
                orderId: orderId,
                manualIncome: value
            },
            success: function(response) {
                layer.close(loadingIndex);
                layer.msg(response.success ? '收益更新成功' : response.message);
                if (response.success && currentStrategyId) {
                    loadStrategyOrder(currentStrategyId);
                }
                layer.close(index);
            },
            error: function() {
                layer.close(loadingIndex);
                layer.msg('更新失败，请重试');
            }
        });
    });
}

function updateOrderStatus(orderId) {
    var statusOptions = [
        {value: 1, title: '待提交'},
        {value: 2, title: '提交中'},
        {value: 5, title: '已提交'},
        {value: 10, title: '部分成交'},
        {value: 11, title: '全部成交'},
        {value: 14, title: '部分撤单'},
        {value: 15, title: '已撤单'},
        {value: 21, title: '下单失败'},
        {value: 22, title: '已失效'},
        {value: 23, title: '已删除'},
        {value: 24, title: '成交撤销'},
        {value: 25, title: '提前指派'}
    ];
    
    layer.open({
        type: 1,
        title: '修改订单状态',
        area: ['400px', '300px'],
        content: `
            <div class="layui-form" style="padding: 20px;">
                <div class="layui-form-item">
                    <label class="layui-form-label">选择状态</label>
                    <div class="layui-input-block">
                        <select name="status" lay-verify="required">
                            ${statusOptions.map(opt => `<option value="${opt.value}">${opt.title}</option>`).join('')}
                        </select>
                    </div>
                </div>
            </div>
        `,
        btn: ['确认', '取消'],
        success: function(layero, index) {
            layui.form.render('select');
        },
        yes: function(index, layero) {
            var status = layero.find('select[name="status"]').val();
            if (!status) {
                layer.msg('请选择状态');
                return;
            }
            
            var loadingIndex = layer.load(1, {shade: [0.1,'#fff']});
            $.ajax({
                url: "/trade/updateStatus",
                method: 'POST',
                data: {
                    password: $("#totp").val(),
                    orderId: orderId,
                    status: status
                },
                success: function(response) {
                    layer.close(loadingIndex);
                    layer.msg(response.success ? '修改成功' : response.message);
                    if (response.success && currentStrategyId) {
                        loadStrategyOrder(currentStrategyId);
                    }
                    layer.close(index);
                },
                error: function() {
                    layer.close(loadingIndex);
                    layer.msg('操作失败，请重试');
                }
            });
        }
    });
}

function updateOrderStrategy(orderId) {
    var loadingIndex = layer.load(1, {shade: [0.1,'#fff']});
    $.ajax({
        url: "/owner/get",
        method: 'GET',
        success: function(response) {
            layer.close(loadingIndex);
            if (!response.success) {
                layer.msg(response.message);
                return;
            }
            
            var strategies = response.data.strategyList;
            if (!strategies || strategies.length === 0) {
                layer.msg('未找到可用策略');
                return;
            }
            
            layer.open({
                type: 1,
                title: '修改订单策略',
                area: ['400px', '300px'],
                content: `
                    <div class="layui-form" style="padding: 20px;">
                        <div class="layui-form-item">
                            <label class="layui-form-label">选择策略</label>
                            <div class="layui-input-block">
                                <select name="strategyId" lay-verify="required">
                                    <option value="">请选择策略</option>
                                    ${strategies.map(strategy => `<option value="${strategy.strategyId}">${strategy.strategyName}</option>`).join('')}
                                </select>
                            </div>
                        </div>
                    </div>
                `,
                btn: ['确认', '取消'],
                success: function(layero, index) {
                    layui.form.render('select');
                },
                yes: function(index, layero) {
                    var strategyId = layero.find('select[name="strategyId"]').val();
                    if (!strategyId) {
                        layer.msg('请选择策略');
                        return;
                    }
                    
                    var submitIndex = layer.load(1, {shade: [0.1,'#fff']});
                    $.ajax({
                        url: "/trade/updateStrategy",
                        method: 'POST',
                        data: {
                            password: $("#totp").val(),
                            orderId: orderId,
                            strategyId: strategyId
                        },
                        success: function(response) {
                            layer.close(submitIndex);
                            layer.msg(response.success ? '修改成功' : response.message);
                            if (response.success) {
                                // 判断是否需要切换到新策略
                                if (strategyId !== currentStrategyId) {
                                    // 如果策略变更，则刷新当前策略，不切换
                                    loadStrategyOrder(currentStrategyId);
                                } else {
                                    // 如果当前策略未变化，直接刷新
                                    loadStrategyOrder(currentStrategyId);
                                }
                            }
                            layer.close(index);
                        },
                        error: function() {
                            layer.close(submitIndex);
                            layer.msg('操作失败，请重试');
                        }
                    });
                }
            });
        },
        error: function() {
            layer.close(loadingIndex);
            layer.msg('获取策略列表失败');
        }
    });
}

// 页面初始化
$(function() {
    // 使用layui组件
    layui.use(['element', 'table', 'layer', 'form', 'laydate', 'laytpl'], function(){
        window.laytpl = layui.laytpl;
        window.layer = layui.layer;
        window.form = layui.form;
        window.laydate = layui.laydate;
        
        // 加载数据
        reloadData();
    });
});
