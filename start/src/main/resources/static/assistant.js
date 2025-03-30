// AI助手模块
layui.use(['layer', 'form', 'util'], function() {
    const layer = layui.layer;
    const util = layui.util;
    
    /**
     * AI聊天应用类
     */
    class AIChatApp {
        constructor() {
            // 核心状态变量
            this.aiSource = null;
            this.currentClientId = null;
            this.currentSessionId = null;
            this.isHistoryVisible = true;
            this.isTyping = false;
            this.currentMessage = {};
            
            // DOM元素缓存
            this.elements = {
                chatHistory: document.getElementById('chat-history'),
                chatInput: document.getElementById('chat-input'),
                chatTitle: document.getElementById('chat-title'),
                chatList: document.getElementById('chat-list'),
                sendBtn: document.getElementById('send-btn'),
                clearBtn: document.getElementById('clear-btn'),
                deleteBtn: document.getElementById('delete-btn'),
                refreshBtn: document.getElementById('refresh-history-btn'),
                newChatBtn: document.getElementById('new-chat-btn'),
                saveTitleBtn: document.getElementById('save-title-btn'),
                toggleHistoryBtn: document.getElementById('toggle-history-btn'),
                titleActions: document.getElementById('title-actions')
            };
            
            // 初始化应用
            this.init();
        }
        
        /**
         * 初始化应用
         */
        init() {
            this.initEventSource();
            this.loadChatSessions();
            this.bindEvents();
            this.loadPrompt();
            
            // 监听窗口大小变化，确保滚动正常工作
            window.addEventListener('resize', () => {
                // 重新检查聊天历史滚动条
                setTimeout(() => this.scrollToBottom(), 200);
            });
        }

        /**
         * 加载提示词
         */
        loadPrompt(){
            const title = localStorage.getItem("title");
            const prompt = localStorage.getItem("prompt");
            if(title){
                this.elements.chatTitle.value = title;
                localStorage.removeItem("title");
            }
            if(prompt){
                this.elements.chatInput.value = prompt;
                localStorage.removeItem("prompt");
            }
        }
        
        /**
         * 初始化EventSource连接
         */
        initEventSource() {
            if (!window.EventSource) {
                layer.msg('您的浏览器不支持EventSource，无法使用AI助手功能');
                return;
            }
            
            this.currentClientId = "ai_chat_" + new Date().getTime();
            this.aiSource = new EventSource(`/ai/init?requestId=${this.currentClientId}`);
            
            this.aiSource.addEventListener("message", (e) => {
                try {
                    const message = JSON.parse(e.data);
                    this.appendMessage(message);
                } catch (error) {
                    console.error('解析消息失败:', error);
                }
            });
            
            this.aiSource.addEventListener("error", () => {
                if (this.aiSource.readyState === EventSource.CLOSED) {
                    console.log('EventSource连接已关闭');
                    // 尝试重新连接
                    setTimeout(() => this.initEventSource(), 3000);
                }
            });
        }
        
        /**
         * 绑定事件处理
         */
        bindEvents() {
            // 发送消息
            this.elements.sendBtn.addEventListener('click', () => this.sendMessage());
            
            // 清理聊天
            this.elements.clearBtn.addEventListener('click', () => this.clearChat());
            
            // 删除会话
            this.elements.deleteBtn.addEventListener('click', () => {
                if (!this.currentSessionId) return;
                
                layer.confirm('确定要删除这个会话吗？', {
                    btn: ['确定', '取消']
                }, () => this.deleteChat(this.currentSessionId));
            });
            
            // 刷新历史
            this.elements.refreshBtn.addEventListener('click', () => this.loadChatSessions());
            
            // 新对话
            this.elements.newChatBtn.addEventListener('click', () => this.startNewChat());
            
            // 保存标题
            this.elements.saveTitleBtn.addEventListener('click', () => {
                if (!this.currentSessionId) return;
                
                const title = this.elements.chatTitle.value.trim();
                this.updateChatTitle(this.currentSessionId, title);
            });
            
            // 切换历史记录显示/隐藏
            this.elements.toggleHistoryBtn.addEventListener('click', () => this.toggleHistoryVisibility());
            
            // 按Enter键发送消息
            this.elements.chatInput.addEventListener('keydown', (e) => {
                if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault();
                    this.sendMessage();
                }
            });
            
            // 标题输入框变化时显示保存按钮
            this.elements.chatTitle.addEventListener('input', () => {
                if (this.currentSessionId) {
                    this.elements.titleActions.style.display = 'block';
                }
            });
        }
        
        /**
         * 切换历史记录显示/隐藏
         */
        toggleHistoryVisibility() {
            if (this.isHistoryVisible) {
                // 隐藏历史记录
                this.elements.chatList.style.display = 'none';
                this.elements.toggleHistoryBtn.innerHTML = '<i class="layui-icon layui-icon-spread-left"></i> 显示历史';
                this.isHistoryVisible = false;
            } else {
                // 显示历史记录
                this.elements.chatList.style.display = 'block';
                this.elements.toggleHistoryBtn.innerHTML = '<i class="layui-icon layui-icon-shrink-right"></i> 隐藏历史';
                this.isHistoryVisible = true;
            }
        }
        
        /**
         * 开始新对话
         */
        startNewChat() {
            // 清空当前会话
            this.clearChat();
            
            // 显示空状态
            this.elements.chatHistory.innerHTML = `
                <div class="empty-state">
                    <i class="layui-icon layui-icon-dialogue"></i>
                    <p>开始新的对话</p>
                </div>
            `;
            
            // 隐藏删除按钮和标题保存按钮
            this.elements.deleteBtn.style.display = 'none';
            this.elements.titleActions.style.display = 'none';
            
            // 清空标题
            this.elements.chatTitle.value = '';
            
            // 聚焦到输入框
            this.elements.chatInput.focus();
            
            // 确保任务管理器清除当前会话过滤
            if (window.taskManager) {
                window.taskManager.setCurrentSessionId(null);
                window.taskManager.filterTasksBySession(null);
            }
        }
        
        /**
         * 加载聊天会话列表
         */
        loadChatSessions() {
            // 显示加载状态
            this.elements.chatList.innerHTML = `
                <div class="history-actions">
                    <button class="layui-btn layui-btn-primary layui-btn-sm action-btn" id="refresh-history-btn">
                        <i class="layui-icon layui-icon-refresh"></i> 刷新
                    </button>
                    <button class="layui-btn layui-btn-normal layui-btn-sm action-btn" id="new-chat-btn">
                        <i class="layui-icon layui-icon-add-1"></i> 新对话
                    </button>
                </div>
                <div class="empty-state">
                    <i class="layui-icon layui-icon-loading layui-anim layui-anim-rotate layui-anim-loop"></i>
                    <p>加载中...</p>
                </div>
            `;
            
            // 重新绑定按钮事件
            document.getElementById('refresh-history-btn').addEventListener('click', () => this.loadChatSessions());
            document.getElementById('new-chat-btn').addEventListener('click', () => this.startNewChat());
            
            // 获取会话列表
            fetch('/ai/record/sessions')
                .then(response => response.json())
                .then(result => {
                    if (result.success) {
                        this.renderChatSessions(result.data);
                    } else {
                        this.showLoadError('加载聊天会话失败: ' + result.message);
                    }
                })
                .catch(error => {
                    console.error('Error:', error);
                    this.showLoadError('加载聊天会话失败');
                });
        }
        
        /**
         * 显示加载错误
         */
        showLoadError(message) {
            layer.msg(message);
            this.elements.chatList.innerHTML = `
                <div class="history-actions">
                    <button class="layui-btn layui-btn-primary layui-btn-sm action-btn" id="refresh-history-btn">
                        <i class="layui-icon layui-icon-refresh"></i> 刷新
                    </button>
                    <button class="layui-btn layui-btn-normal layui-btn-sm action-btn" id="new-chat-btn">
                        <i class="layui-icon layui-icon-add-1"></i> 新对话
                    </button>
                </div>
                <div class="empty-state">
                    <i class="layui-icon layui-icon-face-cry"></i>
                    <p>加载失败，请重试</p>
                </div>
            `;
            
            // 重新绑定按钮事件
            document.getElementById('refresh-history-btn').addEventListener('click', () => this.loadChatSessions());
            document.getElementById('new-chat-btn').addEventListener('click', () => this.startNewChat());
        }
        
        /**
         * 渲染聊天会话列表
         */
        renderChatSessions(sessions) {
            const chatListEl = this.elements.chatList;
            
            // 添加操作按钮
            chatListEl.innerHTML = `
                <div class="history-actions">
                    <button class="layui-btn layui-btn-primary layui-btn-sm action-btn" id="refresh-history-btn">
                        <i class="layui-icon layui-icon-refresh"></i> 刷新
                    </button>
                    <button class="layui-btn layui-btn-normal layui-btn-sm action-btn" id="new-chat-btn">
                        <i class="layui-icon layui-icon-add-1"></i> 新对话
                    </button>
                </div>
            `;
            
            // 重新绑定按钮事件
            document.getElementById('refresh-history-btn').addEventListener('click', () => this.loadChatSessions());
            document.getElementById('new-chat-btn').addEventListener('click', () => this.startNewChat());
            
            if (!sessions || sessions.length === 0) {
                chatListEl.innerHTML += `
                    <div class="empty-state">
                        <i class="layui-icon layui-icon-dialogue"></i>
                        <p>暂无聊天记录</p>
                    </div>
                `;
                return;
            }
            
            // 按时间排序，最新的在前面
            sessions.sort((a, b) => new Date(b.createTime) - new Date(a.createTime));
            
            // 渲染会话列表
            sessions.forEach(session => {
                const chatItem = document.createElement('div');
                chatItem.className = 'chat-item';
                chatItem.dataset.sessionId = session.sessionId;
                
                const timeStr = util.toDateString(new Date(session.createTime), 'yyyy-MM-dd HH:mm:ss');
                
                // 处理预览内容，如果超过50个字符则截断
                const preview = session.content && session.content.length > 80 
                    ? session.content.substring(0, 80) + '...' 
                    : (session.content || '');
                
                chatItem.innerHTML = `
                    <div class="chat-item-title">${session.title || '未命名会话'}</div>
                    <div class="chat-item-preview">${preview}</div>
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
                    this.loadChatDetail(session.sessionId);
                });
                
                chatListEl.appendChild(chatItem);
            });
        }
        
        /**
         * 加载聊天详情
         */
        loadChatDetail(sessionId) {
            fetch(`/ai/record/list?sessionId=${sessionId}`)
                .then(response => response.json())
                .then(result => {
                    if (result.success && result.data && result.data.length > 0) {
                        // 设置当前会话ID
                        this.currentSessionId = sessionId;
                        
                        // 清空当前聊天记录
                        this.clearChatHistory();
                        
                        // 显示删除按钮
                        this.elements.deleteBtn.style.display = 'inline-block';
                        
                        // 显示标题保存按钮
                        this.elements.titleActions.style.display = 'block';
                        
                        // 渲染聊天详情
                        this.renderChatDetail(result.data);
                        
                        // 通知任务管理器当前会话已变更，并过滤相关任务
                        if (window.taskManager) {
                            window.taskManager.setCurrentSessionId(sessionId);
                            window.taskManager.filterTasksBySession(sessionId);
                        }
                    } else {
                        this.currentSessionId = null;
                        layer.msg('加载聊天详情失败: ' + (result.message || '无法找到历史对话记录'));
                        // 显示空状态
                        this.elements.chatHistory.innerHTML = `
                            <div class="empty-state">
                                <i class="layui-icon layui-icon-face-surprised"></i>
                                <p>无法找到历史对话记录</p>
                            </div>
                        `;
                        
                        // 通知任务管理器当前会话已清除
                        if (window.taskManager) {
                            window.taskManager.setCurrentSessionId(null);
                            window.taskManager.filterTasksBySession(null);
                        }
                    }
                })
                .catch(error => {
                    console.error('Error:', error);
                    this.currentSessionId = null;
                    layer.msg('加载聊天详情失败');
                    // 显示空状态
                    this.elements.chatHistory.innerHTML = `
                        <div class="empty-state">
                            <i class="layui-icon layui-icon-face-cry"></i>
                            <p>加载失败，请重试</p>
                        </div>
                    `;
                    
                    // 通知任务管理器当前会话已清除
                    if (window.taskManager) {
                        window.taskManager.setCurrentSessionId(null);
                        window.taskManager.filterTasksBySession(null);
                    }
                });
        }
        
        /**
         * 渲染聊天详情
         */
        renderChatDetail(records) {
            if (!records || records.length === 0) {
                this.elements.chatHistory.innerHTML = `
                    <div class="empty-state">
                        <i class="layui-icon layui-icon-face-surprised"></i>
                        <p>该会话没有聊天记录</p>
                    </div>
                `;
                return;
            }
            
            // 设置标题
            this.elements.chatTitle.value = records[0].title || '';
            
            // 显示聊天记录
            records.forEach(record => {
                const message = {
                    id: record.messageId || new Date().getTime(),
                    role: record.role,
                    content: record.content,
                    reasoningContent: record.reasoningContent,
                    time: new Date(record.createTime || Date.now())
                };
                this.appendMessage(message);
            });
        }
        
        /**
         * 清空聊天历史记录
         */
        clearChatHistory() {
            this.currentMessage = {};
            this.elements.chatHistory.innerHTML = '';
        }
        
        /**
         * 清理聊天
         */
        clearChat() {
            this.clearChatHistory();
            this.elements.chatInput.value = '';
            this.currentSessionId = null;
            this.elements.deleteBtn.style.display = 'none';
            this.elements.titleActions.style.display = 'none';
            
            // 通知任务管理器当前会话已清除
            if (window.taskManager) {
                window.taskManager.setCurrentSessionId(null);
                window.taskManager.filterTasksBySession(null);
            }
        }
        
        /**
         * 添加消息到聊天界面
         */
        appendMessage(message) {
            const historyDiv = this.elements.chatHistory;
            const messageId = message.id?message.id:new Date().getTime();
            const reasoningId = `${message.role}-reasoning-${messageId}`;
            const contentId = `${message.role}-content-${messageId}`;

            var currentReasoningMessage = this.currentMessage[reasoningId];
            if(!currentReasoningMessage){
                this.currentMessage[reasoningId] = "";
            }
            var currentContentMessage = this.currentMessage[contentId];
            if(!currentContentMessage){
                this.currentMessage[contentId] = "";
            }   
            this.currentMessage[reasoningId] += (message.reasoningContent?message.reasoningContent:"");
            this.currentMessage[contentId] += (message.content?message.content:"");

            
            // 如果是AI助手的消息且有思考内容，则先添加思考内容区域
            let reasoningContentDiv = document.getElementById(reasoningId);
            if (!reasoningContentDiv && this.currentMessage[reasoningId]) {
                const reasoningDiv = document.createElement('div');
                reasoningDiv.className = 'reasoning-message';
                const reasoningHeaderDiv = document.createElement('div');
                reasoningHeaderDiv.className = 'reasoning-header';
                reasoningHeaderDiv.innerHTML = `
                    <i class="layui-icon layui-icon-think"></i> AI思考过程
                `;
                reasoningContentDiv = document.createElement('div');
                reasoningContentDiv.className = 'reasoning-content';
                reasoningContentDiv.id = reasoningId;
                reasoningDiv.appendChild(reasoningHeaderDiv);
                reasoningDiv.appendChild(reasoningContentDiv);

                historyDiv.appendChild(reasoningDiv);
            }
            // 使用marked解析Markdown内容
            if(reasoningContentDiv && this.currentMessage[reasoningId]){
                const reasoningContent = marked.parse(this.currentMessage[reasoningId]);
                reasoningContentDiv.innerHTML = reasoningContent;
            }
            
            // 移除正在输入的指示器
            const typingIndicator = document.getElementById('typing-indicator');
            if (typingIndicator) {
                typingIndicator.remove();
                this.isTyping = false;
            }
            
            // 添加或更新消息内容
            let contentDiv = document.getElementById(contentId);
            if (!contentDiv && this.currentMessage[contentId]) {
                const messageDiv = document.createElement('div');
                messageDiv.className = message.role === 'user' ? 'user-message' : 'assistant-message';
                
                // 创建消息头部
                const headerDiv = document.createElement('div');
                headerDiv.className = 'message-header';
                
                const timeStr = message.time ? 
                    message.time.toLocaleString() : 
                    new Date().toLocaleString();
                
                headerDiv.innerHTML = `
                    <span class="message-role">
                        <i class="layui-icon ${message.role === 'user' ? 'layui-icon-username' : 'layui-icon-dialogue'}"></i>
                        ${message.role === 'user' ? '用户' : 'AI助手'}
                    </span>
                    <span class="message-time">${timeStr}</span>
                `;
                
                // 创建消息内容容器
                contentDiv = document.createElement('div');
                contentDiv.id = contentId;
                contentDiv.className = 'message-content';
                
                messageDiv.appendChild(headerDiv);
                messageDiv.appendChild(contentDiv);
                historyDiv.appendChild(messageDiv);
            }
            
            // 更新消息内容
            if (contentDiv && this.currentMessage[contentId]) {
                // 使用marked解析Markdown内容
                const content = marked.parse(this.currentMessage[contentId]);
                contentDiv.innerHTML = content;
                
                // 处理代码高亮
                document.querySelectorAll('pre code').forEach((block) => {
                    if (window.hljs) {
                        hljs.highlightBlock(block);
                    }
                });
            }
            
            // 滚动到底部
            this.scrollToBottom();
        }
        
        /**
         * 滚动到聊天历史底部
         */
        scrollToBottom() {
            const historyDiv = this.elements.chatHistory;
            if (historyDiv) {
                // 确保样式设置允许滚动
                historyDiv.style.overflowY = 'auto';
                
                // 滚动到底部
                setTimeout(() => {
                    historyDiv.scrollTop = historyDiv.scrollHeight;
                }, 50);
            }
        }
        
        /**
         * 显示AI正在输入的指示器
         */
        showTypingIndicator() {
            if (this.isTyping) return;
            
            const historyDiv = this.elements.chatHistory;
            const typingDiv = document.createElement('div');
            typingDiv.id = 'typing-indicator';
            typingDiv.className = 'assistant-message';
            
            typingDiv.innerHTML = `
                <div class="message-header">
                    <span class="message-role">
                        <i class="layui-icon layui-icon-dialogue"></i>
                        AI助手
                    </span>
                    <span class="message-time">${new Date().toLocaleString()}</span>
                </div>
                <div class="message-content">
                    <div class="typing-indicator">
                        <span></span>
                        <span></span>
                        <span></span>
                    </div>
                </div>
            `;
            
            historyDiv.appendChild(typingDiv);
            this.scrollToBottom();
            this.isTyping = true;
        }
        
        /**
         * 发送消息
         */
        sendMessage() {
            const message = this.elements.chatInput.value.trim();
            const title = this.elements.chatTitle.value.trim();
            
            if (!message) return;
            
            // 清空输入框
            this.elements.chatInput.value = '';
            
            // 添加用户消息到聊天界面
            const userMessage = {
                id: new Date().getTime(),
                role: 'user',
                content: message,
                time: new Date()
            };
            this.appendMessage(userMessage);
            
            // 显示AI正在输入的指示器
            this.showTypingIndicator();
            
            // 构建请求参数
            const params = new URLSearchParams({
                requestId: this.currentClientId,
                title: title || '未命名会话',
                message: message
            });
            
            // 如果有会话ID，添加到参数中
            if (this.currentSessionId) {
                params.append('sessionId', this.currentSessionId);
            }
            
            // 确定请求endpoint
            const endpoint = this.currentSessionId ? '/ai/chat/continue' : '/ai/chat';
            
            // 发送请求
            fetch(endpoint, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: params
            })
            .then(response => response.json())
            .then(data => {
                console.log('消息发送成功:', data);
                
                // 如果是新会话，设置会话ID
                if (data.success && data.data && !this.currentSessionId) {
                    this.currentSessionId = data.data;
                    this.elements.deleteBtn.style.display = 'inline-block';
                    
                    // 通知任务管理器当前会话已变更
                    if (window.taskManager) {
                        window.taskManager.setCurrentSessionId(this.currentSessionId);
                    }
                }
                
                // 发送成功后，刷新会话列表
                // setTimeout(() => this.loadChatSessions(), 1000);
            })
            .catch(error => {
                console.error('发送消息失败:', error);
                layer.msg('发送消息失败，请重试');
                
                // 移除正在输入的指示器
                const typingIndicator = document.getElementById('typing-indicator');
                if (typingIndicator) {
                    typingIndicator.remove();
                    this.isTyping = false;
                }
            });
        }
        
        /**
         * 更新聊天标题
         */
        updateChatTitle(sessionId, title) {
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
                    
                    // 隐藏保存按钮
                    this.elements.titleActions.style.display = 'none';
                } else {
                    layer.msg('标题更新失败: ' + result.message);
                }
            })
            .catch(error => {
                console.error('Error:', error);
                layer.msg('标题更新失败');
            });
        }
        
        /**
         * 删除聊天会话
         */
        deleteChat(sessionId) {
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
                    
                    // 清空聊天
                    this.clearChat();
                    
                    // 显示空状态
                    this.elements.chatHistory.innerHTML = `
                        <div class="empty-state">
                            <i class="layui-icon layui-icon-dialogue"></i>
                            <p>选择一个历史会话或开始新对话</p>
                        </div>
                    `;
                    
                    // 确保任务管理器清除当前会话过滤
                    if (window.taskManager) {
                        window.taskManager.setCurrentSessionId(null);
                        window.taskManager.filterTasksBySession(null);
                    }
                    
                    // 重新加载会话列表
                    this.loadChatSessions();
                } else {
                    layer.msg('会话删除失败: ' + result.message);
                }
            })
            .catch(error => {
                console.error('Error:', error);
                layer.msg('会话删除失败');
            });
        }
    }
    
    // 创建并导出AI聊天应用
    window.aiChatApp = new AIChatApp();
}); 