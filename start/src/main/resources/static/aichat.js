//JS
// AI Chat
let aiSource = null;
let currentClientId = null;
let allMessages = {};
let currentSessionId = null;

function initAIChat() {
    if (window.EventSource) {
        currentClientId = "ai_chat_" + new Date().getTime();
        aiSource = new EventSource("/ai/init?requestId=" + currentClientId);
        aiSource.addEventListener("message", function(e) {
            const message = JSON.parse(e.data);
            appendMessage(message);
        });
    }
}

// 初始化时调用
initAIChat();

// 检查是否有继续对话的会话ID
function checkContinueSession() {
    const urlParams = new URLSearchParams(window.location.search);
    const continueChatSession = urlParams.get('continueChatSession');
    
    if (continueChatSession === 'true') {
        const sessionId = localStorage.getItem('continueSessionId');
        const sessionTitle = localStorage.getItem('continueSessionTitle');
        
        if (sessionId) {
            // 保存会话ID，用于后续继续对话
            currentSessionId = sessionId;
            
            // 加载历史消息
            fetch(`/ai/record/list?sessionId=${sessionId}`)
                .then(response => response.json())
                .then(result => {
                    if (result.success && result.data && result.data.length > 0) {
                        // 清空当前聊天记录
                        clearChat();
                        
                        // 显示聊天框
                        const chatBox = document.getElementById('chat-box');
                        chatBox.style.display = 'block';
                        
                        // 设置当前会话标题
                        if (sessionTitle) {
                            currentPrompt = sessionTitle;
                        }
                        
                        // 显示历史消息
                        result.data.forEach(record => {
                            const message = {
                                id: record.messageId || new Date().getTime(),
                                role: record.role,
                                content: record.content,
                                reasoningContent: record.reasoningContent
                            };
                            appendMessage(message);
                        });
                        
                        // 提示用户可以继续对话
                        layer.msg('已加载历史对话，您可以继续对话了');
                    } else {
                        layer.msg('加载历史对话失败');
                    }
                })
                .catch(error => {
                    console.error('Error:', error);
                    layer.msg('加载历史对话失败');
                })
                .finally(() => {
                    // 清除localStorage中的会话ID和标题
                    localStorage.removeItem('continueSessionId');
                    localStorage.removeItem('continueSessionTitle');
                });
        }
    }
}

// 页面加载完成后检查是否有继续对话的会话ID
document.addEventListener('DOMContentLoaded', checkContinueSession);

function showChat() {
    const chatBox = document.getElementById('chat-box');
    chatBox.style.display = chatBox.style.display === 'none' ? 'block' : 'none';
    document.getElementById('chat-input').value = currentPrompt || '';
}

function clearChat() {
    const historyDiv = document.getElementById('chat-history');
    historyDiv.innerHTML = ''; // 清空聊天记录
    document.getElementById('chat-input').value = ''; // 清空输入框
    allMessages = {}; // 清空消息缓存
}

function appendMessage(message) {
    const historyDiv = document.getElementById('chat-history');
    const id = `${message.role}-${message.id}`;
    if (!allMessages[id]){
        allMessages[id] = "";
    }
    allMessages[id] = allMessages[id] + (message.reasoningContent ? message.reasoningContent : message.content);

    // 使用marked.parse格式化消息内容
    const htmlContent = marked.parse(allMessages[id]);
    const messageDiv = document.getElementById(id);
    if (messageDiv) {
        messageDiv.innerHTML = htmlContent;
    } else {
        const messageDiv = document.createElement('div');
        messageDiv.id = id;
        messageDiv.className = `layui-font-14 ${message.role}-message`;
        messageDiv.style.margin = '5px 0';
        messageDiv.style.padding = '8px';
        messageDiv.style.borderRadius = '4px';
        messageDiv.style.backgroundColor = message.role === 'user' ? '#e6f7ff' : message.reasoningContent ? '#f6f6f6' : '#fff';
        
        // 如果是reasoning_content类型，添加思考图标和标题
        if (message.reasoningContent) {
            messageDiv.innerHTML = `
                <div class="reasoning-header" style="margin-bottom: 5px; color: #666;">
                    <i class="layui-icon layui-icon-think"></i> AI思考过程
                </div>
                ${htmlContent}
            `;
        } else {
            messageDiv.innerHTML = htmlContent;
        }
        
        historyDiv.appendChild(messageDiv);
    }
    historyDiv.scrollTop = historyDiv.scrollHeight;
}

function sendMessage(title) {
    const input = document.getElementById('chat-input');
    var message = input.value.trim();
    if (!message) return;
    input.value = '';
    
    appendMessage({'content': message, 'role': 'user', 'id': new Date().getTime()} );

    // 构建请求参数
    const params = {
        requestId: currentClientId,
        title: title,
        message: message
    };

    // 如果有会话ID，则调用继续对话接口
    if (currentSessionId) {
        params.sessionId = currentSessionId;
        
        // 调用继续对话接口
        fetch(`/ai/chat/continue`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: new URLSearchParams(params)
        })
        .then(response => response.json())
        .then(data => {
            console.log(data);
        });
    } else {
        // 调用普通对话接口
        fetch(`/ai/chat`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: new URLSearchParams(params)
        })
        .then(response => response.json())
        .then(data => {
            console.log(data);
        });
    }
}

// 添加聊天记录管理入口
function showChatHistory() {
    window.location.href = 'chat-history.html';
}
