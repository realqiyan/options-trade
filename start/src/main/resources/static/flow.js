var table;
var allFlowData = [];

var flowTableCols = [[
    {field: 'platform', title: '平台', width: 100},
    {field: 'clearingDate', title: '清算日期', width: 120, templet: function(d) { return formatDate(d.clearingDate); }},
    {field: 'settlementDate', title: '结算日期', width: 120, templet: function(d) { return formatDate(d.settlementDate); }},
    {field: 'cashflowType', title: '流水类型', width: 180, sort: true},
    {field: 'cashflowDirection', title: '方向', width: 80, templet: function(d) { return d.cashflowDirection == 'IN' ? '流入' : '流出'; }},
    {field: 'cashflowAmount', title: '金额', width: 120, sort: true},
    {field: 'currency', title: '币种', width: 80},
    {field: 'cashflowRemark', title: '备注', width: 200},
    {fixed: 'right', title: '操作', width: 120, toolbar: '#flowTableBar'}
]];

layui.use(['table', 'layer', 'form', 'laydate'], function(){
    table = layui.table;
    var layer = layui.layer;
    var form = layui.form;
    var laydate = layui.laydate;
    
    laydate.render({
        elem: '#clearingMonth',
        type: 'month',
        format: 'yyyy-MM',
        value: new Date()
    });
    
    laydate.render({
        elem: '#dateRange',
        range: true,
        format: 'yyyy-MM-dd',
        value: getMonthRange()
    });
    
    table.on('tool(flowTable)', function(obj) {
        var data = obj.data;
        if(obj.event === 'detail') {
            showFlowDetail(data);
        }
    });
});


function syncFlowSummary() {
    var clearingMonth = document.getElementById('clearingMonth').value;
    
    if(!clearingMonth) {
        layer.msg('请选择清算月份', {icon: 2});
        return;
    }
    
    layer.load(2);
    
    var xhr = new XMLHttpRequest();
    xhr.open('POST', '/api/flow/sync', true);
    xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
    xhr.onreadystatechange = function() {
        if(xhr.readyState == 4) {
            layer.closeAll('loading');
            var result = JSON.parse(xhr.responseText);
            if(result.code == 0) {
                layer.msg('同步成功，共同步 ' + result.data + ' 条流水', {icon: 1});
                table.reload('flowTable');
            } else {
                layer.msg('同步失败: ' + result.message, {icon: 2});
            }
        }
    };
    xhr.send('clearingMonth=' + clearingMonth);
}

function validateDateRange() {
    var dateRange = document.getElementById('dateRange').value;
    if(!dateRange || dateRange.trim() === '') {
        layer.msg('请选择日期范围', {icon: 2});
        return false;
    }
    return dateRange;
}

function loadSummaryData(dateRange) {
    $.get('/api/flow/list', {dateRange: dateRange}, function(result) {
        if(result.code == 0) {
            calculateAndDisplaySummary(result.data);
        }
    });
}

function searchFlowSummary() {
    var dateRange = validateDateRange();
    if(!dateRange) return;
    
    table.render({
        elem: '#flowTable',
        url: '/api/flow/list',
        method: 'get',
        where: {
            dateRange: dateRange
        },
        parseData: function(res) {
            return {
                "code": res.code,
                "msg": res.message,
                "count": res.data ? res.data.length : 0,
                "data": res.data
            };
        },
        page: true,
        
        limit: 500,
        limits: [500, 1000],
        cols: flowTableCols,
        defaultToolbar: [
          'filter', 'exports', 'print'
        ],
        id: 'flowTable',
        done: function(res) {
            allFlowData = res.data;
            if(dateRange && dateRange.trim() !== '') {
                loadSummaryData(dateRange);
            }
        }
    });
}

function showFlowDetail(data) {
    layer.open({
        type: 1,
        title: '资金流水详情',
        area: ['600px', '400px'],
        content: '<div style="padding: 20px;">' +
                 '<table class="layui-table">' +
                 '<tr><td>ID</td><td>' + data.id + '</td></tr>' +
                 '<tr><td>平台</td><td>' + data.platform + '</td></tr>' +
                 '<tr><td>清算日期</td><td>' + formatDate(data.clearingDate) + '</td></tr>' +
                 '<tr><td>结算日期</td><td>' + formatDate(data.settlementDate) + '</td></tr>' +
                 '<tr><td>流水类型</td><td>' + data.cashflowType + '</td></tr>' +
                 '<tr><td>方向</td><td>' + (data.cashflowDirection == 'IN' ? '流入' : '流出') + '</td></tr>' +
                 '<tr><td>金额</td><td>' + data.cashflowAmount + '</td></tr>' +
                 '<tr><td>币种</td><td>' + data.currency + '</td></tr>' +
                 '<tr><td>备注</td><td>' + data.cashflowRemark + '</td></tr>' +
                 '<tr><td>创建时间</td><td>' + formatDateTime(data.createTime) + '</td></tr>' +
                 '</table></div>'
    });
}

function formatDate(dateStr) {
    if(!dateStr) return '';
    var date = new Date(dateStr);
    return date.getFullYear() + '-' + (date.getMonth() + 1).toString().padStart(2, '0') + '-' + date.getDate().toString().padStart(2, '0');
}

function formatDateTime(dateStr) {
    if(!dateStr) return '';
    var date = new Date(dateStr);
    return date.getFullYear() + '-' + (date.getMonth() + 1).toString().padStart(2, '0') + '-' + date.getDate().toString().padStart(2, '0') + ' ' +
           date.getHours().toString().padStart(2, '0') + ':' + date.getMinutes().toString().padStart(2, '0') + ':' + date.getSeconds().toString().padStart(2, '0');
}

function formatAmount(amount) {
    if(amount === null || amount === undefined) return '0.00';
    return parseFloat(amount).toFixed(2);
}

function getMonthRange() {
    var now = new Date();
    var year = now.getFullYear();
    var month = now.getMonth();
    
    var firstDay = new Date(year, month, 1);
    var lastDay = new Date(year, month + 1, 0);
    
    var firstDayStr = firstDay.getFullYear() + '-' + (firstDay.getMonth() + 1).toString().padStart(2, '0') + '-' + firstDay.getDate().toString().padStart(2, '0');
    var lastDayStr = lastDay.getFullYear() + '-' + (lastDay.getMonth() + 1).toString().padStart(2, '0') + '-' + lastDay.getDate().toString().padStart(2, '0');
    
    return firstDayStr + ' - ' + lastDayStr;
}

function calculateAndDisplaySummary(data) {
    if(!data || data.length == 0) {
        document.getElementById('summaryContainer').style.display = 'none';
        return;
    }
    
    var summaryMap = {};
    
    for(var i = 0; i < data.length; i++) {
        var item = data[i];
        var key = item.cashflowType + '_' + item.currency;
        
        if(!summaryMap[key]) {
            summaryMap[key] = {
                cashflowType: item.cashflowType,
                currency: item.currency,
                inflow: 0,
                outflow: 0
            };
        }
        
        if(item.cashflowDirection == 'IN') {
            summaryMap[key].inflow += item.cashflowAmount;
        } else {
            summaryMap[key].outflow += item.cashflowAmount;
        }
    }
    
    var tbody = document.getElementById('summaryTableBody');
    tbody.innerHTML = '';
    
    var sortedKeys = Object.keys(summaryMap).sort();
    
    for(var i = 0; i < sortedKeys.length; i++) {
        var key = sortedKeys[i];
        var summary = summaryMap[key];
        var netAmount = summary.inflow - summary.outflow;
        
        var tr = document.createElement('tr');
        tr.innerHTML = '<td>' + summary.cashflowType + '</td>' +
                      '<td>' + formatAmount(summary.inflow) + '</td>' +
                      '<td>' + formatAmount(summary.outflow) + '</td>' +
                      '<td>' + formatAmount(netAmount) + '</td>' +
                      '<td>' + summary.currency + '</td>';
        tbody.appendChild(tr);
    }
    
    document.getElementById('summaryContainer').style.display = 'block';
}
