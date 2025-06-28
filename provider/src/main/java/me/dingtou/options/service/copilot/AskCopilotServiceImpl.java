package me.dingtou.options.service.copilot;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import me.dingtou.options.manager.AskManager;
import me.dingtou.options.model.Message;
import me.dingtou.options.model.OwnerChatRecord;
import me.dingtou.options.service.AssistantService;

@Component
public class AskCopilotServiceImpl implements CopilotService {

    @Autowired
    private AssistantService assistantService;

    @Autowired
    private AskManager askManager;

    @Override
    public String mode() {
        return "ask";
    }

    @Override
    public String start(String owner,
            String title,
            Message message,
            Function<Message, Void> callback,
            Function<Message, Void> failCallback) {
        String sessionId = message.getSessionId();
        List<Message> messages = new ArrayList<>();
        messages.add(message);
        askManager.ask(owner, sessionId, title, messages, callback);
        return sessionId;

    }

    @Override
    public void continuing(String owner,
            String sessionId,
            Message message,
            Function<Message, Void> callback,
            Function<Message, Void> failCallback) {
        // 获取历史消息(user & assistant)
        List<OwnerChatRecord> records = assistantService.listRecordsBySessionId(owner, sessionId);
        if (records == null || records.isEmpty()) {
            failCallback.apply(new Message(null, sessionId, "assistant", "无法找到历史对话记录", null));

        }

        // 转换历史消息为Message对象
        List<Message> messages = new ArrayList<>();
        for (OwnerChatRecord record : records) {
            Message chatMessage = new Message(record.getMessageId(),
                    record.getSessionId(),
                    record.getRole(),
                    record.getContent(),
                    record.getReasoningContent());
            messages.add(chatMessage);
        }

        // 添加新消息
        Message newMessage = new Message(null, sessionId, "user", message.getContent(), null);
        messages.add(newMessage);

        // 获取会话标题
        String title = records.get(0).getTitle();
       
        // 继续
        askManager.ask(owner, sessionId, title, messages, callback);

    }

}
