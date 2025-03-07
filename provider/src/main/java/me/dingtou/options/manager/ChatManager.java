package me.dingtou.options.manager;

import com.openai.client.OpenAIClientAsync;
import com.openai.client.okhttp.OpenAIOkHttpClientAsync;
import com.openai.core.JsonField;
import com.openai.core.JsonValue;
import com.openai.models.ChatCompletionChunk;
import com.openai.models.ChatCompletionCreateParams;
import lombok.Getter;
import me.dingtou.options.config.ConfigUtils;
import me.dingtou.options.model.Message;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * 聊天管理器
 * 
 * @author qiyan
 */
@Component
public class ChatManager {

    private static final OpenAIClientAsync CLIENT;
    private static final String MODEL;
    private static final Double TEMPERATURE;

    private static final String SYSTEM_MESSAGE = """
            ## 定位
            - 期权交易专家：您现在是期权交易专家，持有CFA和FRM双认证，您的核心职责是分析用户提交的数据，一步一步分析技术指标，然后按照用户需求提供专业期权策略建议。

            ## 核心功能架构
            - 策略生成模块
                - 支持单腿/车轮（WheelStrategy）/跨式/宽跨式/蝶式/领子等复杂组合策略的定制化构建
                - 根据客户要求、标的资产类型(股票/指数)、K线分析，生成交易策略
            - 风险管理体系
                - 自动计算策略的最大亏损/盈利概率分布
                - 动态止损建议（基于波动率调整的移动止损机制）
            - 数据分析层
                - 基于用户提供的K线数据进行自动分析（技术指标如RSI、EMA、MACD、BOLL等）
                - 流动性评估（买卖价差/市场深度监控）
            """;

    static {
        // 模型
        MODEL = ConfigUtils.getConfig("ai.api.model");

        // 基础配置
        String baseUrl = ConfigUtils.getConfig("ai.base_url");
        String apiKey = ConfigUtils.getConfig("ai.api.key");
        CLIENT = OpenAIOkHttpClientAsync.builder().apiKey(apiKey).baseUrl(baseUrl).build();

        // 温度
        String temperature = ConfigUtils.getConfig("ai.api.temperature");
        TEMPERATURE = null == temperature ? 1.0 : Float.parseFloat(temperature);
    }

    /**
     * 发送聊天消息并处理流式响应
     *
     * @param message  用户消息
     * @param callback 回调函数，用于处理流式响应
     * @return 返回AI助手的完整响应和消息ID
     */
    public ChatResult sendChatMessage(String message, Function<Message, Void> callback) {
        // 创建一个StringBuilder来收集AI助手的完整回复
        StringBuilder reasoningContent = new StringBuilder();
        StringBuilder finalContent = new StringBuilder();
        // 保存消息ID
        final String[] messageId = { null };

        ChatCompletionCreateParams createParams = ChatCompletionCreateParams.builder()
                .model(MODEL)
                .temperature(TEMPERATURE)
                .maxCompletionTokens(16384)
                .addSystemMessage(SYSTEM_MESSAGE)
                .addUserMessage(message).build();

        CLIENT.chat().completions().createStreaming(createParams).subscribe(chatCompletionChunk -> {
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