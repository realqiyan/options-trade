package me.dingtou.options.manager;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.openai.client.OpenAIClientAsync;
import com.openai.client.okhttp.OpenAIOkHttpClientAsync;
import com.openai.core.JsonField;
import com.openai.core.JsonValue;
import com.openai.models.ChatCompletionChunk;
import com.openai.models.ChatCompletionCreateParams;
import com.openai.models.ChatCompletionMessage;

import lombok.Getter;
import me.dingtou.options.model.Message;

import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.util.AccountExtUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
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

    /**
     * 系统提示词，规范AI助手返回结构
     */
    private static final String SYSTEM_PROMPT_TEMPLATE = """
            您是通过卖期权赚取权利金的专家，您同时也是股票技术分析专家，您的职责是分析用户提交的数据，解读股票价格走势，然后按照用户需求，提供期权策交易策略建议，要求：
            1. 通过卖期权赚取权利金，仅考虑单腿策略和车轮策略。
            2. 充分利用提供的数据，逐条地分析，根据需要自动计算技术指标。
            3. 综合分析结论，提供专业期权策交易策略和深度的推荐理由。

            交易策略建议模版：
            # 股票数据分析
            ## 价格走势分析 //基于提供的K线数据，详细分析价格走势，至少要包含EMA、BOLL、RSI、MACD等指标，并提供分析依据；
            ## 支撑位分析 //基于提供的K线数据，技术指标数据，综合分析关键指标和支持位，并提供分析依据；

            # 期权交易建议
            ## 标的分析 //结合股票数据分析结论做标的分析
            ### 交易标的分析 //基于提供的期权标的中的Delta、Theta、Gamma、年化收益率、波动率、交易量数据，结合股票走势和价格指标，逐条做期权交易标的分析；
            ## 交易方案 //提供两套方案，交易方案包含选择的标的代码，行权价、年化、风险应对方式、详细的选择理由，理由需要提供分析依据；
            ### 推荐方案 //最优方案；
            ### 备选方案 //低风险方案；

            ## 风控与调整计划 //Rolling策略、行权应对、止损触发条件；
            ### Rolling策略 //推荐Rolling策略，详细说明；
            ### 止损触发条件 //风控止损策略

            ## 交易任务 //用于系统生成交易任务的json，要求JSON格式必须正确。
            ```json
            {
            "actions": [
                {
                "type": "sell_option", // 操作类型，可选值：buy_option, sell_option, close_option, roll_option
                "underlyingCode": "US.BABA", // 标的代码，格式为"市场.代码"
                "code": "BABA250321C150000", // 期权代码
                "strikePrice": "130.00", // 行权价（如果是期权操作）
                "price": "1.50", // 建议交易价格
                "quantity": 1, // 建议交易数量
                "side": "sell", // 交易方向，可选值：buy, sell
                "strategyId": "81bab49eac5d427b967e93e5e93c9c68", // 策略ID
                "startTime": "2025-03-15", // 建议执行时间
                "endTime": "2025-03-17", // 建议结束时间
                "description": "BABA 3月21日到期的150美元看涨期权", // 操作描述
                "condition": "$currPrice > 135.00" // 执行条件$currPrice代表当前价格
                }
            ]
            }
            ```
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
        // 保存消息ID
        final String[] messageId = { null };

        String aiApiTemperature = AccountExtUtils.getAiApiTemperature(account);

        // 如果系统提示词为空，使用默认提示词
        String systemPrompt = AccountExtUtils.getAiSystemPrompt(account);
        if (StringUtils.isBlank(systemPrompt)) {
            systemPrompt = SYSTEM_PROMPT_TEMPLATE;
        }

        ChatCompletionCreateParams.Builder builder = ChatCompletionCreateParams.builder()
                .model(AccountExtUtils.getAiApiModel(account))
                .temperature(Double.valueOf(aiApiTemperature))
                .maxCompletionTokens(16384)
                .addSystemMessage(systemPrompt);
        for (Message message : messages) {
            ChatCompletionMessage chatMessage = ChatCompletionMessage.builder()
                    .role(JsonValue.from(message.getRole()))
                    .content(message.getContent())
                    .refusal("")
                    .build();
            builder.addMessage(chatMessage);
        }
        ChatCompletionCreateParams createParams = builder.build();

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
                                callback.apply(new Message(id, "assistant", content, null));
                                finalContent.append(content);
                            } else if ("reasoning_content".equals(key)) {
                                callback.apply(new Message(id, "assistant", null, content));
                                reasoningContent.append(content);
                            }
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