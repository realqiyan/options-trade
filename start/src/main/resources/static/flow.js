var table;
var allFlowData = [];

var flowTableCols = [[
    {field: 'platform', title: '平台', width: 100},
    {field: 'clearingDate', title: '清算日期', width: 120, sort: true, templet: function(d) { return formatDate(d.clearingDate); }},
    {field: 'settlementDate', title: '结算日期', width: 120, sort: true, templet: function(d) { return formatDate(d.settlementDate); }},
    {field: 'cashflowType', title: '流水类型', width: 180, sort: true},
    {field: 'cashflowDirection', title: '方向', width: 80, sort: true, templet: function(d) { return d.cashflowDirection == '1' ? '流入' : '流出'; }},
    {field: 'cashflowAmount', title: '金额', width: 120, sort: true},
    {field: 'currency', title: '币种', width: 80, sort: true, templet: function(d) { return formatCurrency(d.currency); }},
    {field: 'cashflowRemark', title: '备注', width: 600, sort: true},
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
        elem: '#clearingYear',
        type: 'year',
        format: 'yyyy',
        value: new Date()
    });
    
    laydate.render({
        elem: '#dateRange',
        range: true,
        format: 'yyyy-MM-dd',
        value: getMonthRange()
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

function syncFlowSummaryByYear() {
    var clearingYear = document.getElementById('clearingYear').value;
    
    if(!clearingYear) {
        layer.msg('请选择清算年份', {icon: 2});
        return;
    }
    
    layer.confirm('确定要同步 ' + clearingYear + ' 年的所有资金流水吗？此操作可能需要较长时间。', {icon: 3, title: '确认'}, function(index) {
        layer.close(index);
        layer.load(2);
        
        var xhr = new XMLHttpRequest();
        xhr.open('POST', '/api/flow/syncByYear', true);
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
        xhr.send('year=' + clearingYear);
    });
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
        toolbar: true,
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

function formatCurrency(currency) {
    if(!currency) return '';
    if(currency == '1') return '港币';
    if(currency == '2') return '美元';
    return currency;
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
        
        if(item.cashflowDirection == '1') {
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
                      '<td>' + formatCurrency(summary.currency) + '</td>';
        tbody.appendChild(tr);
    }
    
    document.getElementById('summaryContainer').style.display = 'block';
}
