package me.dingtou.options.service.copilot;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import me.dingtou.options.manager.OwnerManager;
import me.dingtou.options.model.Message;
import me.dingtou.options.model.OwnerChatRecord;
import me.dingtou.options.service.AssistantService;

@Component
public class AgentCopilotServiceImpl implements CopilotService {
   @Autowired
    private AssistantService assistantService;

    @Autowired
    private OwnerManager ownerManager;

    @Override
    public String mode() {
        return "agent";
    }

    @Override
    public String start(String owner,
            String title,
            Message message,
            Function<Message, Void> callback,
            Function<Message, Void> failCallback) {
        String sessionId = message.getSessionId();
        List<Message> messages = new ArrayList<>();
        
        // 1.初始化系统提示词 系统提示词通过模版初始化，里面包含MCP工具列表。
        
        // 2.初始化用户问题

        // 3.请求大模型

        // 4.解析回复中是否包含MCP工具调用 如果有则调用mcp服务

        // 5.将MCP服务请求结果补充提交给大模型 如有需要循环3、4、5，直到解决好用户问题，或需要用户提供其他信息。

        return null;

    }

    @Override
    public void continuing(String owner,
            String sessionId,
            Message message,
            Function<Message, Void> callback,
            Function<Message, Void> failCallback) {

        //任务完成后用户继续补充提问

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
       

    }

}
