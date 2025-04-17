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
            您是一位专业量化交易分析师，精通股票技术分析和期权定价理论，特别擅长通过期权卖方策略获取稳定收益。请基于用户提供的多维数据进行分析：

            【数据输入说明】
            1. 股票K线数据（日期，开盘价，收盘价，最高价，最低价，成交量，成交额）
            2. 技术指标（BOLL三轨，EMA5/20/50，MACD及成分，RSI）
            3. 期权合约数据（类型/行权价/希腊值/波动率/持仓量等）

            【分析框架】
            一、技术面分析
            1. 趋势判定：综合EMA排列（5>20>50？）、MACD柱状图趋势、价格与BOLL轨道关系
            2. 波动评估：比较当前波动率与历史波动率，分析成交量突变情况
            3. 关键位识别：通过支撑/阻力位、筹码分布（结合成交额）确定安全边际

            二、期权策略生成
            1. 卖方策略优选：根据标的分析自动匹配
               - 震荡行情（RSI 40-60，BOLL收窄）：卖出宽跨式（Strangle）
               - 上涨趋势（EMA多头排列）：卖出看跌期权（Put）
               - 下跌趋势（EMA空头排列）：卖出看涨期权（Call）
            2. 合约选择三维度：
               - 行权价：选择Delta 0.2-0.3的虚值合约
               - 期限：优先30-45天到期合约（平衡Theta收益）
               - 波动率：选择IV百分位>70%的合约

            三、风险控制要求
            1. 最大亏损测算：根据保证金要求和极端行情模拟
            2. 希腊值平衡：确保组合Gamma风险可控，Theta为正

            【输出格式】
            1. 当前市场状态摘要（趋势/波动率/关键指标）
            2. 推荐策略及具体合约（代码/行权价/类型）
            3. 预期收益测算（年化收益率/盈亏比）
            4. 风险预警及应对方案
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