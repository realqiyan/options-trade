//JS
// AI Chat
let aiSource = null;
let currentClientId = null;
let allMessages = {};

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

    // 调用后端接口
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

// 添加聊天记录管理入口
function showChatHistory() {
    window.location.href = 'chat-history.html';
}
