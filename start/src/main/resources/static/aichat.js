//JS
// AI Chat
let aiSource = null;
let currentClientId = null;
function initAIChat() {
    if (window.EventSource) {
        currentClientId = "ai_chat_" + new Date().getTime();
        aiSource = new EventSource("/ai/init?requestId=" + currentClientId);
        aiSource.addEventListener("message", function(e) {
            const response = JSON.parse(e.data);
            appendMessage(response.type, response.message, 'ai', response.id);
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
}

function appendMessage(type, content, role, messageId) {
    const historyDiv = document.getElementById('chat-history');
    const id = `${type}-${messageId}`;
    const messageDiv = document.getElementById(id);
    if (messageDiv) {
        messageDiv.innerHTML = messageDiv.innerHTML + content;
    } else {
        const messageDiv = document.createElement('div');
        messageDiv.id = id;
        messageDiv.className = `layui-font-14 ${role}-message`;
        messageDiv.style.margin = '5px 0';
        messageDiv.style.padding = '8px';
        messageDiv.style.borderRadius = '4px';
        messageDiv.style.backgroundColor = role === 'user' ? '#e6f7ff' : type === 'reasoning_content' ? '#f0f0f0' : '#fff';
        messageDiv.innerHTML = content;
        historyDiv.appendChild(messageDiv);
    }
    historyDiv.scrollTop = historyDiv.scrollHeight;
}

function sendMessage() {
    const input = document.getElementById('chat-input');
    const message = input.value.trim();
    if (!message) return;

    appendMessage('content', message, 'user' , new Date().getTime());
    input.value = '';

    // 调用后端接口
    fetch(`/ai/chat`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: new URLSearchParams({
            requestId: currentClientId,
            message: message
        })
    })
    .then(response => response.json())
    .then(data => {
        console.log(data);
    });
}
