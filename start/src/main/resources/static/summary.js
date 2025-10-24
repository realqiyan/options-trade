// JS
// 实时更新当前股票价格
var currentPrice = {};
var assistantWindow;
// 添加筛选状态变量
var filterState = {
    putOnly: false,
    callOnly: false
};
var originalOrderData = [];

/**
 * 跳转到策略订单页面
 * @param {string} strategyId 策略ID
 */
function goToStrategyOrders(strategyId) {
    if (!strategyId) return;
    
    // 跳转到order.html页面并传递策略ID参数
    window.location.href = 'order.html?strategyId=' + encodeURIComponent(strategyId);
}

function assistant(prompt, title) {
    localStorage.setItem("title", title || "期权订单分析");
    localStorage.setItem("prompt", prompt);
    if(!assistantWindow || assistantWindow.closed){
        assistantWindow = window.open("assistant.html?mode=agent_v2", "_blank");
    }else{
        assistantWindow.focus();
    }
}

// 策略分析功能
function strategyAnalysis(strategyName, prompt) {
    if (!strategyName) {
        layer.msg('策略不能为空');
        return;
    }
    // 打开AI助手进行分析
    assistant(prompt, strategyName + "策略分析");
}

function renderPositionTable(positions){
    if(!positions){
        return;
    }
    layui.use(['table'], function () {
        const table = layui.table;
        // 初始化表格
        table.render({
            elem: '#positionTable',
            data: positions,
            cols: [[
                {field: 'securityCode', title: '代码', width: 220},
                {field: 'securityName', title: '名称', width: 360},
                {field: 'market', title: '市场', width: 80},
                {field: 'quantity', title: '持仓数量', width: 120},
                {field: 'canSellQty', title: '可卖数量', width: 120},
                {field: 'costPrice', title: '成本价', width: 120},
                {field: 'currentPrice', title: '当前价', width: 120}
            ]]
        });
    });

}

function renderOrderTable(orderList){
    if(!orderList){
        return;
    }

    // 保存原始数据
    originalOrderData = orderList;
    
    var convertedData = orderList.map(item => {
        // 计算盈亏比例的颜色
        let profitRatioClass = '';
        let scaleRatioClass = '';
        if(item.ext && item.ext.profitRatio) {
            profitRatioClass = parseFloat(item.ext.profitRatio) >= 0 ? 'positive-value' : 'negative-value';
        }
        
        // 检查现价是否达到行权价
        let rowClass = '';
        if(item.ext && item.ext.strikePrice && currentPrice[item.underlyingCode]) {
            const curPrice = parseFloat(currentPrice[item.underlyingCode]);
            const strikePrice = parseFloat(item.ext.strikePrice);
            // PUT期权：如果现价<=行权价，标红
            // CALL期权：如果现价>=行权价，标红
            if((item.ext.isPut && curPrice <= strikePrice) || 
               (item.ext.isCall && curPrice >= strikePrice)) {
                rowClass = 'strike-alert';
            }
        }
        
        return {
            "strikeTime": extractDate(item.strikeTime),
            "market": item.market,
            "underlyingCode": item.underlyingCode,
            "code": item.code,
            "side": sideMapping(item.side),
            "price": item.price,
            "quantity": item.quantity,
            "statusStr": statusMapping(item.status+''),
            "curDTE": item.ext ? item.ext.curDTE : null,
            "strikePrice": item.ext ? item.ext.strikePrice : null,
            "codeType": item.ext ? item.ext.codeType : null,
            "isPut": item.ext ? item.ext.isPut : false,
            "isCall": item.ext ? item.ext.isCall : false,
            "totalIncome": item.ext ? item.ext.totalIncome : null,
            "orderFee": item.orderFee,
            "tradeTime": item.tradeTime,
            "curPrice": item.ext ? item.ext.curPrice : null,
            "profitRatio": item.ext && item.ext.profitRatio ? item.ext.profitRatio + '%' : null,
            "profitRatioClass":profitRatioClass,
            "scaleRatio": item.ext ? item.ext.scaleRatio : null,
            "rowClass": rowClass,
            "ext": item.ext,
            "prompt": item.ext ? item.ext.prompt : null
        };
    });
    
    // 应用筛选
    var filteredData = applyFilter(convertedData);

    layui.use(['table', 'form'], function(){
      var table = layui.table;
      var form = layui.form;
      
      // 在表格上方添加筛选按钮
      var filterHtml = `
        <div class="layui-btn-group option-filter" style="margin-bottom: 10px;">
          <button class="layui-btn layui-btn-sm ${filterState.putOnly ? 'layui-btn-normal' : 'layui-btn-primary'}" id="putFilterBtn">
            仅显示PUT <span class="layui-badge layui-bg-orange">${countOptionTypes(convertedData).put}</span>
          </button>
          <button class="layui-btn layui-btn-sm ${filterState.callOnly ? 'layui-btn-normal' : 'layui-btn-primary'}" id="callFilterBtn">
            仅显示CALL <span class="layui-badge layui-bg-orange">${countOptionTypes(convertedData).call}</span>
          </button>
          <button class="layui-btn layui-btn-sm layui-btn-primary" id="resetFilterBtn">
            重置筛选 <span class="layui-badge layui-bg-blue">${convertedData.length}</span>
          </button>
        </div>
      `;
      
      // 添加筛选按钮到表格区域
      var tableContainer = document.querySelector('#orderTable');
      var filterContainer = document.createElement('div');
      filterContainer.id = 'option-filter-container';
      filterContainer.innerHTML = filterHtml;
      
      // 检查是否已经存在筛选按钮
      var existingFilterContainer = document.getElementById('option-filter-container');
      if(existingFilterContainer) {
        existingFilterContainer.innerHTML = filterHtml;
      } else {
        tableContainer.parentNode.insertBefore(filterContainer, tableContainer);
      }
      
      var inst = table.render({
        elem: '#orderTable',
        cols: [[
          {title: '操作', width: 160, templet: function(d) {
              if(d.prompt) {
                  return '<a class="layui-btn layui-btn-normal layui-btn-xs" lay-event="orderAnalysis">订单分析</a>'+
                  '<a class="layui-btn layui-btn-normal layui-btn-xs" lay-event="strategyAnalysis">策略分析</a>';
              }
              return '';
          }},
          {field: 'strategyName', title: '策略｜Delta', width: 200, templet: function(d){
              if (d.ext && d.ext.strategyName && d.ext.strategyId && d.ext.strategyAvgDelta) {
                  return `<a href="javascript:void(0);" class="strategy-link table-link" data-strategy-id="${d.ext.strategyId}">${d.ext.strategyName}｜${d.ext.strategyAvgDelta}</a>`;
              }
              return d.ext && d.ext.strategyName ? d.ext.strategyName : '-';
          }},
          {field: 'tradeTime', title: '交易时间', width: 165},
          {field: 'code', title: '期权代码', width: 180},
          {field: 'underlyingCode', title: '证券代码', width: 90, templet: function(d){
              return `<a href="analyze.html?code=${d.underlyingCode}&market=${d.market}&strikeTime=${d.strikeTime}" class="table-link">${d.underlyingCode}</a>`;
          }},
          {field: 'strikeTime', title: '行权时间', width: 120},
          {field: 'codeType', title: '标的类型', width: 90},
          {field: 'strikePrice', title: '行权价/现价', width: 120, templet: function(d){
                return `<span>${d.strikePrice}</span><b name="stock_${d.market}_${d.underlyingCode}" class="current-price layui-badge"></b>`;
            }
          },
          {field: 'side', title: '类型', width: 80},
          {field: 'quantity', title: '合约数', width: 80},
          {field: 'curDTE', title: '剩余/天', width: 100, sort: true},
          {field: 'statusStr', title: '状态', width: 100},
          {field: 'price', title: '订单价', width: 80},
          {field: 'curPrice', title: '现价', width: 80},
          {field: 'totalIncome', title: '总收入', width: 80},
          {field: 'orderFee', title: '订单费用', width: 100},
          {field: 'profitRatio', title: '盈亏', width: 80, templet: function(d){
              return '<span class="' + d.profitRatioClass + '">' + d.profitRatio + '</span>';
          }},
          {field: 'scaleRatio', title: '规模占比', width: 100, templet: function(d){
              if (!d.scaleRatio) return '-';
              var ratio = parseFloat(d.scaleRatio);
              var positionRatio = parseFloat(d.positionRatio || 0.1);
              var color = ratio > positionRatio ? 'color: #ff5722;' : '';
              return '<span style="' + color + '">' + (ratio * 100).toFixed(1) + '%</span>';
          }}
          
        ]],
        data: filteredData,
        toolbar: true,
        lineStyle: 'height: 100%;',
        defaultToolbar: [
          'filter', // 列筛选
          'exports', // 导出
          'print' // 打印
        ],
        initSort: {
          field: 'tradeTime',
          type: 'asc'
        },
        sort: true,
        page: false,
        limits: [100, 200, 500],
        limit: 100,
        // 添加行样式
        done: function(res, curr, count){
            // 为触发行权价的行添加样式
            const table = this.elem.next();
            res.data.forEach(function(item, index){
                if(item.rowClass === 'strike-alert'){
                    const tr = table.find('.layui-table-box tbody tr[data-index="'+ index +'"]');
                    tr.addClass('strike-alert');
                }
            });
            
            // 绑定筛选按钮事件
            bindFilterEvents();
            
            // 绑定策略名称点击事件
            table.find('.strategy-link').on('click', function() {
                var strategyId = this.getAttribute('data-strategy-id');
                if (strategyId) {
                    goToStrategyOrders(strategyId);
                }
            });
        }
      });
      
      // 监听工具条事件
      table.on('tool(orderTable)', function(obj) {
        var data = obj.data;
        if (obj.event === 'orderAnalysis') {
            if(data.ext && data.ext.prompt) {
                assistant(data.ext.prompt, data.code + " 期权订单分析");
            } else {
                layer.msg('没有可分析的数据');
            }
        } else if (obj.event === 'strategyAnalysis') {
            if(data.ext && data.ext.strategyId) {
                strategyAnalysis(data.ext.strategyName, data.ext.strategyPrompt);
            } else {
                layer.msg('没有可分析的数据');
            }
        }
      });
    });

    render();
}

// 添加筛选按钮事件绑定函数
function bindFilterEvents() {
    document.getElementById('putFilterBtn').onclick = function() {
        filterState.putOnly = !filterState.putOnly;
        filterState.callOnly = false; // 确保同时只有一种筛选激活
        refreshOrderTable();
    };
    
    document.getElementById('callFilterBtn').onclick = function() {
        filterState.callOnly = !filterState.callOnly;
        filterState.putOnly = false; // 确保同时只有一种筛选激活
        refreshOrderTable();
    };
    
    document.getElementById('resetFilterBtn').onclick = function() {
        filterState.putOnly = false;
        filterState.callOnly = false;
        refreshOrderTable();
    };
}

// 添加计算不同类型期权数量的函数
function countOptionTypes(data) {
    let putCount = 0;
    let callCount = 0;
    
    data.forEach(item => {
        if (item.isPut) putCount++;
        if (item.isCall) callCount++;
    });
    
    return {
        put: putCount,
        call: callCount
    };
}

// 应用筛选函数
function applyFilter(data) {
    if (!filterState.putOnly && !filterState.callOnly) {
        return data; // 没有筛选条件，返回全部数据
    }
    
    return data.filter(item => {
        if (filterState.putOnly && item.isPut) return true;
        if (filterState.callOnly && item.isCall) return true;
        return false;
    });
}

// 刷新表格数据
function refreshOrderTable() {
    if (!originalOrderData || originalOrderData.length === 0) return;
    
    // 获取表格数据的转换副本
    var convertedData = originalOrderData.map(item => {
        // 此处可以只返回isPut和isCall属性，因为这是筛选所需的
        return {
            "isPut": item.ext ? item.ext.isPut : false,
            "isCall": item.ext ? item.ext.isCall : false
        };
    });
    
    // 更新筛选按钮上的数量显示
    updateFilterCounts(convertedData);
    
    // 重新渲染表格
    renderOrderTable(originalOrderData);
}

// 更新筛选按钮上显示的数量
function updateFilterCounts(data) {
    const counts = countOptionTypes(data);
    const totalCount = data.length;
    
    const putBtn = document.getElementById('putFilterBtn');
    const callBtn = document.getElementById('callFilterBtn');
    const resetBtn = document.getElementById('resetFilterBtn');
    
    if (putBtn) {
        // 如果启用了PUT筛选，显示筛选后的数量和总数
        if (filterState.putOnly) {
            putBtn.innerHTML = `仅显示PUT <span class="layui-badge layui-bg-orange">${counts.put}/${counts.put}</span>`;
        } else {
            putBtn.innerHTML = `仅显示PUT <span class="layui-badge layui-bg-orange">${counts.put}</span>`;
        }
    }
    
    if (callBtn) {
        // 如果启用了CALL筛选，显示筛选后的数量和总数
        if (filterState.callOnly) {
            callBtn.innerHTML = `仅显示CALL <span class="layui-badge layui-bg-orange">${counts.call}/${counts.call}</span>`;
        } else {
            callBtn.innerHTML = `仅显示CALL <span class="layui-badge layui-bg-orange">${counts.call}</span>`;
        }
    }
    
    if (resetBtn) {
        resetBtn.innerHTML = `重置筛选 <span class="layui-badge layui-bg-blue">${totalCount}</span>`;
    }
}

function showChart(label, data, type) {
    const ctx = document.getElementById('monthlyIncomeChart').getContext('2d');
    
    // 计算最大值和最小值，用于设置Y轴范围
    const maxValue = Math.max(...data) * 1.1; // 增加10%的空间
    const minValue = Math.min(0, Math.min(...data) * 1.1); // 如果有负值，则扩展负方向
    
    // 生成渐变背景
    const gradient = ctx.createLinearGradient(0, 0, 0, 400);
    gradient.addColorStop(0, 'rgba(30, 159, 255, 0.5)');
    gradient.addColorStop(1, 'rgba(30, 159, 255, 0)');
    
    new Chart(ctx, {
        type: 'line',
        data: {
            labels: label,
            datasets: [{
                label: '月度收益',
                data: data,
                fill: true,
                backgroundColor: gradient,
                borderColor: '#1E9FFF',
                borderWidth: 2,
                pointBackgroundColor: '#1E9FFF',
                pointBorderColor: '#fff',
                pointRadius: 5,
                tension: 0.3 // 使线条更平滑
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                x: {
                    title: {
                        display: true,
                        text: '月份',
                        font: {
                            weight: 'bold'
                        }
                    },
                    grid: {
                        display: false
                    }
                },
                y: {
                    title: {
                        display: true,
                        text: '收入 ($)',
                        font: {
                            weight: 'bold'
                        }
                    },
                    min: minValue,
                    max: maxValue,
                    grid: {
                        color: 'rgba(200, 200, 200, 0.2)'
                    }
                }
            },
            plugins: {
                tooltip: {
                    backgroundColor: 'rgba(0, 0, 0, 0.7)',
                    titleFont: {
                        size: 14
                    },
                    bodyFont: {
                        size: 14
                    },
                    callbacks: {
                        label: function(context) {
                            return '收入: $' + context.raw;
                        }
                    }
                },
                legend: {
                    display: false
                }
            }
        }
    });
}

function reloadData(){
    // 显示加载进度
    layui.use(['element'], function(){
        var element = layui.element;
        element.progress('loading', '30%');
    });
    
    $.ajax({
      url: "/owner/summary",
      data: {
        time: new Date().getTime()
      },
      beforeSend: function() {
        // 显示加载中状态
        layui.use(['element'], function(){
            var element = layui.element;
            element.progress('loading', '60%');
        });
      },
      success: function(response) {
        // 更新进度
        layui.use(['element'], function(){
            var element = layui.element;
            element.progress('loading', '90%');
        });
        
        var result = response.data;

        var getTpl = summary.innerHTML;
        var view = document.getElementById('result');
        laytpl(getTpl).render(result, function(html){
          view.innerHTML = html;
        });
        renderPositionTable(result.positions);
        renderOrderTable(result.unrealizedOrders);

        // 准备月度收益数据
        const monthlyIncome = result.monthlyIncome;
        const labels = Object.keys(monthlyIncome);
        const data = Object.values(monthlyIncome);

        // 调用 showChart 函数绘制折线图
        showChart(labels, data, 'line');
        render();
        
        // 完成加载
        layui.use(['element', 'layer'], function(){
            var element = layui.element;
            var layer = layui.layer;
            element.progress('loading', '100%');
            layer.msg('数据加载完成', {icon: 1, time: 1000});
        });
      },
      error: function(xhr, status, error) {
        layui.use(['layer'], function(){
            var layer = layui.layer;
            layer.msg('加载数据失败: ' + error, {icon: 2});
        });
      }
    });
}

// 初始加载数据
$(document).ready(function() {
    reloadData();

    // 绑定刷新按钮事件
    $('#refreshBtn').on('click', function() {
        reloadData();
    });
});

// 添加响应式调整
$(window).resize(function() {
    // 如果图表已经存在，重新加载数据以适应新的窗口大小
    if(document.getElementById('monthlyIncomeChart')) {
        // 获取父容器宽度
        const width = $('#monthlyIncomeChart').parent().width();
        // 设置图表宽度
        $('#monthlyIncomeChart').attr('width', width);
    }
});


// 实时股票价格
let clientId = "income_realtime_price_" + new Date().getTime();
let source = null;
if (window.EventSource) {
    // 连接的建立
    source = new EventSource("/connect?requestId=" + clientId);
    source.addEventListener("message", function (e) {
        content = JSON.parse(e.data);
        if(content.data.stock_price){
            var currentData = content.data.stock_price;
            currentPrice[currentData.security.code] = currentData.lastDone;
            var priceEleArr = document.getElementsByName("stock_"+currentData.security.market+'_'+currentData.security.code);
            if(priceEleArr){
                for(var i=0;i<priceEleArr.length;i++){
                    priceEleArr[i].innerHTML = currentData.lastDone;
                }
            }
            
            // 检查表格中是否存在该股票的期权订单，如果存在则重新检查行权条件
            updateStrikeAlerts(currentData.security.code, currentData.lastDone);
        }
    });
}

// 价格更新后重新检查行权触发条件
function updateStrikeAlerts(stockCode, price) {
    layui.use('table', function(){
        var table = layui.table;
        var tableElem = document.getElementById('orderTable');
        if (!tableElem) return;
        
        // 获取表格实例
        var tableInstance = table.cache.orderTable;
        if (!tableInstance) return;
        
        var needRefresh = false;
        
        // 遍历表格数据，检查是否有需要更新的行
        for (var i = 0; i < tableInstance.length; i++) {
            var item = tableInstance[i];
            if (item.underlyingCode === stockCode && item.strikePrice) {
                var oldClass = item.rowClass || '';
                var strikePrice = parseFloat(item.strikePrice);
                var curPrice = parseFloat(price);
                
                // 检查是否触发行权条件
                if ((item.isPut && curPrice <= strikePrice) || 
                    (item.isCall && curPrice >= strikePrice)) {
                    if (oldClass !== 'strike-alert') {
                        item.rowClass = 'strike-alert';
                        needRefresh = true;
                    }
                } else {
                    if (oldClass === 'strike-alert') {
                        item.rowClass = '';
                        needRefresh = true;
                    }
                }
            }
        }
        
        // 如果有行需要更新样式，重新渲染表格
        if (needRefresh) {
            var tableDiv = document.querySelector('.layui-table-box');
            if (tableDiv) {
                table.reload('orderTable', {
                    data: tableInstance
                });
            }
        }
    });
}

// 关闭Sse连接
function closeSse() {
    source.close();
    const httpRequest = new XMLHttpRequest();
    httpRequest.open('GET', '/close?requestId=' + clientId, true);
    httpRequest.send();
    console.log("close");
}
// 监听窗口关闭事件，主动去关闭连接
window.onbeforeunload = function () {
    closeSse();
};
