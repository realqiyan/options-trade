//JS
var currentStrategyId;
var currentCode;
var currentMarket;
var currentOwnerData;

var assistantWindow;
var currentPrompt;
var currentLabel;
var currentChart;

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

function filterStrategyByCode(strategyList, code) {
    // 过滤掉状态为suspend的策略
    strategyList = strategyList.filter(function(strategy) {
        return strategy.stage !== 'suspend';
    });
    return strategyList.filter(item => item.code === code);
}
function filterStrategyById(strategyList, strategyId) {
    // 过滤掉状态为suspend的策略
    strategyList = strategyList.filter(function(strategy) {
        return strategy.stage !== 'suspend';
    });
    return strategyList.filter(item => item.strategyId === strategyId);
}
function strategySelect(code,strategyId){
    currentStrategyId = strategyId;
    localStorage.setItem(code+'_strategyId', currentStrategyId);
}
function strategyLoad(code){
    currentStrategyId = localStorage.getItem(code+'_strategyId');
    console.log('strategyLoad code:'+ code+' currentStrategyId:'+currentStrategyId);
}
function resetContent(title){
    document.getElementById("title").innerHTML = title;
    chartCanvas.style.display = 'none';
    currentLabel = "";
    currentPrompt = "";
}
function assistant(title){
    if(!currentPrompt){
        layer.msg('请选择策略和行权日期！');
        return;
    }
    localStorage.setItem("title", title);
    localStorage.setItem("prompt", currentPrompt);
    if(!assistantWindow || assistantWindow.closed){
        assistantWindow = window.open("assistant.html?mode=agent_v2", "_blank");
    }else{
        assistantWindow.focus();
    }
}
var chartCanvas = document.getElementById('chartZone');
function showChart(label, data, type, options) {
    if (currentChart) {
        currentChart.destroy();
        chartCanvas.removeAttribute('width');
    }
    if (chartCanvas.style.display != 'none' && currentLabel == label) {
        chartCanvas.style.display = 'none';
    } else {
        chartCanvas.style.display = 'block';
        currentLabel = label;

        var labels;
        const datasets = [];
        
        // 图表配置选项
        const chartOptions = options || {};
        
        // 默认颜色配置
        const defaultColors = [
            { borderColor: 'rgba(255, 99, 132, 1)', backgroundColor: 'rgba(255, 99, 132, 0.2)' },
            { borderColor: 'rgba(54, 162, 235, 1)', backgroundColor: 'rgba(54, 162, 235, 0.2)' },
            { borderColor: 'rgba(75, 192, 192, 1)', backgroundColor: 'rgba(75, 192, 192, 0.5)' },
            { borderColor: 'rgba(255, 206, 86, 1)', backgroundColor: 'rgba(255, 206, 86, 0.2)' },
            { borderColor: 'rgba(153, 102, 255, 1)', backgroundColor: 'rgba(153, 102, 255, 0.2)' }
        ];
        
        if (Array.isArray(data)) {
            // 单图表情况
            const reversedData = data.reverse();
            labels = reversedData.map(item => item.date);
            datasets.push({
                label: label,
                data: reversedData.map(item => item.value),
                borderWidth: 1,
                ...defaultColors[0]
            });
        } else {
            // 多图表情况
            let colorIndex = 0;
            
            for (const [key, value] of Object.entries(data)) {
                const reversedData = value.reverse();
                if(!labels || labels.length < reversedData.length){
                    labels = reversedData.map(item => item.date);
                }
                
                // 基本配置
                let datasetConfig = {
                    label: key,
                    data: reversedData.map(item => item.value),
                    borderWidth: 1,
                    ...defaultColors[colorIndex % defaultColors.length]
                };
                
                // 应用特定系列的配置
                if (chartOptions.seriesConfig && chartOptions.seriesConfig[key]) {
                    Object.assign(datasetConfig, chartOptions.seriesConfig[key]);
                }
                
                datasets.push(datasetConfig);
                colorIndex++;
            }
        }

        // 对齐数据 找到最大长度
        const maxLength = Math.max(...datasets.map(dataset => dataset.data.length));
        // 填充数据
        datasets.forEach(dataset => {
            while (dataset.data.length < maxLength) {
                dataset.data.unshift(null);
            }
        });

        // 使用传入的图表类型，如果有混合类型的配置则忽略
        let finalChartType = type;
        if (datasets.some(dataset => dataset.type)) {
            finalChartType = 'line'; // 当有混合类型时，基础类型设为line
        }

        currentChart = new Chart(chartCanvas, {
            type: finalChartType,
            data: {
                labels: labels,
                datasets: datasets
            },
            options: {
                responsive: false,
                scales: {
                    y: {
                        // beginAtZero: true
                    }
                },
                onClick: function (event, elements) {
                    chartCanvas.style.display = 'none';
                }
            }
        });
    }
}
function loadOptionsExpDate(code, market, urlStrikeTime){
    currentCode = code;
    currentMarket = market;
    
    // 更新URL参数
    setUrlParam('code', code);
    setUrlParam('market', market);
    
    strategyLoad(code);
    console.log('loadOptionsExpDate code:'+ code+' market:'+ market+' currentStrategyId:'+currentStrategyId);
    $.ajax({
      url: "/options/strike/list",
      data: {
        code: code,
        market: market,
        time: new Date().getTime()
      },
      success: function( response ) {
        resetContent("请选择期权到期日");
        var result = response.data;

        layui.use(function(){
            var ready = false;
            var tabs = layui.tabs;
            var titleList = [];
            var contentList = [];
            var targetIndex = 0; // 默认选择第一个
            
            for(var i=0; i<result.length; i++) {
                var obj = result[i];
                titleList.push({ title: `${obj.strikeTime}-(${obj.optionExpiryDateDistance})-(${obj.tag})` });
                contentList.push({ content: ''});
                
                // 如果指定了urlStrikeTime且找到匹配项，记录索引
                if (urlStrikeTime && obj.strikeTime == urlStrikeTime) {
                    targetIndex = i;
                }
            }

            tabs.on(`afterChange(strike-list)`, function(data) {
                if (!ready){
                    console.log('stop change to index:'+data.index);
                    return;
                }
                var obj = result[data.index];
                console.log('change to index:'+data.index+' obj:',obj);
                loadOptionsChain(obj.strikeTime);
            });
            
            // 方法渲染
            tabs.render({
                elem: '#strike-list',
                header: titleList,
                body: contentList,
                closable: false
            });

            // 渲染当前证券策略列表
            var currentStrategyList = filterStrategyByCode(currentOwnerData.strategyList,currentCode);
            laytpl(currentStrategy.innerHTML).render({list:currentStrategyList,strategyId:currentStrategyId}, function(html){
                document.getElementById('strategyIdZone').innerHTML = html;
                var form = layui.form;
                form.on('select(strategyId)', function(elem){
                    strategySelect(code,elem.value);
                });
                form.render();
                
                // 在渲染完成后，选择指定的期权日期
                setTimeout(function() {
                    ready = true;
                    tabs.change('strike-list', targetIndex);
                }, 10);
            });
      });
      }
    });
}
// /options/chain/get?code=BABA&market=11&time=1733652854662&strikeTime=2024-12-13
function loadOptionsChain(strikeTime){
    console.log('loadOptionsChain strikeTime:'+ strikeTime+' currentStrategyId:'+currentStrategyId);
    // 更新URL参数
    setUrlParam('strikeTime', strikeTime);
    
    resetContent("loading...");
    $.ajax({
      url: "/options/chain/get",
      data: {
        code: currentCode,
        market: currentMarket,
        strikeTime: strikeTime,
        strategyId: currentStrategyId,
        time: new Date().getTime()
      },
      success: function( response ) {
        if(!response.success){
            layer.msg(response.message);
            return;
        }

        var result = response.data;
        currentPrompt = result.prompt;
        result.currentCode=currentCode;
        
        // 加载财报日历数据并合并到结果中
        if (window.earningsCalendarData && window.earningsCalendarData[currentCode]) {
            result.earningsCalendars = window.earningsCalendarData[currentCode];
        } else {
            result.earningsCalendars = [];
            loadEarningsCalendar(currentCode);
        }
        
        // 缓存股票数据供财报日历使用
        window.currentStockData = result;
        
        var view = document.getElementById('title');
        laytpl(commonInfo.innerHTML).render(result, function(html){
          view.innerHTML = html;
        });

        var convertedData = result.optionsList.map(options => {
            return {
                "LAY_CHECKED": options.strategyData?options.strategyData.recommend:false,
                "data": options,
                "options": JSON.stringify(options),
                "code": options.basic.security.code,
                "type": options.optionExData?(options.optionExData.type == 1 ? 'Call' : 'Put'):'-',//1: call, 2: put
                "group": options.basic.name.match(/^([^ ]+)/)[1],
                "strikePrice": options.optionExData?options.optionExData.strikePrice:'-',
                "range": options.strategyData&&options.strategyData.range?options.strategyData.range + '%' : '-',
                "curPrice": options.realtimeData?options.realtimeData.curPrice:'-',
                "recommendLevel": options.strategyData?options.strategyData.recommendLevel:'-',
                "sellAnnualYield": options.strategyData?options.strategyData.sellAnnualYield + '%' : '-',
                "sellRecommend": options.strategyData?options.strategyData.recommend : false,
            };
        });

        layui.use('table', function(){
          var table = layui.table;
          var inst = table.render({
            elem: '#result',
            cols: [[
              {field: 'type', title: '类型', width: 60, templet: function(d) {
                  if(d.type === 'Call') {
                      return '<span class="option-call-badge">Call</span>';
                  } else if(d.type === 'Put') {
                      return '<span class="option-put-badge">Put</span>';
                  } else {
                      return d.type;
                  }
              }},
              {field: 'code', title: '期权代码', width: 165},
              {field: 'strikePrice', title: '行权价', width: 80, templet: function(d) {
                  var currentPrice = parseFloat(d.data.realtimeData.underlyingPrice);
                  var strikePrice = parseFloat(d.strikePrice);
                  if (!isNaN(currentPrice) && !isNaN(strikePrice)) {
                      if (d.type === 'Call' && strikePrice <= currentPrice || 
                          d.type === 'Put' && strikePrice >= currentPrice) {
                          return '<span class="option-strike-in">' + d.strikePrice + '</span>';
                      } else {
                          return '<span class="option-strike-out">' + d.strikePrice + '</span>';
                      }
                  }
                  return d.strikePrice;
              }},
              {field: 'range', title: '涨跌幅', width: 80},
              {field: 'curPrice', title: '当前价格', width: 110},
              {field: 'sellAnnualYield', title: '年化', width: 80},
              {title: '交易参考信息', width: 500, templet: '#TPL-table-tradeInfo'},
              {field: 'group', title: '分组', width: 80},
              {field: 'options', title: '卖出', width: 80, templet: function(d) {
                  if (d.options) {
                      var btnClass = 'layui-btn-primary';
                      if (d.type === 'Call') {
                          btnClass = 'layui-bg-cyan';
                      } else if (d.type === 'Put') {
                          btnClass = 'layui-bg-red';
                      }
                      return `<div><a title="${d.data.basic.name}" class="layui-btn ${btnClass} layui-btn-xs" onclick='sell(${d.options})' lay-event="sell">卖出${d.type}</a></div>`;
                  }
                  return '';
              }},
            ]],
            data: convertedData,
            toolbar: true,
            lineStyle: 'height: 100%;',
            defaultToolbar: [
              'filter', // 列筛选
              'exports', // 导出
              'print' // 打印
            ],
            done: function(res) {
                // 为表格行添加样式区分Call和Put
                $('.layui-table-body .layui-table tr').each(function() {
                    var type = $(this).find('td[data-field="type"]').text();
                    if (type.indexOf('Call') > -1) {
                        $(this).addClass('layui-table-call');
                    } else if (type.indexOf('Put') > -1) {
                        $(this).addClass('layui-table-put');
                    }
                    
                    // 为价格添加颜色
                    var priceCell = $(this).find('td[data-field="curPrice"]');
                    var price = parseFloat(priceCell.text());
                    if (!isNaN(price)) {
                        if (price > 0) {
                            if (type.indexOf('Call') > -1) {
                                priceCell.addClass('option-price-up');
                            } else if (type.indexOf('Put') > -1) {
                                priceCell.addClass('option-price-down');
                            }
                        }
                    }
                });
                
            },
            //skin: 'line',
            //even: true,
            height: 'full-320',
            lineStyle: 'height: 100%;',
            initSort: {
              field: 'strikePrice',
              type: 'asc'
            },
            page: false,
            limits: [100, 200, 500],
            limit: 100
          });
        });

      }
    });
}

//owner: $("#owner").val(),
function trade(side, options, orderBook){
    if(!currentStrategyId){
        layer.msg('请先选择策略！');
        return;
    }
    // 渲染当前证券策略列表
    var currentStrategyList = filterStrategyByCode(currentOwnerData.strategyList,currentCode);
    var selectStrategyArr = filterStrategyById(currentStrategyList, currentStrategyId);
    if(!selectStrategyArr || selectStrategyArr.length != 1){
        layer.msg('无法匹配策略！');
        return;
    }

    // 创建表单内容
    var formContent = `
        <div class="layui-form" style="padding: 20px;">
            <div class="layui-form-item">
                <label class="layui-form-label">交易策略</label>
                <div class="layui-input-block">
                    <select name="strategyId" lay-filter="formStrategySelect" lay-verify="required">
                        ${currentStrategyList.map(strategy => `<option value="${strategy.strategyId}" ${strategy.strategyId === currentStrategyId ? 'selected' : ''}>${strategy.strategyName}</option>`).join('')}
                    </select>
                </div>
            </div>
            <div class="layui-form-item">
                <label class="layui-form-label">期权代码</label>
                <div class="layui-input-block">
                    <input type="text" class="layui-input layui-bg-gray" value="${options.basic.security.code}" readonly>
                </div>
            </div>
            <div class="layui-form-item">
                <label class="layui-form-label">行权价</label>
                <div class="layui-input-block">
                    <input type="text" class="layui-input layui-bg-gray" value="${options.optionExData ? options.optionExData.strikePrice : '-'}" readonly>
                </div>
            </div>
            <div class="layui-form-item">
                <label class="layui-form-label">当前价格</label>
                <div class="layui-input-block">
                    <input type="text" class="layui-input layui-bg-gray" value="${options.realtimeData ? options.realtimeData.curPrice : '-'}" readonly>
                </div>
            </div>
            <div class="layui-form-item">
                <div class="layui-inline" style="margin-right: 0;">
                    <label class="layui-form-label">Ask价</label>
                    <div class="layui-input-inline" style="width: 100px;">
                        <input type="text" class="layui-input layui-bg-gray" value="${orderBook.askList}" readonly>
                    </div>
                </div>
                <div class="layui-inline">
                    <label class="layui-form-label" style="width: 80px;">Bid价</label>
                    <div class="layui-input-inline" style="width: 100px;">
                        <input type="text" class="layui-input layui-bg-gray" value="${orderBook.bidList}" readonly>
                    </div>
                </div>
            </div>
            <div class="layui-form-item">
                <label class="layui-form-label">卖出份数</label>
                <div class="layui-input-block">
                    <input type="number" name="quantity" class="layui-input" value="1" min="1">
                </div>
            </div>
            <div class="layui-form-item">
                <label class="layui-form-label">卖出价格</label>
                <div class="layui-input-block">
                    <input type="number" name="price" class="layui-input" value="${options.realtimeData.curPrice}" step="0.01">
                </div>
            </div>
            <div class="layui-form-item">
                <label class="layui-form-label">创建平仓单</label>
                <div class="layui-input-block">
                    <input type="checkbox" name="closeOrderEnabled" lay-skin="switch" lay-text="是|否" lay-filter="toggleCloseOrder">
                </div>
            </div>
            <div class="layui-form-item" id="closeOrderTimeItem" style="display:none;">
                <label class="layui-form-label">截止时间</label>
                <div class="layui-input-block">
                    <input type="text" name="closeOrderTime" id="closeOrderTime" class="layui-input" placeholder="选择平仓单截止时间">
                </div>
            </div>
            <div class="layui-form-item" id="closeOrderProfitRatioItem" style="display:none;">
                <label class="layui-form-label">盈利比例</label>
                <div class="layui-input-block">
                    <div class="layui-input-inline" style="width: 60px;">
                        <input type="number" name="closeOrderProfitRatio" id="closeOrderProfitRatio" class="layui-input" value="80" min="0" max="100">
                    </div>
                    <div class="layui-form-mid layui-word-aux">%</div>
                </div>
            </div>
            <div class="layui-form-item" id="closeOrderPriceItem" style="display:none;">
                <label class="layui-form-label">平仓价格</label>
                <div class="layui-input-block">
                    <input type="number" name="closeOrderPrice" id="closeOrderPrice" class="layui-input" value="" step="0.01">
                </div>
            </div>
        </div>
    `;

    layer.open({
        type: 1,
        title: '确认卖出期权',
        area: ['500px', '650px'],
        content: formContent,
        btn: ['确认卖出', '取消'],
        success: function(layero, index) {
            // 渲染日期选择器
            layui.laydate.render({
                elem: '#closeOrderTime',
                type: 'datetime',
                min: 0,
                format: 'yyyy-MM-dd HH:mm:ss'
            });
            
            // 监听开关切换事件
            layui.form.on('switch(toggleCloseOrder)', function(data){
                var closeOrderTimeItem = layero.find('#closeOrderTimeItem');
                var closeOrderProfitRatioItem = layero.find('#closeOrderProfitRatioItem');
                var closeOrderPriceItem = layero.find('#closeOrderPriceItem');
                if(data.elem.checked) {
                    closeOrderTimeItem.show();
                    closeOrderProfitRatioItem.show();
                    closeOrderPriceItem.show();
                    
                    // 初始化价格
                    updateCloseOrderPrice();
                } else {
                    closeOrderTimeItem.hide();
                    closeOrderProfitRatioItem.hide();
                    closeOrderPriceItem.hide();
                }
            });
            
            // 监听盈利比例变化
            var $profitRatio = layero.find('#closeOrderProfitRatio');
            var $closePrice = layero.find('#closeOrderPrice');
            var $sellPrice = layero.find('input[name="price"]');
            
            function updateCloseOrderPrice() {
                var ratio = parseFloat($profitRatio.val()) / 100;
                var sellPrice = parseFloat($sellPrice.val());
                if (!isNaN(ratio) && !isNaN(sellPrice)) {
                    // 按照盈利比例计算价格: 卖出价格 * (1-盈利比例)
                    var calculatedPrice = (sellPrice * (1-ratio)).toFixed(2);
                    $closePrice.val(calculatedPrice);
                }
            }
            
            $profitRatio.on('input', updateCloseOrderPrice);
            // 当卖出价格变化时也更新平仓价格
            $sellPrice.on('input', updateCloseOrderPrice);
            
            // 重新渲染表单元素以激活开关等控件
            layui.form.render();
        },
        yes: function(index, layero) {
            // 获取表单数据
            var selectedStrategyId = layero.find('select[name="strategyId"]').val();
            var quantity = layero.find('input[name="quantity"]').val();
            var price = layero.find('input[name="price"]').val();
            var closeOrderEnabled = layero.find('input[name="closeOrderEnabled"]').prop('checked');
            var closeOrderTime = layero.find('input[name="closeOrderTime"]').val();
            var closeOrderPrice = layero.find('input[name="closeOrderPrice"]').val();
            
            if (!selectedStrategyId) {
                layer.msg('请选择交易策略');
                return;
            }
            
            if (!quantity || !price) {
                layer.msg('请输入份数和价格');
                return;
            }
            
            if (closeOrderEnabled) {
                if (!closeOrderTime) {
                    layer.msg('请选择平仓单截止时间');
                    return;
                }
                
                if (!closeOrderPrice || closeOrderPrice <= 0) {
                    layer.msg('请输入有效的平仓价格');
                    return;
                }
            }

            // 二次确认对话框
            layer.confirm(`确定要使用 <strong>${filterStrategyById(currentOwnerData.strategyList, selectedStrategyId)[0].strategyName}</strong> 策略卖出 <strong>${options.basic.security.code}</strong> 期权吗？`, {
                icon: 3,
                title: '策略确认',
                btn: ['确定卖出','再检查一下']
            }, function(confirmIndex){
                // 准备平仓单数据
                var closeOrderJob = null;
                if (closeOrderEnabled) {
                    closeOrderJob = {
                        enabled: true,
                        cannelTime: closeOrderTime,
                        price: closeOrderPrice
                    };
                }

                // 下单
                $.ajax({
                    url: "/trade/submit",
                    method: 'POST',
                    data: {
                        password: $("#totp").val(),
                        side: side,
                        strategyId: selectedStrategyId,
                        quantity: quantity,
                        price: price,
                        options: JSON.stringify(options),
                        closeOrderJob: closeOrderJob ? JSON.stringify(closeOrderJob) : null
                    },
                    success: function(response) {
                        layer.msg(response.success ? '操作成功' : response.message);
                        layer.close(index);
                        layer.close(confirmIndex);
                    }
                });
            });
        }
    });
}

function sell(options){
    $.ajax({
         url: "/options/orderbook/get",
         method: 'GET',
         data: {
           code: options.basic.security.code,
           market: options.basic.security.market,
           time: new Date().getTime()
         },
         success: function(response) {
            if (response.success) {
                trade(2, options, response.data);
            } else {
                layer.msg(response.message || '获取订单簿失败');
            }
         },
         error: function() {
            layer.msg('网络错误，请重试');
         }
       });
}

function reloadData(){
    // 先获取URL参数
    var urlCode = getUrlParam('code');
    var urlMarket = getUrlParam('market');
    var urlStrikeTime = getUrlParam('strikeTime');
    
    $.ajax({
      url: "/owner/get",
      data: {
        time: new Date().getTime()
      },
      success: function( response ) {
        currentOwnerData = response.data;
        $("#owner").val(currentOwnerData.owner);

        // 使用layui tab
        layui.use(['element', 'util'], function(){
            var element = layui.element;
            var util = layui.util;
            var $ = layui.jquery;
            
            // 清空标的列表
            var securityTab = $("#security");
            securityTab.empty();
            
            // 根据URL参数查找默认标的的索引
            var defaultSecurityIndex = 0;
            var defaultSecurity = null;
            
            // 如果URL中存在code和market参数，查找对应的标的
            if (urlCode && urlMarket) {
                for (var i = 0; i < currentOwnerData.securityList.length; i++) {
                    var security = currentOwnerData.securityList[i];
                    if (security.code == urlCode && security.market == urlMarket) {
                        defaultSecurityIndex = i;
                        defaultSecurity = security;
                        break;
                    }
                }
            }
            
            // 如果没有找到匹配的标的或URL中没有指定标的，则选择第一个标的
            if (!defaultSecurity && currentOwnerData.securityList.length > 0) {
                defaultSecurity = currentOwnerData.securityList[0];
                defaultSecurityIndex = 0;
            }
            
            // 添加标的列表到Tab
            for(var i=0; i<currentOwnerData.securityList.length; i++) {
                var obj = currentOwnerData.securityList[i];
                var isActive = (i == defaultSecurityIndex) ? ' class="layui-this"' : '';
                var unexercisedCount = obj.unexercisedOrders ? obj.unexercisedOrders.length : 0;
                var redDot = unexercisedCount > 0 ? 
                    '<span class="layui-badge-dot layui-bg-red security-badge-dot" data-orders=\'' + JSON.stringify(obj.unexercisedOrders) + '\'></span>' : '';
                
                // 添加Tab标签，设置选中状态
                securityTab.append('<li lay-id="' + obj.code + '" data-code="' + obj.code + '" data-market="' + obj.market + '"' + isActive + '>' + obj.name + obj.code + redDot + '</li>');
            }
            
            // 渲染Tab
            element.render('tab');
            
            // 绑定Tab点击事件
            element.on('tab(security-tabs)', function(){
                var code = $(this).attr('data-code');
                var market = $(this).attr('data-market');
                if(code && market) {
                    loadOptionsExpDate(code, market);
                }
            });
            
            // 加载标的数据
            if (defaultSecurity) {
                // 需要延迟执行，确保Tab已经渲染完成
                loadOptionsExpDate(defaultSecurity.code, defaultSecurity.market, urlStrikeTime);
            }
            
            // 添加鼠标悬停事件
            $('.security-badge-dot').each(function() {
                var $dot = $(this);
                var orders = JSON.parse($dot.attr('data-orders'));
                
                // 处理订单数据，解析ext字段
                orders = orders.map(order => {
                    try {
                        if (order.ext) {
                            let sourceOptions = null;
                            if (typeof order.ext.sourceOptions === 'string') {
                                try {
                                    sourceOptions = JSON.parse(order.ext.sourceOptions);
                                } catch (e) {
                                    console.error('解析sourceOptions字符串失败:', e);
                                }
                            } else if (typeof order.ext.sourceOptions === 'object') {
                                sourceOptions = order.ext.sourceOptions;
                            }
                            
                            // 计算剩余天数
                            const strikeDate = new Date(order.strikeTime);
                            const today = new Date();
                            const curDTE = Math.ceil((strikeDate - today) / (1000 * 60 * 60 * 24));
                            
                            return {
                                ...order,
                                ext: {
                                    ...order.ext,
                                    curDTE: curDTE
                                }
                            };
                        }
                        return order;
                    } catch (e) {
                        console.error('处理订单数据失败:', e);
                        return order;
                    }
                });
                
                layui.use(['layer', 'laytpl'], function(){
                    var layer = layui.layer;
                    var laytpl = layui.laytpl;
                    
                    $dot.hover(function(e){
                        // 使用模板渲染未行权期权信息
                        laytpl(unexercisedOrders.innerHTML).render(orders, function(html){
                            layer.open({
                                type: 1,
                                title: '未行权期权信息',
                                shade: 0,
                                offset: [e.pageY + 10, e.pageX + 10],
                                area: ['400px'],
                                skin: 'layui-layer-molv',
                                content: html,
                                success: function(layero, index){
                                    // 当鼠标离开弹出层和红点时关闭
                                    $(layero).add($dot).one('mouseleave', function(){
                                        setTimeout(function(){
                                            if(!$(layero).is(':hover') && !$dot.is(':hover')){
                                                layer.close(index);
                                            }
                                        }, 100);
                                    });
                                }
                            });
                        });
                    }, function(){
                        // 鼠标离开红点时，不立即关闭，让用户有机会移动到弹出层上
                        setTimeout(function(){
                            if(!$('.layui-layer').is(':hover')){
                                layer.closeAll();
                            }
                        }, 100);
                    });
                });
            });
        });
      }
    });
}

reloadData();

let clientId = "realtime_data_" + new Date().getTime();
let source = null;
if (window.EventSource) {
    // 连接的建立
    source = new EventSource("/connect?requestId=" + clientId);

    source.addEventListener('open', function (e) {
        console.log("sse open.")
    }, false);

    source.addEventListener("message", function (e) {
        content = JSON.parse(e.data);
        if(content.data.nyc_time){
            document.getElementById("nyc_time").innerHTML = content.data.nyc_time;
        }
        if(content.data.stock_price){
            var currentData = content.data.stock_price;
            var priceEle = document.getElementById("stock_"+currentData.security.market+'_'+currentData.security.code);
            if(priceEle){
                priceEle.innerHTML = currentData.lastDone;
            }
        }
    });

    source.addEventListener('error', function (e) {
        if (e.readyState === EventSource.CLOSED) {
            console.log("sse close.")
        } else {
            console.log(e);
        }
    }, false);

}

// 监听窗口关闭事件，主动去关闭连接
window.onbeforeunload = function () {
    closeSse();
};

// 关闭Sse连接
function closeSse() {
    source.close();
    const httpRequest = new XMLHttpRequest();
    httpRequest.open('GET', '/close?requestId=' + clientId, true);
    httpRequest.send();
    console.log("close");
}

// 加载财报日历信息
function loadEarningsCalendar(code) {
    if (!code) return;
    
    $.ajax({
        url: "/api/earnings-calendar/symbol",
        data: {
            symbol: code,
            time: new Date().getTime()
        },
        success: function(response) {
            if (response.success && response.data) {
                // 存储财报日历数据到全局变量
                if (!window.earningsCalendarData) {
                    window.earningsCalendarData = {};
                }
                window.earningsCalendarData[code] = response.data;
                
                // 如果当前显示的是这个标的，更新显示
                if (currentCode === code) {
                    updateEarningsCalendarDisplay(response.data);
                }
            }
        },
        error: function() {
            console.error('加载财报日历失败:', code);
        }
    });
}

// 加载财报日历数据
function loadEarningsCalendar(symbol) {
    if (!symbol) return;
    
    // 检查缓存
    if (window.earningsCalendarData && window.earningsCalendarData[symbol]) {
        updateEarningsCalendarDisplay(symbol, window.earningsCalendarData[symbol]);
        return;
    }
    
    $.ajax({
        url: "/api/earnings-calendar/symbol",
        data: {
            symbol: symbol,
            time: new Date().getTime()
        },
        success: function(response) {
            if (response.success && response.data) {
                // 缓存数据
                if (!window.earningsCalendarData) {
                    window.earningsCalendarData = {};
                }
                window.earningsCalendarData[symbol] = response.data;
                
                updateEarningsCalendarDisplay(symbol, response.data);
            } else {
                console.log('加载财报日历数据失败:', response.message);
            }
        },
        error: function(xhr, status, error) {
            console.error('加载财报日历数据出错:', error);
        }
    });
}

// 更新财报日历显示
function updateEarningsCalendarDisplay(symbol, data) {
    if (!data || data.length === 0) return;
    
    // 获取当前展示的数据
    var currentResult = {
        currentCode: symbol,
        earningsCalendars: data,
        stockIndicator: window.currentStockData ? window.currentStockData.stockIndicator : {}
    };
    
    // 更新commonInfo区域
    var view = document.getElementById('title');
    laytpl(commonInfo.innerHTML).render(currentResult, function(html){
        view.innerHTML = html;
    });
}

// 显示财报日历详情弹窗
function showEarningsCalendar(symbol) {
    if (!symbol) return;
    
    // 获取数据
    var data = window.earningsCalendarData ? window.earningsCalendarData[symbol] : [];
    if (!data || data.length === 0) {
        // 如果缓存中没有数据，重新加载
        loadEarningsCalendar(symbol);
        data = window.earningsCalendarData ? window.earningsCalendarData[symbol] : [];
        if (!data || data.length === 0) {
            layer.msg('暂无财报日历信息');
            return;
        }
    }
    
    renderEarningsCalendarModal(symbol, data);
}

// 渲染财报日历弹窗
function renderEarningsCalendarModal(symbol, data) {
    var html = `
        <div class="layui-card">
            <div class="layui-card-header">
                <h3>${symbol} 财报日历</h3>
            </div>
            <div class="layui-card-body">
                <table class="layui-table" lay-even>
                    <thead>
                        <tr>
                            <th>公司名称</th>
                            <th>财报日期</th>
                            <th>发布时间</th>
                            <th>预期EPS</th>
                            <th>去年EPS</th>
                            <th>市值</th>
                        </tr>
                    </thead>
                    <tbody>
    `;
    
    data.forEach(function(item) {
        html += `
            <tr>
                <td>${item.name || symbol}</td>
                <td>${layui.util.toDateString(new Date(item.earningsDate), 'yyyy-MM-dd')}</td>
                <td>${item.time || 'TAS'}</td>
                <td>${item.epsForecast || '-'}</td>
                <td>${item.lastYearEps || '-'}</td>
                <td>${item.marketCap ? (item.marketCap / 1000000000).toFixed(2) + 'B' : '-'}</td>
            </tr>
        `;
    });
    
    html += `
                    </tbody>
                </table>
            </div>
        </div>
    `;
    
    layer.open({
        type: 1,
        title: symbol + ' 财报日历详情',
        area: ['800px', '600px'],
        content: html,
        shadeClose: true,
        btn: ['关闭']
    });
}
