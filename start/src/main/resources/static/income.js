// JS

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
        
        return {
            "strikeTime": extractDate(item.strikeTime),
            "market": item.market,
            "underlyingCode": item.underlyingCode,
            "code": item.code,
            "side": sideMapping(item.side),
            "price": item.price,
            "quantity": item.quantity,
            "totalIncome": item.ext ? item.ext.totalIncome : null,
            "orderFee": item.orderFee,
            "tradeTime": item.tradeTime,
            "curPrice": item.ext ? item.ext.curPrice : null,
            "profitRatio": item.ext && item.ext.profitRatio ? item.ext.profitRatio + '%' : null,
            "profitRatioClass":profitRatioClass,
            "scaleRatio": item.ext ? item.ext.scaleRatio : null
        };
    });

    layui.use('table', function(){
      var table = layui.table;
      var inst = table.render({
        elem: '#orderTable',
        cols: [[
          {field: 'tradeTime', title: '交易时间', width: 180, sort: true},
          {field: 'strikeTime', title: '行权时间', width: 130, sort: true},
          {field: 'underlyingCode', title: '证券代码', width: 180, sort: true, templet: function(d){
                return `<span>${d.underlyingCode}</span><b name="stock_${d.market}_${d.underlyingCode}" class="layui-badge"></b>`;
            }
          },
          {field: 'code', title: '期权代码', width: 180, sort: true},
          {field: 'side', title: '类型', width: 80},
          {field: 'quantity', title: '数量', width: 80},
          {field: 'price', title: '价格', width: 100},
          {field: 'totalIncome', title: '收入', width: 100},
          {field: 'curPrice', title: '现价', width: 100},
          {field: 'orderFee', title: '订单费用', width: 100},
          {field: 'profitRatio', title: '盈亏', width: 100, templet: function(d){
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
            var priceEleArr = document.getElementsByName("stock_"+currentData.security.market+'_'+currentData.security.code);
            if(priceEleArr){
                for(var i=0;i<priceEleArr.length;i++){
                    priceEleArr[i].innerHTML = currentData.lastDone;
                }
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

