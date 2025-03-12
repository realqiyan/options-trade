// 聊天记录管理页面的JavaScript
layui.use(['layer', 'form', 'util'], function() {
    const layer = layui.layer;
    const form = layui.form;
    const util = layui.util;
    
    // 当前选中的会话ID
    let currentSessionId = null;
    
    // 初始化页面
    function init() {
        loadChatSessions();
    }
    
    // 加载聊天会话列表
    function loadChatSessions() {
        fetch('/ai/record/sessions')
            .then(response => response.json())
            .then(result => {
                if (result.success) {
                    renderChatSessions(result.data);
                } else {
                    layer.msg('加载聊天会话失败: ' + result.message);
                    document.getElementById('chat-list').innerHTML = `
                        <div class="empty-state">
                            <i class="layui-icon layui-icon-face-cry" style="font-size: 48px;"></i>
                            <p>加载失败，请重试</p>
                        </div>
                    `;
                }
            })
            .catch(error => {
                console.error('Error:', error);
                layer.msg('加载聊天会话失败');
                document.getElementById('chat-list').innerHTML = `
                    <div class="empty-state">
                        <i class="layui-icon layui-icon-face-cry" style="font-size: 48px;"></i>
                        <p>加载失败，请重试</p>
                    </div>
                `;
            });
    }
    
    // 渲染聊天会话列表
    function renderChatSessions(sessions) {
        const chatListEl = document.getElementById('chat-list');
        
        if (!sessions || sessions.length === 0) {
            chatListEl.innerHTML = `
                <div class="empty-state">
                    <i class="layui-icon layui-icon-dialogue" style="font-size: 48px;"></i>
                    <p>暂无聊天记录</p>
                </div>
            `;
            return;
        }
        
        // 清空列表
        chatListEl.innerHTML = '';
        
        // 添加刷新按钮
        const refreshBtn = document.createElement('button');
        refreshBtn.className = 'layui-btn layui-btn-primary layui-btn-sm';
        refreshBtn.innerHTML = '<i class="layui-icon layui-icon-refresh"></i> 刷新';
        refreshBtn.style.marginBottom = '10px';
        refreshBtn.onclick = loadChatSessions;
        chatListEl.appendChild(refreshBtn);
        
        // 添加加载提示
        const loadingEl = document.createElement('div');
        loadingEl.className = 'empty-state';
        loadingEl.innerHTML = '<i class="layui-icon layui-icon-loading layui-anim layui-anim-rotate layui-anim-loop"></i><p>加载中...</p>';
        chatListEl.appendChild(loadingEl);
        
        // 创建一个Promise数组，用于存储所有会话详情的加载Promise
        const fetchPromises = sessions.map(sessionId => {
            return fetch(`/ai/record/list?sessionId=${sessionId}`)
                .then(response => response.json())
                .then(result => {
                    if (result.success && result.data && result.data.length > 0) {
                        const records = result.data;
                        const firstRecord = records[0];
                        const lastRecord = records[records.length - 1];
                        
                        return {
                            sessionId: sessionId,
                            title: firstRecord.title || '未命名会话',
                            time: new Date(firstRecord.createTime),
                            preview: firstRecord.content.length > 50 
                                ? firstRecord.content.substring(0, 50) + '...' 
                                : firstRecord.content
                        };
                    }
                    return null;
                })
                .catch(error => {
                    console.error('Error fetching session details:', error);
                    return null;
                });
        });
        
        // 等待所有Promise完成后，一次性渲染会话列表
        Promise.all(fetchPromises)
            .then(sessionDetails => {
                // 移除加载提示
                chatListEl.removeChild(loadingEl);
                
                // 过滤掉加载失败的会话
                const validSessions = sessionDetails.filter(session => session !== null);
                
                if (validSessions.length === 0) {
                    chatListEl.innerHTML += `
                        <div class="empty-state">
                            <i class="layui-icon layui-icon-face-surprised" style="font-size: 48px;"></i>
                            <p>无法加载会话详情</p>
                        </div>
                    `;
                    return;
                }
                
                // 渲染会话列表
                validSessions.forEach(session => {
                    const chatItem = document.createElement('div');
                    chatItem.className = 'chat-item';
                    chatItem.dataset.sessionId = session.sessionId;
                    
                    const timeStr = util.toDateString(session.time, 'yyyy-MM-dd HH:mm:ss');
                    
                    chatItem.innerHTML = `
                        <div class="chat-item-title">${session.title}</div>
                        <div class="chat-item-preview">${session.preview}</div>
                        <div class="chat-item-time">${timeStr}</div>
                    `;
                    
                    // 点击会话项加载详情
                    chatItem.addEventListener('click', () => {
                        // 移除其他项的选中状态
                        document.querySelectorAll('.chat-item').forEach(item => {
                            item.classList.remove('active');
                        });
                        
                        // 添加选中状态
                        chatItem.classList.add('active');
                        
                        // 加载会话详情
                        currentSessionId = session.sessionId;
                        loadChatDetail(session.sessionId);
                    });
                    
                    chatListEl.appendChild(chatItem);
                });
            })
            .catch(error => {
                console.error('Error rendering sessions:', error);
                chatListEl.innerHTML = `
                    <div class="empty-state">
                        <i class="layui-icon layui-icon-face-cry" style="font-size: 48px;"></i>
                        <p>加载失败，请重试</p>
                    </div>
                `;
            });
    }
    
    // 加载聊天详情
    function loadChatDetail(sessionId) {
        fetch(`/ai/record/list?sessionId=${sessionId}`)
            .then(response => response.json())
            .then(result => {
                if (result.success) {
                    renderChatDetail(result.data);
                } else {
                    layer.msg('加载聊天详情失败: ' + result.message);
                }
            })
            .catch(error => {
                console.error('Error:', error);
                layer.msg('加载聊天详情失败');
            });
    }
    
    // 渲染聊天详情
    function renderChatDetail(records) {
        const chatDetailEl = document.getElementById('chat-detail');
        
        if (!records || records.length === 0) {
            chatDetailEl.innerHTML = `
                <div class="empty-state">
                    <i class="layui-icon layui-icon-face-surprised" style="font-size: 48px;"></i>
                    <p>该会话没有聊天记录</p>
                </div>
            `;
            return;
        }
        
        // 清空详情区域
        chatDetailEl.innerHTML = '';
        
        // 添加操作按钮
        const actionsDiv = document.createElement('div');
        actionsDiv.className = 'chat-actions';
        
        // 标题输入框
        const titleInput = document.createElement('div');
        titleInput.className = 'layui-form-item title-input';
        titleInput.innerHTML = `
            <div class="layui-input-group">
                <div class="layui-input-split layui-input-prefix">标题</div>
                <input type="text" id="chat-title" placeholder="请输入会话标题" value="${records[0].title || ''}" class="layui-input">
                <div class="layui-input-suffix">
                    <button type="button" class="layui-btn" id="save-title-btn">保存</button>
                </div>
            </div>
        `;
        actionsDiv.appendChild(titleInput);
        
        // 操作按钮
        const buttonsDiv = document.createElement('div');
        buttonsDiv.innerHTML = `
            <button type="button" class="layui-btn layui-btn-normal layui-btn-sm" id="continue-chat-btn">
                <i class="layui-icon layui-icon-dialogue"></i> 继续对话
            </button>
            <button type="button" class="layui-btn layui-btn-danger layui-btn-sm" id="delete-chat-btn">
                <i class="layui-icon layui-icon-delete"></i> 删除会话
            </button>
        `;
        actionsDiv.appendChild(buttonsDiv);
        
        chatDetailEl.appendChild(actionsDiv);
        
        // 添加聊天记录
        const chatMessagesDiv = document.createElement('div');
        chatMessagesDiv.className = 'chat-messages';
        
        records.forEach(record => {
            // 如果是AI助手的消息且有思考内容，则先添加思考内容区域
            if (record.role === 'assistant' && record.reasoningContent) {
                const reasoningDiv = document.createElement('div');
                reasoningDiv.className = 'reasoning-message';
                reasoningDiv.style.backgroundColor = '#f6f6f6';
                reasoningDiv.style.padding = '10px';
                reasoningDiv.style.borderRadius = '4px';
                reasoningDiv.style.marginTop = '5px';
                reasoningDiv.style.marginBottom = '15px';
                reasoningDiv.style.border = '1px dashed #ddd';
                
                // 使用marked解析Markdown内容
                const reasoningContent = marked.parse(record.reasoningContent);
                
                reasoningDiv.innerHTML = `
                    <div class="reasoning-header" style="margin-bottom: 5px; color: #666;">
                        <i class="layui-icon layui-icon-think"></i> AI思考过程
                    </div>
                    <div class="reasoning-content">${reasoningContent}</div>
                `;
                
                chatMessagesDiv.appendChild(reasoningDiv);
            }
            
            const messageDiv = document.createElement('div');
            messageDiv.className = record.role === 'user' ? 'user-message' : 'assistant-message';
            
            // 使用marked解析Markdown内容
            const content = marked.parse(record.content);
            
            messageDiv.innerHTML = `
                <div class="message-header">
                    <span class="message-role">${record.role === 'user' ? '用户' : 'AI助手'}</span>
                    <span class="message-time">${util.toDateString(new Date(record.createTime), 'yyyy-MM-dd HH:mm:ss')}</span>
                </div>
                <div class="message-content">${content}</div>
            `;
            
            chatMessagesDiv.appendChild(messageDiv);
        });
        
        chatDetailEl.appendChild(chatMessagesDiv);
        
        // 绑定事件
        document.getElementById('save-title-btn').addEventListener('click', () => {
            const title = document.getElementById('chat-title').value.trim();
            updateChatTitle(currentSessionId, title);
        });
        
        document.getElementById('delete-chat-btn').addEventListener('click', () => {
            layer.confirm('确定要删除这个会话吗？', {
                btn: ['确定', '取消']
            }, function() {
                deleteChat(currentSessionId);
            });
        });
        
        // 绑定继续对话按钮事件
        document.getElementById('continue-chat-btn').addEventListener('click', () => {
            continueChatSession(currentSessionId, records);
        });
    }
    
    // 更新聊天标题
    function updateChatTitle(sessionId, title) {
        if (!title) {
            layer.msg('标题不能为空');
            return;
        }
        
        fetch('/ai/record/update-title', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: new URLSearchParams({
                sessionId: sessionId,
                title: title
            })
        })
        .then(response => response.json())
        .then(result => {
            if (result.success) {
                layer.msg('标题更新成功');
                
                // 更新列表中的标题
                const chatItem = document.querySelector(`.chat-item[data-session-id="${sessionId}"]`);
                if (chatItem) {
                    const titleEl = chatItem.querySelector('.chat-item-title');
                    if (titleEl) {
                        titleEl.textContent = title;
                    }
                }
            } else {
                layer.msg('标题更新失败: ' + result.message);
            }
        })
        .catch(error => {
            console.error('Error:', error);
            layer.msg('标题更新失败');
        });
    }
    
    // 删除聊天会话
    function deleteChat(sessionId) {
        fetch(`/ai/record/delete?sessionId=${sessionId}`, {
            method: 'DELETE'
        })
        .then(response => response.json())
        .then(result => {
            if (result.success) {
                layer.msg('会话删除成功');
                
                // 从列表中移除
                const chatItem = document.querySelector(`.chat-item[data-session-id="${sessionId}"]`);
                if (chatItem) {
                    chatItem.remove();
                }
                
                // 清空详情区域
                document.getElementById('chat-detail').innerHTML = `
                    <div class="empty-state">
                        <i class="layui-icon layui-icon-dialogue" style="font-size: 48px;"></i>
                        <p>请选择一个聊天会话</p>
                    </div>
                `;
                
                // 重新加载会话列表
                loadChatSessions();
            } else {
                layer.msg('会话删除失败: ' + result.message);
            }
        })
        .catch(error => {
            console.error('Error:', error);
            layer.msg('会话删除失败');
        });
    }
    
    // 继续对话功能
    function continueChatSession(sessionId, records) {
        if (!sessionId || !records || records.length === 0) {
            layer.msg('无法继续对话，会话数据不完整');
            return;
        }
        
        // 将会话ID和记录保存到localStorage
        localStorage.setItem('continueSessionId', sessionId);
        localStorage.setItem('continueSessionTitle', records[0].title || '未命名会话');
        
        // 跳转到AI聊天页面
        window.location.href = 'index.html?continueChatSession=true';
    }
    
    // 初始化页面
    init();
});
