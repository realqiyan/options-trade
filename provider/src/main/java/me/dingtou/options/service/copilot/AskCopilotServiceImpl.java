package me.dingtou.options.service.copilot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.manager.OwnerManager;
import me.dingtou.options.model.Message;
import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.model.OwnerChatRecord;
import me.dingtou.options.service.AssistantService;
import me.dingtou.options.util.AccountExtUtils;
import me.dingtou.options.util.ChatClient;
import me.dingtou.options.util.ChatClient.ChatResponse;

@Slf4j
@Component
public class AskCopilotServiceImpl implements CopilotService {

    private final String SYSTEM_PROMPT = "You are a helpful assistant.";

    @Autowired
    private AssistantService assistantService;

    @Autowired
    private OwnerManager ownerManager;

    @Override
    public String mode() {
        return "ask";
    }

    @Override
    public String start(String owner,
            String sessionId,
            String title,
            Message message,
            Function<Message, Void> callback,
            Function<Message, Void> failCallback) {
        List<Message> messages = new ArrayList<>();
        messages.add(new Message("system", SYSTEM_PROMPT));
        messages.add(message);
        ask(owner, sessionId, title, messages, callback);
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
            failCallback.apply(new Message("assistant", "无法找到历史对话记录"));
            return;
        }

        // 转换历史消息为Message对象
        List<Message> messages = new ArrayList<>();
        messages.addAll(records);

        // 添加新消息
        Message newMessage = new Message("user", message.getContent());
        messages.add(newMessage);

        // 获取会话标题
        String title = records.get(0).getTitle();

        // 继续
        ask(owner, sessionId, title, messages, callback);

    }

    /**
     * Ask 模式
     * 
     * @param owner
     * @param sessionId
     * @param title
     * @param messages
     * @param callback
     */
    public void ask(String owner,
            String sessionId,
            String title,
            List<Message> messages,
            Function<Message, Void> callback) {
        OwnerAccount ownerAccount = ownerManager.queryOwnerAccount(owner);
        if (null == messages
                || messages.isEmpty()
                || null == ownerAccount
                || null == AccountExtUtils.getAiApiKey(ownerAccount)) {
            return;
        }

        // 检查消息列表中是否有已存在的消息ID
        for (Message message : messages) {
            if (message.getMessageId() != null) {
                // 查询这条消息的会话ID
                OwnerChatRecord record = assistantService.getRecordByMessageId(owner, message.getMessageId());
                if (record != null) {
                    sessionId = record.getSessionId();
                    break;
                }
            }
        }

        // 保存用户新消息
        final String finalSessionId = sessionId;
        messages.stream().filter(message -> "user".equals(message.getRole()) && null == message.getMessageId())
                .forEach(message -> {
                    OwnerChatRecord userRecord = new OwnerChatRecord(owner,
                            finalSessionId,
                            title,
                            "user",
                            message.getContent(),
                            null);
                    assistantService.addChatRecord(owner, finalSessionId, userRecord);
                });

        // 发送消息并获取响应
        ChatResult result = sendChatMessage(ownerAccount, messages, callback);

        // 保存AI助手的完整回复
        if (!result.getContent().isEmpty()) {
            OwnerChatRecord assistantRecord = new OwnerChatRecord(
                    owner,
                    finalSessionId,
                    title,
                    "assistant",
                    result.getContent(),
                    result.getReasoningContent());
            assistantRecord.setMessageId(result.getMessageId());
            assistantService.addChatRecord(owner, finalSessionId, assistantRecord);
        }
    }

    /**
     * 发送聊天消息并处理流式响应
     *
     * @param account  账号
     * @param messages 用户消息列表
     * @param callback 回调函数，用于处理流式响应
     * @return 返回AI助手的完整响应和消息ID
     */
    private ChatResult sendChatMessage(OwnerAccount account, List<Message> messages, Function<Message, Void> callback) {
        // 创建一个StringBuilder来收集AI助手的完整回复
        StringBuilder reasoningContent = new StringBuilder();
        StringBuilder finalContent = new StringBuilder();

        // 系统提示词
        String mcpSettings = AccountExtUtils.getSystemPrompt(account);
        // 添加系统消息
        if (StringUtils.isNotBlank(mcpSettings)) {
            messages.add(0, new Message("system", mcpSettings));
        }

        String baseUrl = AccountExtUtils.getAiBaseUrl(account);
        String apiKey = AccountExtUtils.getAiApiKey(account);
        String model = AccountExtUtils.getAiApiModel(account);
        double temperature = Double.parseDouble(AccountExtUtils.getAiApiTemperature(account));

        ChatResponse chatResponse;
        try {
            // chatResponse = ChatClient.sendChatRequest(account, mcpSettings, messages);
            chatResponse = ChatClient.sendStreamChatRequest(baseUrl,
                    apiKey,
                    model,
                    temperature,
                    messages,
                    new Consumer<ChatClient.ChatResponse>() {
                        @Override
                        public void accept(ChatClient.ChatResponse chatResp) {
                            // 收集AI助手的回复
                            if (chatResp.isChunk() && chatResp.getContent() != null) {
                                callback.apply(new Message(chatResp.getId(), "assistant", chatResp.getContent()));
                                finalContent.append(chatResp.getContent());
                            } else if (chatResp.isChunk() && chatResp.getReasoningContent() != null) {
                                Message reasoning = new Message();
                                reasoning.setMessageId(chatResp.getId());
                                reasoning.setRole("assistant");
                                reasoning.setReasoningContent(chatResp.getReasoningContent());
                                callback.apply(reasoning);
                                reasoningContent.append(chatResp.getReasoningContent());
                            }
                        }
                    }).get(300, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("发送聊天消息失败: {}", e.getMessage(), e);
            return new ChatResult(null, e.getMessage(), null);
        }

        return new ChatResult(chatResponse.getId(), chatResponse.getContent(), chatResponse.getReasoningContent());
    }

    /**
     * 聊天结果
     */
    @Getter
    public static class ChatResult {
        private final String messageId;
        private final String content;
        private final String reasoningContent;

        public ChatResult(String messageId, String content, String reasoningContent) {
            this.messageId = messageId;
            this.content = content;
            this.reasoningContent = reasoningContent;
        }

    }

}
