// AI助手 - 交易任务模块
layui.use(['layer', 'form', 'element', 'util'], function() {
    const layer = layui.layer;
    const form = layui.form;
    const element = layui.element;
    const util = layui.util;
    
    /**
     * 交易任务管理类
     */
    class TaskManager {
        constructor() {
            // DOM元素缓存
            this.elements = {
                taskList: document.getElementById('task-list'),
                statusFilter: document.getElementById('status-filter'),
                typeFilter: document.getElementById('type-filter'),
                codeFilter: document.getElementById('code-filter'),
                searchBtn: document.getElementById('search-btn'),
                resetBtn: document.getElementById('reset-btn'),
                tasksPanel: document.querySelector('.tasks-panel')
            };
            
            // 当前会话ID
            this.currentSessionId = null;
            
            // 初始化
            this.init();
        }
        
        /**
         * 初始化任务管理
         */
        init() {
            // 显示任务面板
            this.elements.tasksPanel.classList.add('active');
            
            // 初始化表单
            form.render();
            
            // 绑定事件
            this.bindEvents();
            
            // 修复layui标签页可能导致的滚动问题
            this.fixLayuiTabScroll();
            
            // 选项卡切换时触发事件
            element.on('tab(chat-tabs)', (data) => {
                if (data.index === 1) {
                    // 切换到任务选项卡时加载任务列表
                    this.loadTasks();
                } else if (data.index === 0) {
                    // 切换回聊天选项卡时修复滚动
                    setTimeout(() => this.fixChatHistoryScroll(), 100);
                }
            });
        }
        
        /**
         * 修复layui标签页切换可能导致的滚动问题
         */
        fixLayuiTabScroll() {
            // 确保聊天历史记录区域的滚动始终可用
            const fixScroll = () => {
                const chatHistory = document.getElementById('chat-history');
                if (chatHistory) {
                    chatHistory.style.overflowY = 'auto';
                    // 尝试滚动到底部
                    chatHistory.scrollTop = chatHistory.scrollHeight;
                }
            };
            
            // 绑定到窗口大小变化事件
            window.addEventListener('resize', fixScroll);
            
            // 初始调用一次
            fixScroll();
        }
        
        /**
         * 修复聊天历史滚动问题
         */
        fixChatHistoryScroll() {
            const chatHistory = document.getElementById('chat-history');
            if (chatHistory) {
                // 确保样式正确
                chatHistory.style.overflowY = 'auto';
                // 滚动到最新消息
                chatHistory.scrollTop = chatHistory.scrollHeight;
            }
        }
        
        /**
         * 绑定事件处理
         */
        bindEvents() {
            // 搜索按钮点击事件
            this.elements.searchBtn.addEventListener('click', () => this.loadTasks());
            
            // 重置按钮点击事件
            this.elements.resetBtn.addEventListener('click', () => {
                this.elements.statusFilter.value = '';
                this.elements.typeFilter.value = '';
                this.elements.codeFilter.value = '';
                form.render('select');
                this.loadTasks();
            });
        }
        
        /**
         * 设置当前会话ID（由聊天模块调用）
         */
        setCurrentSessionId(sessionId) {
            this.currentSessionId = sessionId;
            // 加载与当前会话相关的任务
            if (element.tabChange) {
                const activeTab = element.tabChange('chat-tabs', 1);
                if (activeTab) {
                    this.loadTasks();
                }
            }
        }
        
        /**
         * 根据会话ID过滤任务
         * 该方法将由聊天模块调用，用于在选择会话时更新任务过滤条件
         */
        filterTasksBySession(sessionId) {
            console.log('过滤任务，会话ID:', sessionId);
            
            // 只有在会话ID发生变化时才进行处理
            if (this.currentSessionId !== sessionId) {
                // 更新当前会话ID
                this.currentSessionId = sessionId;
                
                // 在任何情况下都重新加载任务列表，不管当前是什么标签页
                // 这样当用户切换到任务标签时就能看到正确的任务列表
                this.loadTasks();
                
                // 如果当前标签页是任务标签页，可以考虑切换到任务标签页以便用户能立即看到过滤结果
                const activeTab = document.querySelector('.layui-tab-title .layui-this');
                if (activeTab) {
                    const tabs = document.querySelectorAll('.layui-tab-title li');
                    const activeIndex = Array.from(tabs).indexOf(activeTab);
                    
                    // 如果当前不是在任务标签页，且有会话ID，可以考虑切换到任务标签页
                    // 但为了不影响用户体验，我们暂时不自动切换标签页
                    /*
                    if (activeIndex !== 1 && sessionId) {
                        element.tabChange('chat-tabs', 1);
                    }
                    */
                }
            }
        }
        
        /**
         * 加载任务列表
         */
        loadTasks() {
            // 显示加载中
            this.elements.taskList.innerHTML = `
                <div class="empty-state">
                    <i class="layui-icon layui-icon-loading layui-anim layui-anim-rotate layui-anim-loop"></i>
                    <p>加载中...</p>
                </div>
            `;
            
            // 构建请求URL和参数
            let url = '/task/list';
            const params = new URLSearchParams();
            
            // 添加筛选条件
            const statusFilter = this.elements.statusFilter.value;
            const typeFilter = this.elements.typeFilter.value;
            const codeFilter = this.elements.codeFilter.value;
            
            if (statusFilter) params.append('status', statusFilter);
            if (typeFilter) params.append('taskType', typeFilter);
            if (codeFilter) params.append('code', codeFilter);
            
            // 如果有当前会话ID，添加到请求参数
            // 确保使用正确的参数名称 - 可能后端期望的是session_id或sessionId或其他
            if (this.currentSessionId) {
                // 尝试多种可能的参数名称，增加过滤成功的可能性
                params.append('sessionId', this.currentSessionId);
                params.append('session_id', this.currentSessionId);
                params.append('chat_session_id', this.currentSessionId);
            }
            
            // 构建最终URL
            const queryString = params.toString();
            if (queryString) {
                url += '?' + queryString;
            }
            
            console.log('任务查询URL:', url); // 添加日志帮助调试
            
            // 发送请求
            fetch(url)
                .then(response => response.json())
                .then(result => {
                    if (result.success) {
                        console.log('获取到的任务:', result.data.length); // 添加日志帮助调试
                        
                        // 如果服务器没有正确过滤，在前端进行过滤
                        let tasks = result.data;
                        if (this.currentSessionId && tasks.length > 0) {
                            const sessionIdStr = this.currentSessionId.toString();
                            // 前端再次过滤，确保只显示与当前会话相关的任务
                            tasks = tasks.filter(task => 
                                task.sessionId && task.sessionId.toString() === sessionIdStr
                            );
                            console.log('前端过滤后的任务:', tasks.length); // 添加日志帮助调试
                        }
                        
                        this.renderTaskList(tasks);
                    } else {
                        layer.msg('加载任务列表失败: ' + result.message);
                        this.elements.taskList.innerHTML = `
                            <div class="empty-state">
                                <i class="layui-icon layui-icon-face-cry"></i>
                                <p>加载失败，请重试</p>
                            </div>
                        `;
                    }
                })
                .catch(error => {
                    console.error('Error:', error);
                    layer.msg('加载任务列表失败');
                    this.elements.taskList.innerHTML = `
                        <div class="empty-state">
                            <i class="layui-icon layui-icon-face-cry"></i>
                            <p>加载失败，请重试</p>
                        </div>
                    `;
                });
        }
        
        /**
         * 渲染任务列表
         */
        renderTaskList(tasks) {
            const statusFilter = this.elements.statusFilter.value;
            const typeFilter = this.elements.typeFilter.value;
            const codeFilter = this.elements.codeFilter.value.toLowerCase();
            
            // 检查是否已经按会话ID过滤
            const isAlreadyFiltered = this.currentSessionId !== null;
            
            // 如果任务已经在前端过滤过，就不需要再过滤了
            let filteredTasks = tasks;
            
            // 如果没有在前端过滤过，则按照界面上的筛选条件过滤
            if (!isAlreadyFiltered) {
                filteredTasks = tasks.filter(task => {
                    if (statusFilter && task.status != statusFilter) return false;
                    if (typeFilter && task.taskType != typeFilter) return false;
                    if (codeFilter && (!task.code || !task.code.toLowerCase().includes(codeFilter))) return false;
                    return true;
                });
            }
            
            // 添加会话筛选提示
            let filterHint = '';
            if (this.currentSessionId) {
                const sessionIdShort = this.currentSessionId.length > 10 ? 
                    this.currentSessionId.substring(0, 7) + '...' + this.currentSessionId.substring(this.currentSessionId.length - 7) : 
                    this.currentSessionId;
                    
                filterHint = `
                    <div style="margin-bottom: 15px; padding: 8px 12px; background-color: #f0f8ff; border-radius: 4px; border-left: 3px solid #1e9fff;">
                        <i class="layui-icon layui-icon-tips"></i> 
                        <span title="${this.currentSessionId}">当前仅显示与会话 <b>${sessionIdShort}</b> 相关的任务</span>
                        <button class="layui-btn layui-btn-xs layui-btn-primary" id="clear-session-filter" style="margin-left: 10px;">
                            <i class="layui-icon layui-icon-close"></i> 清除筛选
                        </button>
                    </div>
                `;
            }
            
            if (filteredTasks.length === 0) {
                let emptyStateMessage = '暂无符合条件的任务';
                if (this.currentSessionId) {
                    emptyStateMessage = '当前会话没有相关任务';
                }
                
                this.elements.taskList.innerHTML = `
                    ${filterHint}
                    <div class="empty-state">
                        <i class="layui-icon layui-icon-face-smile"></i>
                        <p>${emptyStateMessage}</p>
                    </div>
                `;
                
                // 如果有会话筛选，添加清除筛选按钮事件
                if (this.currentSessionId) {
                    document.getElementById('clear-session-filter').addEventListener('click', () => {
                        this.currentSessionId = null;
                        this.loadTasks();
                    });
                }
                
                return;
            }
            
            let html = filterHint;
            
            filteredTasks.forEach(task => {
                // 获取任务状态文本
                const statusText = this.getStatusText(task.status);
                // 获取任务类型文本
                const typeText = this.getTypeText(task.taskType);
                
                // 处理ext字段，将其解析为JSON树形结构
                let extContent = '';
                if (task.ext) {
                    let extObj;
                    try {
                        // 尝试解析ext字段，如果它是字符串
                        extObj = typeof task.ext === 'string' ? JSON.parse(task.ext) : task.ext;
                        extContent = this.renderJsonTree(extObj);
                    } catch (e) {
                        // 如果解析失败，直接显示ext内容
                        extContent = `<pre>${task.ext}</pre>`;
                    }
                } else {
                    extContent = '<span>无任务扩展信息</span>';
                }
                
                html += `
                    <div class="layui-card task-card">
                        <div class="layui-card-header task-header">
                            <div class="task-title">${typeText} - ${task.code || '未知标的'}</div>
                            <div class="task-status task-status-${task.status}">${statusText}</div>
                        </div>
                        <div class="layui-card-body task-body">
                            <div class="task-info">
                                <div class="task-info-item">
                                    <span class="task-info-label">任务ID：</span>
                                    <span>${task.id}</span>
                                </div>
                                <div class="task-info-item">
                                    <span class="task-info-label">标的代码：</span>
                                    <span>${task.code || '未知'}</span>
                                </div>
                                <div class="task-info-item">
                                    <span class="task-info-label">会话ID：</span>
                                    <span>${task.sessionId || '未关联'}</span>
                                </div>
                                <div class="task-info-item">
                                    <span class="task-info-label">策略ID：</span>
                                    <span>${task.strategyId || '未知'}</span>
                                </div>
                                <div class="task-info-item">
                                    <span class="task-info-label">开始时间：</span>
                                    <span>${task.startTime ? util.toDateString(new Date(task.startTime), 'yyyy-MM-dd HH:mm:ss') : '未知'}</span>
                                </div>
                                <div class="task-info-item">
                                    <span class="task-info-label">结束时间：</span>
                                    <span>${task.endTime ? util.toDateString(new Date(task.endTime), 'yyyy-MM-dd HH:mm:ss') : '未设置'}</span>
                                </div>
                                <div class="task-info-item">
                                    <span class="task-info-label">创建时间：</span>
                                    <span>${task.createTime ? util.toDateString(new Date(task.createTime), 'yyyy-MM-dd HH:mm:ss') : '未知'}</span>
                                </div>
                                <div class="task-info-item">
                                    <span class="task-info-label">扩展信息：</span>
                                    <div class="json-tree-container">${extContent}</div>
                                </div>
                            </div>
                            <div class="task-actions">
                                ${task.status === 1 ? `
                                    <button class="layui-btn layui-btn-sm execute-task-btn" data-id="${task.id}">
                                        <i class="layui-icon layui-icon-play"></i> 采纳
                                    </button>
                                    <button class="layui-btn layui-btn-sm cancel-task-btn" data-id="${task.id}">
                                        <i class="layui-icon layui-icon-close"></i> 取消
                                    </button>
                                ` : ''}
                                <button class="layui-btn layui-btn-sm layui-btn-normal edit-task-btn" data-id="${task.id}">
                                    <i class="layui-icon layui-icon-edit"></i> 编辑
                                </button>
                                <button class="layui-btn layui-btn-sm delete-task-btn" data-id="${task.id}">
                                    <i class="layui-icon layui-icon-delete"></i> 删除
                                </button>
                            </div>
                        </div>
                    </div>
                `;
            });
            
            this.elements.taskList.innerHTML = html;

            // 给任务操作按钮添加事件
            document.querySelectorAll('.execute-task-btn').forEach(btn => {
                btn.addEventListener('click', () => {
                    const taskId = btn.dataset.id;
                    this.executeTask(taskId);
                });
            });

            document.querySelectorAll('.cancel-task-btn').forEach(btn => {
                btn.addEventListener('click', () => {
                    const taskId = btn.dataset.id;
                    this.cancelTask(taskId);
                });
            });

            document.querySelectorAll('.delete-task-btn').forEach(btn => {
                btn.addEventListener('click', () => {
                    const taskId = btn.dataset.id;
                    this.deleteTask(taskId);
                });
            });

            // 添加编辑按钮事件监听
            document.querySelectorAll('.edit-task-btn').forEach(btn => {
                btn.addEventListener('click', () => {
                    const taskId = btn.dataset.id;
                    this.showEditTaskForm(taskId);
                });
            });

            // 如果有会话筛选，添加清除筛选按钮事件
            if (this.currentSessionId) {
                const clearBtn = document.getElementById('clear-session-filter');
                if (clearBtn) {
                    clearBtn.addEventListener('click', () => {
                        this.currentSessionId = null;
                        this.loadTasks();
                    });
                }
            }
        }
        
        /**
         * 将JSON对象渲染为树形结构HTML
         */
        renderJsonTree(obj) {
            if (!obj) return '<span>无数据</span>';
            
            const renderNode = (key, value, isRoot = false) => {
                if (value === null) return `<span class="json-null">null</span>`;
                
                if (typeof value === 'object') {
                    const isArray = Array.isArray(value);
                    let content = '';
                    
                    // 遍历对象属性或数组元素
                    for (const k in value) {
                        if (value.hasOwnProperty(k)) {
                            const nodeKey = isArray ? `[${k}]` : k;
                            content += `
                                <div class="json-tree-node">
                                    <span class="json-key">${nodeKey}:</span>
                                    ${renderNode(k, value[k])}
                                </div>
                            `;
                        }
                    }
                    
                    // 根节点直接返回内容，无需折叠
                    if (isRoot) {
                        return `
                            <div class="json-tree-node">
                                ${content || '<span class="json-empty">{}</span>'}
                            </div>
                        `;
                    }
                    
                    // 非根节点，添加折叠功能
                    return `
                        <div class="json-tree-value">
                            <span class="json-braces">${isArray ? '[' : '{'}</span>
                            <div class="json-tree-children">
                                ${content || '<span class="json-empty">' + (isArray ? '[]' : '{}') + '</span>'}
                            </div>
                            <span class="json-braces">${isArray ? ']' : '}'}</span>
                        </div>
                    `;
                }
                
                // 基本类型值
                if (typeof value === 'string') {
                    return `<span class="json-string">"${value.replace(/"/g, '\\"')}"</span>`;
                } else if (typeof value === 'number') {
                    return `<span class="json-number">${value}</span>`;
                } else if (typeof value === 'boolean') {
                    return `<span class="json-boolean">${value}</span>`;
                }
                
                return `<span>${String(value)}</span>`;
            };
            
            // 添加CSS样式
            const style = `
                <style>
                    .json-tree-container {
                        font-family: monospace;
                        font-size: 14px;
                        background-color: #f8f8f8;
                        border-radius: 4px;
                        padding: 10px;
                        overflow: auto;
                        max-height: 200px;
                    }
                    .json-tree-node {
                        padding-left: 20px;
                        position: relative;
                    }
                    .json-key {
                        color: #881391;
                        font-weight: bold;
                        margin-right: 5px;
                    }
                    .json-string {
                        color: #1a1aa6;
                    }
                    .json-number {
                        color: #116644;
                    }
                    .json-boolean {
                        color: #116644;
                        font-weight: bold;
                    }
                    .json-null {
                        color: #777;
                        font-style: italic;
                    }
                    .json-braces {
                        color: #333;
                    }
                    .json-tree-children {
                        margin-left: 10px;
                    }
                    .json-empty {
                        color: #777;
                        font-style: italic;
                    }
                </style>
            `;
            
            return style + renderNode(null, obj, true);
        }
        
        /**
         * 获取任务状态文本
         */
        getStatusText(status) {
            switch (parseInt(status)) {
                case 1: return '待采纳';
                case 2: return '采纳中';
                case 3: return '已完成';
                case 4: return '已取消';
                case 5: return '采纳失败';
                default: return '未知状态';
            }
        }
        
        /**
         * 获取任务类型文本
         */
        getTypeText(type) {
            switch (parseInt(type)) {
                case 1: return '买入期权';
                case 2: return '卖出期权';
                case 5: return '平仓期权';
                case 7: return '滚动期权';
                case 99: return '其他任务';
                default: return '未知类型';
            }
        }
        
        /**
         * 执行任务
         */
        executeTask(taskId) {
            layer.confirm('确定要采纳此任务吗？', {
                btn: ['确定', '取消']
            }, () => {
                fetch('/task/execute', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded',
                    },
                    body: `taskId=${taskId}`
                })
                .then(response => response.json())
                .then(result => {
                    if (result.success) {
                        layer.msg('任务采纳请求已发送');
                        this.loadTasks();
                        
                        // 在聊天中添加消息提示
                        if (this.currentSessionId && window.aiChatApp) {
                            window.aiChatApp.addSystemMessage(`任务#${taskId}已被采纳`);
                        }
                    } else {
                        layer.msg('采纳任务失败: ' + result.message);
                    }
                })
                .catch(error => {
                    console.error('Error:', error);
                    layer.msg('采纳任务失败');
                });
            });
        }
        
        /**
         * 取消任务
         */
        cancelTask(taskId) {
            layer.confirm('确定要取消此任务吗？', {
                btn: ['确定', '取消']
            }, () => {
                fetch('/task/cancel', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded',
                    },
                    body: `taskId=${taskId}`
                })
                .then(response => response.json())
                .then(result => {
                    if (result.success) {
                        layer.msg('任务已取消');
                        this.loadTasks();
                        
                        // 在聊天中添加消息提示
                        if (this.currentSessionId && window.aiChatApp) {
                            window.aiChatApp.addSystemMessage(`任务#${taskId}已取消`);
                        }
                    } else {
                        layer.msg('取消任务失败: ' + result.message);
                    }
                })
                .catch(error => {
                    console.error('Error:', error);
                    layer.msg('取消任务失败');
                });
            });
        }
        
        /**
         * 删除任务
         */
        deleteTask(taskId) {
            layer.confirm('确定要删除此任务吗？', {
                btn: ['确定', '取消']
            }, () => {
                fetch(`/task/delete?taskId=${taskId}`, {
                    method: 'DELETE'
                })
                .then(response => response.json())
                .then(result => {
                    if (result.success) {
                        layer.msg('任务删除成功');
                        this.loadTasks();
                    } else {
                        layer.msg('删除任务失败: ' + result.message);
                    }
                })
                .catch(error => {
                    console.error('Error:', error);
                    layer.msg('删除任务失败');
                });
            });
        }
        
        /**
         * 显示编辑任务表单
         */
        showEditTaskForm(taskId) {
            // 先获取任务详情
            fetch(`/task/list?taskId=${taskId}`)
                .then(response => response.json())
                .then(result => {
                    if (!result.success || !result.data || result.data.length === 0) {
                        layer.msg('获取任务详情失败');
                        return;
                    }
                    
                    const task = result.data.find(t => t.id == taskId);
                    if (!task) {
                        layer.msg('未找到任务详情');
                        return;
                    }
                    
                    // 准备ext字段的JSON格式
                    let extJson = '';
                    if (task.ext) {
                        try {
                            const extObj = typeof task.ext === 'string' ? JSON.parse(task.ext) : task.ext;
                            extJson = JSON.stringify(extObj, null, 2);
                        } catch (e) {
                            extJson = typeof task.ext === 'string' ? task.ext : JSON.stringify(task.ext);
                            console.error('格式化ext字段失败:', e);
                        }
                    }
                    
                    // 打开编辑表单
                    layer.open({
                        type: 1,
                        title: '编辑交易任务',
                        area: ['500px', '550px'],
                        content: `
                            <div class="layui-form" style="padding: 20px;">
                                <div class="layui-form-item">
                                    <label class="layui-form-label">任务类型</label>
                                    <div class="layui-input-block">
                                        <select name="taskType" lay-verify="required">
                                            <option value="">请选择任务类型</option>
                                            <option value="1" ${task.taskType == 1 ? 'selected' : ''}>买入期权</option>
                                            <option value="2" ${task.taskType == 2 ? 'selected' : ''}>卖出期权</option>
                                            <option value="5" ${task.taskType == 5 ? 'selected' : ''}>平仓期权</option>
                                            <option value="7" ${task.taskType == 7 ? 'selected' : ''}>滚动期权</option>
                                            <option value="99" ${task.taskType == 99 ? 'selected' : ''}>其他任务</option>
                                        </select>
                                    </div>
                                </div>
                                <div class="layui-form-item">
                                    <label class="layui-form-label">标的代码</label>
                                    <div class="layui-input-block">
                                        <input type="text" name="code" value="${task.code || ''}" placeholder="请输入标的代码" class="layui-input">
                                    </div>
                                </div>
                                <div class="layui-form-item">
                                    <label class="layui-form-label">策略ID</label>
                                    <div class="layui-input-block">
                                        <input type="text" name="strategyId" value="${task.strategyId || ''}" placeholder="请输入策略ID" class="layui-input">
                                    </div>
                                </div>
                                <div class="layui-form-item">
                                    <label class="layui-form-label">开始时间</label>
                                    <div class="layui-input-block">
                                        <input type="text" name="startTime" id="startTime" value="${task.startTime ? util.toDateString(new Date(task.startTime), 'yyyy-MM-dd HH:mm:ss') : ''}" placeholder="请选择开始时间" class="layui-input">
                                        <div class="layui-form-mid layui-word-aux">格式: YYYY-MM-DD HH:MM:SS</div>
                                    </div>
                                </div>
                                <div class="layui-form-item">
                                    <label class="layui-form-label">结束时间</label>
                                    <div class="layui-input-block">
                                        <input type="text" name="endTime" id="endTime" value="${task.endTime ? util.toDateString(new Date(task.endTime), 'yyyy-MM-dd HH:mm:ss') : ''}" placeholder="请选择结束时间" class="layui-input">
                                        <div class="layui-form-mid layui-word-aux">格式: YYYY-MM-DD HH:MM:SS</div>
                                    </div>
                                </div>
                                <div class="layui-form-item layui-form-text">
                                    <label class="layui-form-label">扩展字段</label>
                                    <div class="layui-input-block">
                                        <textarea name="extJson" placeholder="请输入JSON格式的扩展字段" class="layui-textarea" style="height: 150px; font-family: monospace;">${extJson}</textarea>
                                        <div class="layui-form-mid layui-word-aux">
                                            <p>请确保输入有效的JSON格式。此字段包含任务的所有扩展属性。</p>
                                            <p>例如：{"task_description":"任务说明","strike_price":"100","option_type":"call"}</p>
                                            <button type="button" class="layui-btn layui-btn-xs" id="validate-json-btn">
                                                <i class="layui-icon layui-icon-ok"></i> 验证JSON
                                            </button>
                                        </div>
                                    </div>
                                </div>
                                <div class="layui-form-item">
                                    <div class="layui-input-block">
                                        <button class="layui-btn" lay-submit lay-filter="editTaskForm">提交</button>
                                        <button type="reset" class="layui-btn layui-btn-primary">重置</button>
                                    </div>
                                </div>
                            </div>
                        `,
                        success: (layero, index) => {
                            // 初始化日期选择器
                            layui.laydate.render({
                                elem: '#startTime',
                                type: 'datetime'
                            });
                            
                            layui.laydate.render({
                                elem: '#endTime',
                                type: 'datetime'
                            });
                            
                            // 初始化表单
                            form.render();
                            
                            // 添加JSON验证功能
                            document.getElementById('validate-json-btn').addEventListener('click', () => {
                                const jsonText = document.querySelector('textarea[name="extJson"]').value;
                                try {
                                    if (!jsonText.trim()) {
                                        layer.msg('JSON内容为空');
                                        return;
                                    }
                                    
                                    // 尝试解析JSON
                                    const parsedJson = JSON.parse(jsonText);
                                    
                                    // 格式化JSON并更新文本区域
                                    const formattedJson = JSON.stringify(parsedJson, null, 2);
                                    document.querySelector('textarea[name="extJson"]').value = formattedJson;
                                    
                                    layer.msg('JSON格式有效，已自动格式化');
                                } catch (e) {
                                    layer.msg('JSON格式无效: ' + e.message);
                                }
                            });
                            
                            // 监听表单提交
                            form.on('submit(editTaskForm)', (data) => {
                                // 验证日期格式
                                if (data.field.startTime && !this.formatDateToISO(data.field.startTime)) {
                                    layer.msg('开始时间格式无效，请使用正确的日期格式');
                                    return false;
                                }
                                if (data.field.endTime && !this.formatDateToISO(data.field.endTime)) {
                                    layer.msg('结束时间格式无效，请使用正确的日期格式');
                                    return false;
                                }
                                
                                // 处理扩展字段JSON
                                if (data.field.extJson) {
                                    try {
                                        // 验证JSON格式
                                        const extObj = JSON.parse(data.field.extJson);
                                        
                                        // 如果有任务描述，更新到扩展字段中
                                        if (data.field.description) {
                                            extObj.task_description = data.field.description;
                                        }
                                        
                                        // 设置ext字段 - 作为对象而不是字符串
                                        data.field.ext = extObj;
                                    } catch (e) {
                                        layer.msg('扩展字段JSON格式无效，请检查');
                                        console.error('解析扩展字段JSON失败:', e);
                                        return false;
                                    }
                                } else if (data.field.description) {
                                    // 如果没有提供扩展字段但有任务描述，创建新的扩展字段
                                    data.field.ext = {
                                        task_description: data.field.description
                                    };
                                }
                                
                                // 删除中间字段，不需要发送到服务器
                                delete data.field.description;
                                delete data.field.extJson;
                                
                                // 发送编辑任务请求
                                this.editTask(taskId, data.field, index);
                                return false;
                            });
                        }
                    });
                })
                .catch(error => {
                    console.error('Error:', error);
                    layer.msg('获取任务详情失败');
                });
        }
        
        /**
         * 编辑任务
         */
        editTask(taskId, formData, layerIndex) {
            // 构建请求数据
            const taskData = { ...formData };
            
            // 处理日期格式
            if (taskData.startTime) {
                const isoStartTime = this.formatDateToISO(taskData.startTime);
                if (isoStartTime) {
                    taskData.startTime = isoStartTime;
                }
            }
            
            if (taskData.endTime) {
                const isoEndTime = this.formatDateToISO(taskData.endTime);
                if (isoEndTime) {
                    taskData.endTime = isoEndTime;
                }
            }
            
            // 确保ext字段是对象而不是字符串
            if (taskData.ext && typeof taskData.ext === 'string') {
                try {
                    taskData.ext = JSON.parse(taskData.ext);
                } catch (e) {
                    console.error('解析ext字段失败:', e);
                    layer.msg('扩展字段格式错误，请重新检查');
                    return;
                }
            }
            
            // 发送请求
            fetch(`/task/update?taskId=${taskId}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(taskData)
            })
            .then(response => response.json())
            .then(result => {
                if (result.success) {
                    layer.msg('任务更新成功');
                    layer.close(layerIndex);
                    this.loadTasks();
                    
                    // 如果更新了与会话相关的任务，在聊天中添加消息提示
                    if (this.currentSessionId && window.aiChatApp) {
                        const taskTypeText = this.getTypeText(formData.taskType);
                        const message = `已更新${taskTypeText}任务：${formData.code || '未知标的'}`;
                        
                        // 添加系统消息
                        window.aiChatApp.addSystemMessage(message);
                    }
                } else {
                    layer.msg('更新任务失败: ' + result.message);
                }
            })
            .catch(error => {
                console.error('Error:', error);
                layer.msg('更新任务失败');
            });
        }
        
        /**
         * 将日期字符串转换为ISO 8601格式
         * @param {string} dateStr 日期字符串
         * @returns {string} ISO 8601格式的日期字符串
         */
        formatDateToISO(dateStr) {
            if (!dateStr) return null;
            
            try {
                // 尝试解析日期字符串
                const date = new Date(dateStr);
                
                // 检查日期是否有效
                if (isNaN(date.getTime())) {
                    console.error('无效的日期:', dateStr);
                    return null;
                }
                
                // 返回ISO格式的日期字符串
                return date.toISOString();
            } catch (e) {
                console.error('日期格式转换失败:', e);
                return null;
            }
        }
    }
    
    // 创建并导出任务管理器实例
    window.taskManager = new TaskManager();
}); 