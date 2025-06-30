/**
 * 期权交易管理系统 - 管理页面
 */
layui.use(['table', 'form', 'layer', 'util', 'element'], function () {
    const table = layui.table;
    const form = layui.form;
    const layer = layui.layer;
    const util = layui.util;
    const element = layui.element;

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
                field: 'createTime', title: '创建时间', width: 200, sort: true,
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
            {
                field: 'totalRatio', 
                title: '总规模占比', 
                width: 100, 
                templet: function(d){
                    return (d.totalRatio * 100).toFixed(1) + '%';
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
    let strategyTable;
    
    // 账户表格
    let accountTable;
    
    // 初始化策略表格
    function initStrategyTable() {
        strategyTable = table.render({
            elem: '#strategyTable',
            url: '/admin/strategy/list',
            page: true,
            toolbar: true,
            defaultToolbar: ['filter', 'exports', 'print'],
            cols: [[
                {type: 'checkbox'},
                {field: 'id', title: 'ID', sort: true, width: 50},
                {field: 'strategyName', title: '策略名称', width: 200},
                {field: 'strategyCode', title: '策略代码', width: 160},
                {field: 'stage', title: '策略阶段', width: 100},
                {field: 'code', title: '标的代码', width: 100},
                {field: 'lotSize', title: '合约股数', width: 100},
                {
                    field: 'startTime', title: '开始时间', width: 180, sort: true,
                    templet: function (d) {
                        return util.toDateString(d.startTime);
                    }
                },
                {
                    field: 'ext', title: '扩展配置', width: 300, templet: function(d) {
                        var extStr = '';
                        if (d.ext && typeof d.ext === 'object') {
                            if (d.ext.initial_stock_num) {
                                extStr += '初始股票数:' + d.ext.initial_stock_num+'; ';
                            }
                            if (d.ext.initial_stock_cost) {
                                extStr += '成本价:' + d.ext.initial_stock_cost+'; ';
                            }
                            if (d.ext.wheel_sellput_strike_price) {
                                extStr += 'SellPut行权价:' + d.ext.wheel_sellput_strike_price+'; ';
                            }
                        }
                        return extStr;
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
    }

    // 初始化账户表格
    function initAccountTable() {
        accountTable = table.render({
            elem: '#accountTable',
            url: '/admin/account/list',
            page: true,
            toolbar: true,
            defaultToolbar: ['filter', 'exports', 'print'],
            cols: [[
                {type: 'checkbox'},
                {field: 'id', title: 'ID', sort: true, width: 80},
                {field: 'platform', title: '平台', width: 100},
                {field: 'market', title: '市场', width: 80},
                {field: 'accountId', title: '账号', width: 200},
                {
                    field: 'createTime', title: '创建时间', width: 200, sort: true,
                    templet: function (d) {
                        return util.toDateString(d.createTime);
                    }
                },
                {title: '操作', toolbar: '#accountTableBar', width: 150}
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
    }

    // 监听选项卡切换事件
    element.on('tab(adminTab)', function(data){
        if(data.index === 1) {
            // 切换到策略管理选项卡
            if(!strategyTable) {
                initStrategyTable();
            } else {
                strategyTable.reload();
            }
        } else if(data.index === 0) {
            // 切换到标的管理选项卡
            securityTable.reload();
        } else if(data.index === 2) {
            // 切换到账户管理选项卡
            if(!accountTable) {
                initAccountTable();
            } else {
                accountTable.reload();
            }
        }
    });

    // 初始化策略表格
    initStrategyTable();
    
    // 初始化账户表格
    initAccountTable();

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
                success: function (layero, index) {
                    form.val('securityForm', data);
                    form.render();
                    
                    // 监听表单提交事件
                    form.on('submit(securitySubmit)', function (data) {
                        $.ajax({
                            url: '/admin/security/save',
                            type: 'POST',
                            contentType: 'application/json',
                            data: JSON.stringify(data.field),
                            success: function (res) {
                                if (res.success) {
                                    layer.close(index);
                                    layer.msg('保存成功');
                                    securityTable.reload();
                                } else {
                                    layer.msg('保存失败：' + res.message);
                                }
                            }
                        });
                        return false;
                    });
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
            layer.open({
                type: 1,
                title: '编辑期权策略',
                area: ['500px', '600px'],
                content: $('#strategyFormTpl').html(),
                success: function (layero, index) {
                    // 先渲染表单
                    form.render();
                    
                    // 加载标的列表
                    $.ajax({
                        url: '/admin/security/list',
                        type: 'GET',
                        success: function (res) {
                            if (res.success) {
                                var securityList = res.data;
                                var securitySelect = $('#securitySelect');
                                securitySelect.empty();
                                securitySelect.append('<option value="">请选择标的代码</option>');
                                $.each(securityList, function (index, item) {
                                    securitySelect.append('<option value="' + item.code + '">' + item.code + ' - ' + item.name + '</option>');
                                });
                                
                                // 设置表单值
                                form.val('strategyForm', data);
                                
                                // 处理ext字段中的wheel_sellput_strike_price
                                if (data.ext && typeof data.ext === 'object' && data.ext.wheel_sellput_strike_price) {
                                    $('input[name="sellPutStrikePrice"]').val(data.ext.wheel_sellput_strike_price);
                                }
                                
                                // 处理ext字段中的initial_stock_num
                                if (data.ext && typeof data.ext === 'object' && data.ext.initial_stock_num) {
                                    $('input[name="initialStockNum"]').val(data.ext.initial_stock_num);
                                }
                                
                                // 处理ext字段中的initial_stock_cost
                                if (data.ext && typeof data.ext === 'object' && data.ext.initial_stock_cost) {
                                    $('input[name="initialStockCost"]').val(data.ext.initial_stock_cost);
                                }
                                
                                // 根据策略代码显示或隐藏特定配置
                                toggleStrategyConfig(data.strategyCode);
                                
                                form.render('select');
                            } else {
                                layer.msg('加载标的列表失败：' + res.message, {icon: 2});
                                // 仍然设置表单值
                                form.val('strategyForm', data);
                            }
                        },
                        error: function(xhr, status, error) {
                            console.error('加载标的列表请求失败：', error);
                            layer.msg('加载标的列表请求失败，请检查网络连接', {icon: 2});
                            // 仍然设置表单值
                            form.val('strategyForm', data);
                        }
                    });
                    
                    // 监听策略代码选择变化
                    form.on('select(strategyCode)', function(data){
                        toggleStrategyConfig(data.value);
                    });
                    
                    // 根据策略代码显示或隐藏特定配置
                    function toggleStrategyConfig(strategyCode) {
                        if (strategyCode === 'wheel_strategy') {
                            $('#wheelStrategyConfig').show();
                            
                            // 获取当前表单数据
                            var formData = form.val('strategyForm');
                            
                            // 如果有ext数据，尝试从中提取wheel_sellput_strike_price和initial_stock_num
                            if (formData.ext) {
                                var extData;
                                if (typeof formData.ext === 'string') {
                                    try {
                                        extData = JSON.parse(formData.ext);
                                    } catch (e) {
                                        console.error('解析ext数据失败', e);
                                        extData = {};
                                    }
                                } else {
                                    extData = formData.ext;
                                }
                                
                                if (extData.wheel_sellput_strike_price) {
                                    $('input[name="sellPutStrikePrice"]').val(extData.wheel_sellput_strike_price);
                                }
                                
                                if (extData.initial_stock_num) {
                                    $('input[name="initialStockNum"]').val(extData.initial_stock_num);
                                }
                                
                                if (extData.initial_stock_cost) {
                                    $('input[name="initialStockCost"]').val(extData.initial_stock_cost);
                                }
                            }
                        } else {
                            $('#wheelStrategyConfig').hide();
                            // 清空sellPutStrikePrice字段
                            $('input[name="sellPutStrikePrice"]').val('');
                        }
                    }
                    
                    // 监听表单提交事件
                    form.on('submit(strategySubmit)', function (data) {
                        // 处理ext字段
                        if (!data.field.ext || data.field.ext === '') {
                            data.field.ext = {};
                        } else if (typeof data.field.ext === 'string') {
                            try {
                                data.field.ext = JSON.parse(data.field.ext);
                            } catch (e) {
                                data.field.ext = {};
                            }
                        }
                        
                        // 处理车轮策略特有的配置
                        if (data.field.strategyCode === 'wheel_strategy' && data.field.sellPutStrikePrice) {
                            data.field.ext.wheel_sellput_strike_price = data.field.sellPutStrikePrice;
                        }
                        
                        // 处理通用配置 - 初始股票数
                        if (data.field.initialStockNum) {
                            data.field.ext.initial_stock_num = data.field.initialStockNum;
                        }
                        
                        // 处理通用配置 - 初始股票成本价
                        if (data.field.initialStockCost) {
                            data.field.ext.initial_stock_cost = data.field.initialStockCost;
                        }
                        
                        // 删除临时字段
                        delete data.field.sellPutStrikePrice;
                        delete data.field.initialStockNum;
                        delete data.field.initialStockCost;
                        
                        console.log('提交的数据:', JSON.stringify(data.field));
                        
                        $.ajax({
                            url: '/admin/strategy/save',
                            type: 'POST',
                            contentType: 'application/json',
                            data: JSON.stringify(data.field),
                            success: function (res) {
                                if (res.success) {
                                    layer.close(index);
                                    layer.msg('保存成功');
                                    strategyTable.reload();
                                } else {
                                    layer.msg('保存失败：' + res.message);
                                }
                            },
                            error: function(xhr, status, error) {
                                console.error('保存策略失败:', xhr.responseText);
                                layer.msg('保存策略失败: ' + error);
                            }
                        });
                        return false;
                    });
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
            success: function (layero, index) {
                form.render();
                
                // 监听表单提交事件
                form.on('submit(securitySubmit)', function (data) {
                    $.ajax({
                        url: '/admin/security/save',
                        type: 'POST',
                        contentType: 'application/json',
                        data: JSON.stringify(data.field),
                        success: function (res) {
                            if (res.success) {
                                layer.close(index);
                                layer.msg('保存成功');
                                securityTable.reload();
                            } else {
                                layer.msg('保存失败：' + res.message);
                            }
                        }
                    });
                    return false;
                });
            }
        });
    });

    // 添加期权策略按钮事件
    $('#addStrategyBtn').click(function () {
        layer.open({
            type: 1,
            title: '添加期权策略',
            area: ['500px', '600px'],
            content: $('#strategyFormTpl').html(),
            success: function (layero, index) {
                // 先渲染表单
                form.render();
                
                // 加载标的列表
                $.ajax({
                    url: '/admin/security/list',
                    type: 'GET',
                    success: function (res) {
                        if (res.success) {
                            var securityList = res.data;
                            var securitySelect = $('#securitySelect');
                            securitySelect.empty();
                            securitySelect.append('<option value="">请选择标的代码</option>');
                            $.each(securityList, function (index, item) {
                                securitySelect.append('<option value="' + item.code + '">' + item.code + ' - ' + item.name + '</option>');
                            });
                            form.render('select');
                            
                            // 默认隐藏车轮策略特有配置
                            $('#wheelStrategyConfig').hide();
                            
                            // 监听策略代码选择变化
                            form.on('select(strategyCode)', function(data){
                                toggleStrategyConfig(data.value);
                            });
                            
                            // 根据策略代码显示或隐藏特定配置
                            function toggleStrategyConfig(strategyCode) {
                                if (strategyCode === 'wheel_strategy') {
                                    $('#wheelStrategyConfig').show();
                                } else {
                                    $('#wheelStrategyConfig').hide();
                                    // 清空sellPutStrikePrice字段
                                    $('input[name="sellPutStrikePrice"]').val('');
                                }
                            }
                        } else {
                            layer.msg('加载标的列表失败：' + res.message, {icon: 2});
                        }
                    },
                    error: function(xhr, status, error) {
                        console.error('加载标的列表请求失败：', error);
                        layer.msg('加载标的列表请求失败，请检查网络连接', {icon: 2});
                    }
                });
                
                // 监听表单提交事件
                form.on('submit(strategySubmit)', function (data) {
                    // 处理ext字段
                    if (!data.field.ext || data.field.ext === '') {
                        data.field.ext = {};
                    } else if (typeof data.field.ext === 'string') {
                        try {
                            data.field.ext = JSON.parse(data.field.ext);
                        } catch (e) {
                            data.field.ext = {};
                        }
                    }
                    
                    // 处理车轮策略特有的配置
                    if (data.field.strategyCode === 'wheel_strategy' && data.field.sellPutStrikePrice) {
                        data.field.ext.wheel_sellput_strike_price = data.field.sellPutStrikePrice;
                    }
                    
                    // 处理通用配置 - 初始股票数
                    if (data.field.initialStockNum) {
                        data.field.ext.initial_stock_num = data.field.initialStockNum;
                    }
                    
                    // 处理通用配置 - 初始股票成本价
                    if (data.field.initialStockCost) {
                        data.field.ext.initial_stock_cost = data.field.initialStockCost;
                    }
                    
                    // 删除临时字段
                    delete data.field.sellPutStrikePrice;
                    delete data.field.initialStockNum;
                    delete data.field.initialStockCost;
                    
                    console.log('提交的数据:', JSON.stringify(data.field));
                    
                    $.ajax({
                        url: '/admin/strategy/save',
                        type: 'POST',
                        contentType: 'application/json',
                        data: JSON.stringify(data.field),
                        success: function (res) {
                            if (res.success) {
                                layer.close(index);
                                layer.msg('保存成功');
                                strategyTable.reload();
                            } else {
                                layer.msg('保存失败：' + res.message);
                            }
                        },
                        error: function(xhr, status, error) {
                            console.error('保存策略失败:', xhr.responseText);
                            layer.msg('保存策略失败: ' + error);
                        }
                    });
                    return false;
                });
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

    // 账户表格工具条事件
    table.on('tool(accountTable)', function (obj) {
        const data = obj.data;
        if (obj.event === 'edit') {
            // 编辑
            layer.open({
                type: 1,
                title: '编辑账户',
                area: ['800px', '800px'],
                content: $('#accountFormTpl').html(),
                success: function (layero, index) {
                    // 设置表单值
                    form.val('accountForm', data);
                    
                    // 处理ext字段
                    if (data.ext) {
                        //设置资金规模
                        $('#account_size').val(data.ext.account_size || '');
                        $('#margin_ratio').val(data.ext.margin_ratio || '');
                        $('#position_ratio').val(data.ext.position_ratio || '');

                        // 设置长桥配置
                        $('#longport_app_key').val(data.ext.longport_app_key || '');
                        $('#longport_app_secret').val(data.ext.longport_app_secret || '');
                        $('#longport_access_token').val(data.ext.longport_access_token || '');
                        
                        // 设置AI配置
                        $('#ai_base_url').val(data.ext.ai_base_url || 'https://dashscope.aliyuncs.com/compatible-mode/v1');
                        $('#ai_api_model').val(data.ext.ai_api_model || 'deepseek-r1');
                        $('#ai_api_key').val(data.ext.ai_api_key || '');
                        $('#ai_api_temperature').val(data.ext.ai_api_temperature || '0.3');
                        $('#ai_mcp_settings').val(data.ext.ai_mcp_settings || '');
                        
                        // 设置分析配置
                        $('#kline_period').val(data.ext.kline_period || 'WEEK');
                    }
                    
                    // 渲染表单
                    form.render();
                    
                    // 监听表单提交事件
                    form.on('submit(accountSubmit)', function (data) {
                        // 处理ext字段
                        const ext = {};
                        
                        // 处理资金规模
                        ext.account_size = $('#account_size').val();
                        ext.margin_ratio = $('#margin_ratio').val();
                        ext.position_ratio = $('#position_ratio').val();    
                        // 处理长桥配置
                        ext.longport_app_key = $('#longport_app_key').val();
                        ext.longport_app_secret = $('#longport_app_secret').val();
                        ext.longport_access_token = $('#longport_access_token').val();

                        // 处理AI配置
                        ext.ai_base_url = $('#ai_base_url').val();
                        ext.ai_api_model = $('#ai_api_model').val();
                        ext.ai_api_key = $('#ai_api_key').val();
                        ext.ai_api_temperature = $('#ai_api_temperature').val();
                        ext.ai_mcp_settings = $('#ai_mcp_settings').val();
                        
                        // 处理分析配置
                        ext.kline_period = $('#kline_period').val();
                        
                        // 设置ext字段
                        data.field.ext = ext;
                        
                        // 删除表单中的kline_period字段，因为它已经被放入ext对象中
                        delete data.field.kline_period;
                        
                        // 确保ext字段是对象而不是字符串
                        const formData = {...data.field};
                        
                        $.ajax({
                            url: '/admin/account/save',
                            type: 'POST',
                            contentType: 'application/json',
                            data: JSON.stringify(formData),
                            success: function (res) {
                                if (res.success) {
                                    layer.close(index);
                                    layer.msg('保存成功');
                                    accountTable.reload();
                                } else {
                                    layer.msg('保存失败：' + res.message);
                                }
                            },
                            error: function(xhr, status, error) {
                                console.error('保存账户失败:', xhr.responseText);
                                layer.msg('保存账户失败: ' + error);
                            }
                        });
                        return false;
                    });
                }
            });
        } else if (obj.event === 'delete') {
            // 删除
            layer.confirm('确定要删除该账户吗？', function (index) {
                $.ajax({
                    url: '/admin/account/delete',
                    type: 'POST',
                    data: {
                        id: data.id
                    },
                    success: function (res) {
                        if (res.success) {
                            layer.msg('删除成功');
                            accountTable.reload();
                        } else {
                            layer.msg('删除失败：' + res.message);
                        }
                    }
                });
                layer.close(index);
            });
        }
    });
    
    // 添加账户按钮事件
    $('#addAccountBtn').click(function () {
        layer.open({
            type: 1,
            title: '添加账户',
            area: ['800px', '800px'],
            content: $('#accountFormTpl').html(),
            success: function (layero, index) {
                // 渲染表单
                form.render();
                // 监听表单提交事件
                form.on('submit(accountSubmit)', function (data) {
                    // 处理ext字段
                    const ext = {};
                    
                    // 处理长桥配置
                    if (data.field.platform === 'longport') {
                        ext.longport_app_key = $('#longport_app_key').val();
                        ext.longport_app_secret = $('#longport_app_secret').val();
                        ext.longport_access_token = $('#longport_access_token').val();
                    }
                    
                    // 处理AI配置
                    ext.ai_base_url = $('#ai_base_url').val();
                    ext.ai_api_model = $('#ai_api_model').val();
                    ext.ai_api_key = $('#ai_api_key').val();
                    ext.ai_api_temperature = $('#ai_api_temperature').val();
                    ext.ai_mcp_settings = $('#ai_mcp_settings').val();
                    
                    // 处理分析配置
                    ext.kline_period = $('#kline_period').val();
                    
                    // 设置ext字段
                    data.field.ext = ext;
                    
                    // 删除表单中的kline_period字段，因为它已经被放入ext对象中
                    delete data.field.kline_period;
                    
                    // 确保ext字段是对象而不是字符串
                    const formData = {...data.field};
                    
                    $.ajax({
                        url: '/admin/account/save',
                        type: 'POST',
                        contentType: 'application/json',
                        data: JSON.stringify(formData),
                        success: function (res) {
                            if (res.success) {
                                layer.close(index);
                                layer.msg('保存成功');
                                accountTable.reload();
                            } else {
                                layer.msg('保存失败：' + res.message);
                            }
                        },
                        error: function(xhr, status, error) {
                            console.error('保存账户失败:', xhr.responseText);
                            layer.msg('保存账户失败: ' + error);
                        }
                    });
                    return false;
                });
            }
        });
    });
    
    // 刷新账户表格按钮事件
    $('#refreshAccountBtn').click(function () {
        accountTable.reload();
    });
    
    // 批量删除账户按钮事件
    $('#deleteAccountBtn').click(function () {
        const checkStatus = table.checkStatus('accountTable');
        const data = checkStatus.data;
        if (data.length === 0) {
            layer.msg('请选择要删除的账户');
            return;
        }
        layer.confirm('确定要删除选中的' + data.length + '个账户吗？', function (index) {
            let count = 0;
            let success = 0;
            data.forEach(function (item) {
                $.ajax({
                    url: '/admin/account/delete',
                    type: 'POST',
                    async: false,
                    data: {
                        id: item.id
                    },
                    success: function (res) {
                        count++;
                        if (res.success) {
                            success++;
                        }
                        if (count === data.length) {
                            layer.msg('成功删除' + success + '个账户');
                            accountTable.reload();
                        }
                    }
                });
            });
            layer.close(index);
        });
    });
}); 