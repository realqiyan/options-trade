var table;
var owner = 'default';
var allFlowData = [];

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
        format: 'yyyy-MM-dd'
    });
    
    renderTable();
});

function renderTable() {
    table.render({
        elem: '#flowTable',
        url: '/api/flow/list',
        method: 'get',
        where: {
            owner: owner
        },
        page: true,
        limit: 10,
        limits: [10, 20, 50, 100],
        cols: [[
            {field: 'platform', title: '平台', width: 100},
            {field: 'clearingDate', title: '清算日期', width: 120, templet: function(d) { return formatDate(d.clearingDate); }},
            {field: 'settlementDate', title: '结算日期', width: 120, templet: function(d) { return formatDate(d.settlementDate); }},
            {field: 'cashflowType', title: '流水类型', width: 180, sort: true},
            {field: 'cashflowDirection', title: '方向', width: 80, templet: function(d) { return d.cashflowDirection == 1 ? '流入' : '流出'; }},
            {field: 'cashflowAmount', title: '金额', width: 120, sort: true},
            {field: 'currency', title: '币种', width: 80},
            {field: 'cashflowRemark', title: '备注', width: 200},
            {fixed: 'right', title: '操作', width: 120, toolbar: '#flowTableBar'}
        ]],
        id: 'flowTableReload',
        done: function(res) {
            allFlowData = res.data;
        }
    });
    
    table.on('tool(flowTable)', function(obj) {
        var data = obj.data;
        if(obj.event === 'detail') {
            showFlowDetail(data);
        }
    });
}

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
                table.reload('flowTableReload');
            } else {
                layer.msg('同步失败: ' + result.message, {icon: 2});
            }
        }
    };
    xhr.send('clearingMonth=' + clearingMonth);
}

function searchFlowSummary() {
    var dateRange = document.getElementById('dateRange').value;
    
    var url = '/api/flow/list';
    var params = {
        dateRange: dateRange
    };
    
    $.get(url, params, function(res) {
        if(res.code == 0) {
            var filteredData = filterByDateRange(res.data, dateRange);
            table.reload('flowTableReload', {
                data: filteredData,
                page: {
                    curr: 1
                }
            });
        } else {
            layer.msg('查询失败: ' + res.message, {icon: 2});
        }
    });
}

function filterByDateRange(data, dateRange) {
    if(!dateRange || !data || data.length == 0) {
        return data;
    }
    
    var dates = dateRange.split(' - ');
    if(dates.length != 2) {
        return data;
    }
    
    var startDate = new Date(dates[0]);
    var endDate = new Date(dates[1]);
    endDate.setHours(23, 59, 59, 999);
    
    return data.filter(function(item) {
        if(!item.clearingDate) {
            return false;
        }
        var clearingDate = new Date(item.clearingDate);
        return clearingDate >= startDate && clearingDate <= endDate;
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
                 '<tr><td>方向</td><td>' + (data.cashflowDirection == 1 ? '流入' : '流出') + '</td></tr>' +
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

function init() {
    document.getElementById('owner').value = owner;
}

window.onload = function() {
    init();
};
