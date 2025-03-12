package me.dingtou.options.manager;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.openai.client.OpenAIClientAsync;
import com.openai.client.okhttp.OpenAIOkHttpClientAsync;
import com.openai.core.JsonField;
import com.openai.core.JsonValue;
import com.openai.models.ChatCompletionChunk;
import com.openai.models.ChatCompletionCreateParams;
import lombok.Getter;
import me.dingtou.options.model.Message;

import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.util.AccountExtUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 聊天管理器
 *
 * @author qiyan
 */
@Component
public class ChatManager {
    /**
     * 价格实时缓存
     */
    private static final Cache<String, OpenAIClientAsync> CLIENT = CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build();

    private static final String SYSTEM_MESSAGE = """
            您现在是期权交易专家，您的职责是分析用户提交的数据，解读市场图表形态和技术指标信号，然后按照用户需求提供期权策交易略建议，要求：
            1. 期权无关的问题，请不要回答。
            2. 仅考虑卖期权策略，主要目的是赚取期权合约权利金。
            3. 仅考虑单腿策略和车轮策略。
            4. 需要充分利用用户提供的数据，不要遗漏，逐条且深度地分析用户提供的数据，并总结。
            5. 基于分析总结结论，提供专业期权策交易策略。
            """;

    /**
     * 发送聊天消息并处理流式响应
     *
     * @param account  账号
     * @param message  用户消息
     * @param callback 回调函数，用于处理流式响应
     * @return 返回AI助手的完整响应和消息ID
     */
    public ChatResult sendChatMessage(OwnerAccount account, String message, Function<Message, Void> callback) {
        // 创建一个StringBuilder来收集AI助手的完整回复
        StringBuilder reasoningContent = new StringBuilder();
        StringBuilder finalContent = new StringBuilder();
        // 保存消息ID
        final String[] messageId = { null };

        String aiApiTemperature = AccountExtUtils.getAiApiTemperature(account);
        ChatCompletionCreateParams createParams = ChatCompletionCreateParams.builder()
                .model(AccountExtUtils.getAiApiModel(account))
                .temperature(Double.valueOf(aiApiTemperature))
                .maxCompletionTokens(16384)
                .addSystemMessage(SYSTEM_MESSAGE)
                .addUserMessage(message).build();

        getClient(account).chat().completions().createStreaming(createParams).subscribe(chatCompletionChunk -> {
            String id = chatCompletionChunk.id();
            // 保存第一个消息块的ID作为整个消息的ID
            if (messageId[0] == null) {
                messageId[0] = id;
            }

            chatCompletionChunk.choices().forEach(choice -> {
                JsonField<ChatCompletionChunk.Choice.Delta> deltaJsonField = choice._delta();
                Optional<Map<String, JsonValue>> object = deltaJsonField.asObject();
                if (object.isEmpty()) {
                    return;
                }
                object.get().forEach((key, value) -> {
                    if (null != value && !value.isNull() && value.asString().isPresent()) {
                        String content = value.asString().get().toString();
                        if (StringUtils.isNotBlank(content)) {
                            // 收集AI助手的回复
                            if ("content".equals(key)) {
                                finalContent.append(content);
                            } else if ("reasoning_content".equals(key)) {
                                reasoningContent.append(content);
                            }
                            callback.apply(new Message(id, key, content));
                        }
                    }
                });
            });
        }).onCompleteFuture().join();

        return new ChatResult(messageId[0], finalContent.toString(), reasoningContent.toString());
    }

    private OpenAIClientAsync getClient(OwnerAccount account) {
        try {
            return CLIENT.get(account.getOwner(), () -> {
                String baseUrl = AccountExtUtils.getAiBaseUrl(account);
                String apiKey = AccountExtUtils.getAiApiKey(account);
                return OpenAIOkHttpClientAsync.builder().apiKey(apiKey).baseUrl(baseUrl).build();
            });
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
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