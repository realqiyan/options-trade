package me.dingtou.options.manager;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.dao.OwnerChatRecordDAO;
import me.dingtou.options.model.Message;

import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.model.OwnerChatRecord;
import me.dingtou.options.util.AccountExtUtils;
import me.dingtou.options.util.ChatClient;
import me.dingtou.options.util.ChatClient.ChatResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Agent 模式
 *
 * @author qiyan
 */
@Slf4j
@Component
public class AskManager {

    @Autowired
    private OwnerManager ownerManager;

    @Autowired
    private OwnerChatRecordDAO ownerChatRecordDAO;

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
            if (message.getId() != null) {
                // 查询这条消息的会话ID
                LambdaQueryWrapper<OwnerChatRecord> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(OwnerChatRecord::getOwner, owner)
                        .eq(OwnerChatRecord::getMessageId, message.getId());
                OwnerChatRecord record = ownerChatRecordDAO.selectOne(queryWrapper);
                if (record != null) {
                    sessionId = record.getSessionId();
                    break;
                }
            }
        }

        // 保存用户新消息
        final String finalSessionId = sessionId;
        messages.stream().filter(message -> "user".equals(message.getRole()) && null == message.getId())
                .forEach(message -> {
                    OwnerChatRecord userRecord = new OwnerChatRecord(owner,
                            finalSessionId,
                            String.valueOf(System.currentTimeMillis()),
                            title,
                            "user",
                            message.getContent(),
                            null);
                    ownerChatRecordDAO.insert(userRecord);
                });

        // 发送消息并获取响应
        ChatResult result = sendChatMessage(ownerAccount, messages, callback);

        // 保存AI助手的完整回复
        if (!result.getContent().isEmpty()) {
            OwnerChatRecord assistantRecord = new OwnerChatRecord(
                    owner,
                    finalSessionId,
                    result.getMessageId(),
                    title,
                    "assistant",
                    result.getContent(),
                    result.getReasoningContent());
            ownerChatRecordDAO.insert(assistantRecord);
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
        String systemPrompt = AccountExtUtils.getSystemPrompt(account);
        // 添加系统消息
        if (StringUtils.isNotBlank(systemPrompt)) {
            messages.add(0, new Message("system", systemPrompt));
        }

        String baseUrl = AccountExtUtils.getAiBaseUrl(account);
        String apiKey = AccountExtUtils.getAiApiKey(account);
        String model = AccountExtUtils.getAiApiModel(account);
        double temperature = Double.parseDouble(AccountExtUtils.getAiApiTemperature(account));

        ChatResponse chatResponse;
        try {
            // chatResponse = ChatClient.sendChatRequest(account, systemPrompt, messages);
            String sessionId = messages.get(0).getSessionId();
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
                                callback.apply(new Message(chatResp.getId(),
                                        sessionId,
                                        "assistant",
                                        chatResp.getContent(),
                                        null));
                                finalContent.append(chatResp.getContent());
                            } else if (chatResp.isChunk() && chatResp.getReasoningContent() != null) {
                                Message reasoning = new Message(chatResp.getId(),
                                        sessionId,
                                        "assistant",
                                        null,
                                        chatResp.getReasoningContent());
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