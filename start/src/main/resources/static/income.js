// JS
// 实时更新当前股票价格
var currentPrice = {};
var assistantWindow;

function assistant(prompt, title) {
    localStorage.setItem("title", title || "期权订单分析");
    localStorage.setItem("prompt", prompt);
    if(!assistantWindow || assistantWindow.closed){
        assistantWindow = window.open("assistant.html", "assistant");
    }else{
        assistantWindow.focus();
    }
}

function renderOrderTable(orderList){
    if(!orderList){
        return;
    }

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

    layui.use('table', function(){
      var table = layui.table;
      var inst = table.render({
        elem: '#orderTable',
        cols: [[
          {title: '操作', width: 60, templet: function(d) {
              if(d.prompt) {
                  return '<a class="layui-btn layui-btn-normal layui-btn-xs" lay-event="assistant"><i class="layui-icon layui-icon-chat"></i>AI</a>';
              }
              return '';
          }},
          {field: 'tradeTime', title: '交易时间', width: 165},
          {field: 'strikeTime', title: '行权时间', width: 120},
          {field: 'code', title: '期权代码', width: 180},
          {field: 'underlyingCode', title: '证券代码', width: 150, templet: function(d){
                return `<span>${d.underlyingCode}</span><b name="stock_${d.market}_${d.underlyingCode}" class="current-price layui-badge"></b>`;
            }
          },
          {field: 'strikePrice', title: '行权价', width: 80},
          {field: 'curDTE', title: '到期天数', width: 100},
          {field: 'side', title: '类型', width: 80},
          {field: 'quantity', title: '数量', width: 80},
          {field: 'statusStr', title: '状态', width: 100},
          {field: 'price', title: '价格', width: 80},
          {field: 'totalIncome', title: '收入', width: 80},
          {field: 'curPrice', title: '现价', width: 80},
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
        data: convertedData,
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
        }
      });
      
      // 监听工具条事件
      table.on('tool(orderTable)', function(obj) {
        var data = obj.data;
        if (obj.event === 'assistant') {
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

