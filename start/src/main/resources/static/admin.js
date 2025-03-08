/**
 * 期权交易管理系统 - 管理页面
 */
layui.use(['table', 'form', 'layer', 'util'], function () {
    const table = layui.table;
    const form = layui.form;
    const layer = layui.layer;
    const util = layui.util;

    // 期权标的表格
    const securityTable = table.render({
        elem: '#securityTable',
        url: '/admin/security/list',
        page: true,
        toolbar: true,
        defaultToolbar: ['filter', 'exports', 'print'],
        cols: [[
            {type: 'checkbox'},
            {field: 'id', title: 'ID', sort: true, width: 80},
            {field: 'name', title: '标的名称', width: 120},
            {field: 'code', title: '标的代码', width: 100},
            {field: 'market', title: '市场', width: 80},
            {
                field: 'createTime', title: '创建时间', width: 160, sort: true,
                templet: function (d) {
                    return util.toDateString(d.createTime);
                }
            },
            {
                field: 'status', title: '状态', width: 80,
                templet: function (d) {
                    return d.status === 1 ? 
                        '<span class="layui-badge layui-bg-green">启用</span>' : 
                        '<span class="layui-badge layui-bg-gray">禁用</span>';
                }
            },
            {title: '操作', toolbar: '#securityTableBar', width: 180}
        ]],
        response: {
            statusCode: 0
        },
        parseData: function (res) {
            return {
                "code": res.success ? 0 : 1,
                "msg": res.message,
                "count": res.data ? res.data.length : 0,
                "data": res.data
            };
        }
    });

    // 期权策略表格
    const strategyTable = table.render({
        elem: '#strategyTable',
        url: '/admin/strategy/list',
        page: true,
        toolbar: true,
        defaultToolbar: ['filter', 'exports', 'print'],
        cols: [[
            {type: 'checkbox'},
            {field: 'id', title: 'ID', sort: true, width: 80},
            {field: 'strategyName', title: '策略名称', width: 150},
            {field: 'strategyCode', title: '策略代码', width: 100},
            {field: 'stage', title: '策略阶段', width: 100},
            {field: 'code', title: '标的代码', width: 100},
            {field: 'lotSize', title: '合约股数', width: 100},
            {
                field: 'startTime', title: '开始时间', width: 160, sort: true,
                templet: function (d) {
                    return util.toDateString(d.startTime);
                }
            },
            {
                field: 'status', title: '状态', width: 80,
                templet: function (d) {
                    return d.status === 1 ? 
                        '<span class="layui-badge layui-bg-green">启用</span>' : 
                        '<span class="layui-badge layui-bg-gray">禁用</span>';
                }
            },
            {title: '操作', toolbar: '#strategyTableBar', width: 180}
        ]],
        response: {
            statusCode: 0
        },
        parseData: function (res) {
            return {
                "code": res.success ? 0 : 1,
                "msg": res.message,
                "count": res.data ? res.data.length : 0,
                "data": res.data
            };
        }
    });

    // 加载标的列表到策略表单的下拉框
    function loadSecuritiesForSelect() {
        $.ajax({
            url: '/admin/security/list',
            type: 'GET',
            success: function (res) {
                if (res.success) {
                    let html = '<option value="">请选择标的代码</option>';
                    res.data.forEach(function (item) {
                        if (item.status === 1) {
                            html += '<option value="' + item.code + '">' + item.name + ' (' + item.code + ')</option>';
                        }
                    });
                    $('#securitySelect').html(html);
                    form.render('select');
                }
            }
        });
    }

    // 期权标的表格工具条事件
    table.on('tool(securityTable)', function (obj) {
        const data = obj.data;
        if (obj.event === 'edit') {
            // 编辑
            layer.open({
                type: 1,
                title: '编辑期权标的',
                area: ['500px', '400px'],
                content: $('#securityFormTpl').html(),
                success: function () {
                    form.val('securityForm', data);
                    form.render();
                }
            });
        } else if (obj.event === 'delete') {
            // 删除
            layer.confirm('确定要删除该期权标的吗？', function (index) {
                $.ajax({
                    url: '/admin/security/status',
                    type: 'POST',
                    data: {
                        id: data.id,
                        status: 0
                    },
                    success: function (res) {
                        if (res.success) {
                            layer.msg('删除成功');
                            securityTable.reload();
                        } else {
                            layer.msg('删除失败：' + res.message);
                        }
                    }
                });
                layer.close(index);
            });
        } else if (obj.event === 'toggle') {
            // 切换状态
            const newStatus = data.status === 1 ? 0 : 1;
            const statusText = newStatus === 1 ? '启用' : '禁用';
            $.ajax({
                url: '/admin/security/status',
                type: 'POST',
                data: {
                    id: data.id,
                    status: newStatus
                },
                success: function (res) {
                    if (res.success) {
                        layer.msg(statusText + '成功');
                        securityTable.reload();
                    } else {
                        layer.msg(statusText + '失败：' + res.message);
                    }
                }
            });
        }
    });

    // 期权策略表格工具条事件
    table.on('tool(strategyTable)', function (obj) {
        const data = obj.data;
        if (obj.event === 'edit') {
            // 编辑
            loadSecuritiesForSelect();
            layer.open({
                type: 1,
                title: '编辑期权策略',
                area: ['500px', '600px'],
                content: $('#strategyFormTpl').html(),
                success: function () {
                    form.val('strategyForm', data);
                    form.render();
                }
            });
        } else if (obj.event === 'delete') {
            // 删除
            layer.confirm('确定要删除该期权策略吗？', function (index) {
                $.ajax({
                    url: '/admin/strategy/status',
                    type: 'POST',
                    data: {
                        id: data.id,
                        status: 0
                    },
                    success: function (res) {
                        if (res.success) {
                            layer.msg('删除成功');
                            strategyTable.reload();
                        } else {
                            layer.msg('删除失败：' + res.message);
                        }
                    }
                });
                layer.close(index);
            });
        } else if (obj.event === 'toggle') {
            // 切换状态
            const newStatus = data.status === 1 ? 0 : 1;
            const statusText = newStatus === 1 ? '启用' : '禁用';
            $.ajax({
                url: '/admin/strategy/status',
                type: 'POST',
                data: {
                    id: data.id,
                    status: newStatus
                },
                success: function (res) {
                    if (res.success) {
                        layer.msg(statusText + '成功');
                        strategyTable.reload();
                    } else {
                        layer.msg(statusText + '失败：' + res.message);
                    }
                }
            });
        }
    });

    // 添加期权标的按钮事件
    $('#addSecurityBtn').click(function () {
        layer.open({
            type: 1,
            title: '添加期权标的',
            area: ['500px', '400px'],
            content: $('#securityFormTpl').html(),
            success: function () {
                form.render();
            }
        });
    });

    // 添加期权策略按钮事件
    $('#addStrategyBtn').click(function () {
        loadSecuritiesForSelect();
        layer.open({
            type: 1,
            title: '添加期权策略',
            area: ['500px', '600px'],
            content: $('#strategyFormTpl').html(),
            success: function () {
                form.render();
            }
        });
    });

    // 刷新期权标的表格按钮事件
    $('#refreshSecurityBtn').click(function () {
        securityTable.reload();
    });

    // 刷新期权策略表格按钮事件
    $('#refreshStrategyBtn').click(function () {
        strategyTable.reload();
    });

    // 批量删除期权标的按钮事件
    $('#deleteSecurityBtn').click(function () {
        const checkStatus = table.checkStatus('securityTable');
        const data = checkStatus.data;
        if (data.length === 0) {
            layer.msg('请选择要删除的期权标的');
            return;
        }
        layer.confirm('确定要删除选中的' + data.length + '个期权标的吗？', function (index) {
            let count = 0;
            let success = 0;
            data.forEach(function (item) {
                $.ajax({
                    url: '/admin/security/status',
                    type: 'POST',
                    async: false,
                    data: {
                        id: item.id,
                        status: 0
                    },
                    success: function (res) {
                        count++;
                        if (res.success) {
                            success++;
                        }
                        if (count === data.length) {
                            layer.msg('成功删除' + success + '个期权标的');
                            securityTable.reload();
                        }
                    }
                });
            });
            layer.close(index);
        });
    });

    // 批量删除期权策略按钮事件
    $('#deleteStrategyBtn').click(function () {
        const checkStatus = table.checkStatus('strategyTable');
        const data = checkStatus.data;
        if (data.length === 0) {
            layer.msg('请选择要删除的期权策略');
            return;
        }
        layer.confirm('确定要删除选中的' + data.length + '个期权策略吗？', function (index) {
            let count = 0;
            let success = 0;
            data.forEach(function (item) {
                $.ajax({
                    url: '/admin/strategy/status',
                    type: 'POST',
                    async: false,
                    data: {
                        id: item.id,
                        status: 0
                    },
                    success: function (res) {
                        count++;
                        if (res.success) {
                            success++;
                        }
                        if (count === data.length) {
                            layer.msg('成功删除' + success + '个期权策略');
                            strategyTable.reload();
                        }
                    }
                });
            });
            layer.close(index);
        });
    });

    // 期权标的表单提交事件
    form.on('submit(securitySubmit)', function (data) {
        $.ajax({
            url: '/admin/security/save',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(data.field),
            success: function (res) {
                if (res.success) {
                    layer.closeAll('page');
                    layer.msg('保存成功');
                    securityTable.reload();
                } else {
                    layer.msg('保存失败：' + res.message);
                }
            }
        });
        return false;
    });

    // 期权策略表单提交事件
    form.on('submit(strategySubmit)', function (data) {
        // 处理ext字段
        data.field.ext = JSON.stringify({});
        
        $.ajax({
            url: '/admin/strategy/save',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(data.field),
            success: function (res) {
                if (res.success) {
                    layer.closeAll('page');
                    layer.msg('保存成功');
                    strategyTable.reload();
                } else {
                    layer.msg('保存失败：' + res.message);
                }
            }
        });
        return false;
    });
}); 