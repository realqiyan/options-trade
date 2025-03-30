package me.dingtou.options.manager;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.model.Message;

import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.util.AccountExtUtils;
import me.dingtou.options.util.ChatClient;
import me.dingtou.options.util.ChatClient.ChatResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 聊天管理器
 *
 * @author qiyan
 */
@Slf4j
@Component
public class ChatManager {

    /**
     * 系统提示词，规范AI助手返回结构
     */
    private static final String SYSTEM_PROMPT_TEMPLATE = """
            您是股票、期权交易专家，擅长技术分析，擅长通过卖期权赚取权利金，您的职责是分析用户提供的数据，解读股票价格走势，提供期权策交易建议。
            要求：
            1. 充分利用提供的数据，根据需要自动计算技术指标。
            2. 综合分析结论，提供专业期权策交易策略和充分的推荐理由。
            3. 仅考虑单腿策略和车轮策略。
            """;

    /**
     * 发送聊天消息并处理流式响应
     *
     * @param account  账号
     * @param messages 用户消息列表
     * @param callback 回调函数，用于处理流式响应
     * @return 返回AI助手的完整响应和消息ID
     */
    public ChatResult sendChatMessage(OwnerAccount account, List<Message> messages, Function<Message, Void> callback) {
        // 创建一个StringBuilder来收集AI助手的完整回复
        StringBuilder reasoningContent = new StringBuilder();
        StringBuilder finalContent = new StringBuilder();

        // 如果系统提示词为空，使用默认提示词
        String systemPrompt = AccountExtUtils.getAiSystemPrompt(account);
        if (StringUtils.isBlank(systemPrompt)) {
            systemPrompt = SYSTEM_PROMPT_TEMPLATE;
        }

        String baseUrl = AccountExtUtils.getAiBaseUrl(account);
        String apiKey = AccountExtUtils.getAiApiKey(account);
        String model = AccountExtUtils.getAiApiModel(account);
        double temperature = Double.parseDouble(AccountExtUtils.getAiApiTemperature(account));

        ChatResponse chatResponse;
        try {
            // chatResponse = ChatClient.sendChatRequest(account, systemPrompt, messages);
            chatResponse = ChatClient.sendStreamChatRequest(baseUrl,
                    apiKey,
                    model,
                    temperature,
                    systemPrompt,
                    messages,
                    new Consumer<ChatClient.ChatResponse>() {
                        @Override
                        public void accept(ChatClient.ChatResponse chatResp) {
                            // 收集AI助手的回复
                            if (chatResp.isChunk() && chatResp.getContent() != null) {
                                callback.apply(new Message(chatResp.getId(),
                                        "assistant",
                                        chatResp.getContent(),
                                        null));
                                finalContent.append(chatResp.getContent());
                            } else if (chatResp.isChunk() && chatResp.getReasoningContent() != null) {
                                Message reasoning = new Message(chatResp.getId(),
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