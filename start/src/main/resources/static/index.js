//JS
var currentStrategyId;
var currentCode;
var currentMarket;
var currentAiPrompt;
var currentOwnerData;

function filterStrategyByCode(jsonArray, code) {
  return jsonArray.filter(item => item.code === code);
}
function filterStrategyById(jsonArray, strategyId) {
  return jsonArray.filter(item => item.strategyId === strategyId);
}
function strategySelect(code,strategyId){
    currentStrategyId = strategyId;
    localStorage.setItem(code+'_strategyId', currentStrategyId);
}
function strategyLoad(code){
    currentStrategyId = localStorage.getItem(code+'_strategyId');
    console.log('strategyLoad code:'+ code+' currentStrategyId:'+currentStrategyId);
}

function loadOptionsExpDate(code, market){
    currentCode = code;
    currentMarket = market;
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
        var result = response.data;
        var output = document.getElementById("strike-list");
        output.innerHTML = "";
        var moreHtml = ""
        for(var i=0; i<result.length; i++) {
            var obj = result[i];
            if (i < 8){
                output.innerHTML += '<li class="layui-nav-item layui-hide-xs" onclick="loadOptionsChain(\''+obj.strikeTime+'\',\''+obj.strikeTimestamp+'\',\''+obj.optionExpiryDateDistance+'\')"><a href="javascript:;">'+obj.strikeTime+'('+obj.optionExpiryDateDistance+')</a></li>'
            }else{
                moreHtml += '<dd onclick="loadOptionsChain(\''+obj.strikeTime+'\',\''+obj.strikeTimestamp+'\',\''+obj.optionExpiryDateDistance+'\')"><a href="javascript:;">'+obj.strikeTime+'('+obj.optionExpiryDateDistance+')</a></dd>'
            }
        }
        output.innerHTML += '<li class="layui-nav-item"><a href="javascript:;">More</a><dl class="layui-nav-child">' + moreHtml + '</dl></li>';
        render();

        // 渲染当前证券策略列表
        var currentStrategyList = filterStrategyByCode(currentOwnerData.strategyList,currentCode);
        laytpl(currentStrategy.innerHTML).render({list:currentStrategyList,strategyId:currentStrategyId}, function(html){
          document.getElementById('strategyIdZone').innerHTML = html;
          var form = layui.form;
          form.on('select(strategyId)', function(elem){
            strategySelect(code,elem.value);
          });
          form.render();
        });
      }
    });
}
// /options/chain/get?code=BABA&market=11&time=1733652854662&strikeTime=2024-12-13&strikeTimestamp=1734066000&optionExpiryDateDistance=5
function loadOptionsChain(strikeTime, strikeTimestamp, optionExpiryDateDistance){
    currentAiPrompt = "";
    console.log('loadOptionsChain strikeTime:'+ strikeTime+' optionExpiryDateDistance:'+ optionExpiryDateDistance+' currentStrategyId:'+currentStrategyId);
    document.getElementById("title").innerHTML = "loading...";
    $.ajax({
      url: "/options/chain/get",
      data: {
        code: currentCode,
        market: currentMarket,
        strikeTime: strikeTime,
        strikeTimestamp: strikeTimestamp,
        optionExpiryDateDistance: optionExpiryDateDistance,
        strategyId: currentStrategyId,
        time: new Date().getTime()
      },
      success: function( response ) {
        var result = response.data;
        currentAiPrompt = result.aiPrompt;
        result.currentCode=currentCode;
        result.optionExpiryDateDistance=optionExpiryDateDistance
        var view = document.getElementById('title');
        laytpl(commonInfo.innerHTML).render(result, function(html){
          view.innerHTML = html;
        });

        var convertedData = result.optionList.map(item => {
            return {
                "LAY_CHECKED": item.call&&item.call.strategyData?item.call.strategyData.recommend:(item.put&&item.put.strategyData?item.put.strategyData.recommend:false),
                "data": item.call?item.call:item.put,
                "callObj": item.call,
                "putObj": item.put,
                "group": item.call?item.call.basic.name.match(/^([^ ]+)/)[1]:item.put.basic.name.match(/^([^ ]+)/)[1],
                "strikePrice": item.call?item.call.optionExData.strikePrice:item.put.optionExData.strikePrice,
                "call": item.call?JSON.stringify(item.call):null,
                "put": item.put?JSON.stringify(item.put):null,
                "putCurPrice": item.put && item.put.realtimeData?item.put.realtimeData.curPrice:'-',
                "callCurPrice": item.call && item.call.realtimeData?item.call.realtimeData.curPrice:'-',
                "putSellAnnualYield": item.put && item.put.strategyData?item.put.strategyData.sellAnnualYield + '%' : '-',
                "callSellAnnualYield": item.call && item.call.strategyData?item.call.strategyData.sellAnnualYield + '%' : '-',
                "putSellRecommend": item.put && item.put.strategyData?item.put.strategyData.recommend : false,
                "callSellRecommend": item.call && item.call.strategyData?item.call.strategyData.recommend : false,
            };
        });

        layui.use('table', function(){
          var table = layui.table;
          var inst = table.render({
            elem: '#result',
            cols: [[
              {field: 'data', title: '推荐-涨跌', width: 100, align: 'left', templet: '#TPL-recommendInfo'},
              {title: '交易参考信息', width: 260, rowspan: 3, templet: '#id-table-call-info'},
              {field: 'callCurPrice', title: '价格', width: 85},
              {field: 'call', title: '卖', width: 20, templet: '{{#  if(d.call){ }}<div><a title="{{= d.callObj.basic.name }}" class="layui-btn layui-btn-primary layui-btn-xs" onclick="sell({{= d.call }})" lay-event="sell">卖</a></div>{{#  } }}'},
              {field: 'callSellAnnualYield', title: '年化', width: 85},
              {field: 'strikePrice', title: '行权价', width: 90, sort: true},
              {field: 'putSellAnnualYield', title: '年化', width: 85},
              {field: 'put', title: '卖', width: 20, templet: '{{#  if(d.put){ }}<div><a title="{{= d.putObj.basic.name }}" class="layui-btn layui-btn-primary layui-btn-xs" onclick="sell({{= d.put }})" lay-event="sell">卖</a></div>{{#  } }}'},
              {field: 'putCurPrice', title: '价格', width: 85},
              {title: '交易参考信息', width: 260, templet: '#id-table-put-info'},
            ]],
            data: convertedData,
            toolbar: true,
            lineStyle: 'height: 100%;',
            defaultToolbar: [
              'filter', // 列筛选
              'exports', // 导出
              'print' // 打印
            ],
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
    var selectStrategyArr = filterStrategyById(currentOwnerData.strategyList, currentStrategyId);
    if(!selectStrategyArr || selectStrategyArr.length != 1){
        layer.msg('无法匹配策略！');
        return;
    }

    layer.confirm('确认交易策略名称：'+selectStrategyArr[0].strategyName, {
            btn: ['确定', '取消'] //按钮
          }, function(){
            // 确认
            layer.prompt({title: '请输入卖出份数', value: 1}, function(value, index, elem){
                if(value === ''){
                    return elem.focus();
                }
                var quantity = util.escape(value);
                layer.close(index);
                layer.prompt({title: '请输入卖出价格（ask:'+orderBook.askList+' bid:'+orderBook.bidList+'）', value: options.realtimeData.curPrice}, function(value, index, elem){
                    if(value === ''){
                        return elem.focus();
                    }
                    // 下单
                    var price = util.escape(value);
                    $.ajax({
                      url: "/trade/submit",
                      method: 'POST',
                      data: {
                        password: $("#totp").val(),
                        side: side,
                        strategyId: currentStrategyId,
                        quantity: quantity,
                        price: price,
                        options: JSON.stringify(options),
                      },
                      success: function( response ) {
                        layer.msg(response.success ? '操作成功' : response.message);
                        layer.close(index);
                      }
                    });
                });
            });
       }, function(){
                // 取消
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
         success: function( response ) {
            trade(2, options, response.data);
         }
       });
}

function ai(){
    layer.prompt({title: '提示词', formType: 2, value: currentAiPrompt,area: ['600px', '420px']}, function(value, index, elem){
            if(value === '') return elem.focus();
            layer.msg('获得：'+ util.escape(value)); // 显示 value
            // 关闭 prompt
            layer.close(index);
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

        var output = document.getElementById("security");
        output.innerHTML = "";
        for(var i=0; i<currentOwnerData.securityList.length; i++) {
            var obj = currentOwnerData.securityList[i];
            output.innerHTML += '<dd onclick="loadOptionsExpDate(\''+obj.code+'\',\''+obj.market+'\')"><a href="javascript:;">'+obj.name+obj.code+'</a></dd>'
        }
        render();
      }
    });
}

reloadData();
