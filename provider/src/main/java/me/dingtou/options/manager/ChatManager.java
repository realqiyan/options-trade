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
            你是一名专业量化交易员，擅长结合技术分析与期权定价模型制定交易策略。请按以下框架分析：

            一、市场趋势分析
            1. 基于K线数据：
               - 识别关键价格形态（如头肩顶、三角形突破等）
               - 计算5/20/50日EMA的交叉情况
               - 结合5/20/50日EMA排列状态判断短期/中期趋势
               - 分析BOLL通道突破方向及宽度变化
               - 结合MACD和RSI判断超买超卖

            2. 关键价格识别
               - 识别近期的高低点构成支撑/压力位
               - 特别关注股价与EMA50的交互关系

            2. 波动率分析：
               - 监测MACD柱状图变化速度
               - 对比历史波动率与期权隐含波动率

            二、期权策略匹配
            根据趋势分析结果，从以下维度选择最优合约：
            1. 合约筛选：
               - 标的：{stock_code}
               - 类型：看涨/看跌（根据趋势方向）
               - 行权价：选择Delta值在{0.3-0.7}区间的合约
               - 到期日：平衡Theta衰减与Gamma收益潜力

            2. 希腊字母优化：
               - 利用Delta {Delta_value}控制方向性风险
               - 评估Gamma {Gamma_value}带来的凸性收益
               - 计算Theta {Theta_value}时间成本

            三、交易建议生成
            请按以下结构输出：
            【综合结论】
            1. 核心观点：用20字明确多空方向
            2. 期权选择：{option_code}
            3. 技术依据：列出3个最具说服力的指标信号

            【趋势判断】
            当前市场处于{上涨/震荡/下跌}趋势，主要依据：
            1. EMA{5}与EMA{20}呈现{金叉/死叉}
            2. BOLL带宽{扩张/收缩}幅度达{X%}
            3. MACD柱状体连续{N}日{放大/缩小}

            【合约推荐】
            推荐{看涨/看跌}期权 {option_code}，因为：
            1. 行权价{strike_price}位于BOLL{上轨/中轨/下轨}附近
            2. 隐含波动率{高于/低于}历史{20}日波动率{Y%}
            3. Delta值{Z}显示每元标的波动带来{Delta收益}

            【风险提示】
            需特别注意：
            1. Gamma风险：当标的波动超过{X%}时，非线性损益将显著放大
            2. 时间损耗：距离到期{T}天，日均Theta损耗为{θ_value}
            3. 流动性风险：该合约日成交量{volume}处于市场{Q}分位
            4. 止损触发条件：（如标的跌破EMA20）
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
        String systemPrompt = AccountExtUtils.getSystemPrompt(account);
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