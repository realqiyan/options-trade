package me.dingtou.options.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import me.dingtou.options.model.Message;
import me.dingtou.options.model.OwnerAccount;

/**
 * LLM 工具类
 */
public class LlmUtils {

    /**
     * 过滤模型思考内容 <think> XML标签
     * 
     * @param result
     * @return
     */
    public static String filterThink(String result) {
        if (StringUtils.isBlank(result) || !result.startsWith("<think>")) {
            return result;
        }
        return result.replaceAll("^<think>[\\s\\S]*</think>[\n\t\r ]*", "");
    }

    /**
     * 构建ChatModel
     * 
     * @param account   账户信息
     * @param isSummary 是否是总结模型
     * @return 大模型对象
     */
    public static ChatModel buildChatModel(OwnerAccount account, boolean isSummary) {
        String baseUrl = isSummary ? AccountExtUtils.getSummaryBaseUrl(account) : AccountExtUtils.getAiBaseUrl(account);
        String model = isSummary ? AccountExtUtils.getSummaryApiModel(account) : AccountExtUtils.getAiApiModel(account);
        String apiKey = isSummary ? AccountExtUtils.getSummaryApiKey(account) : AccountExtUtils.getAiApiKey(account);
        String temperatureVal = isSummary ? AccountExtUtils.getSummaryApiTemperature(account)
                : AccountExtUtils.getAiApiTemperature(account);
        Double temperature = Double.parseDouble(temperatureVal);

        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(model)
                .customHeaders(Map.of("Content-Type", "application/json;charset=UTF-8"))
                .temperature(temperature)
                .returnThinking(true)
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    /**
     * 构建流式ChatModel
     * 
     * @param account   账户信息
     * @param isSummary 是否是总结模型
     * @return 大模型对象
     */
    public static StreamingChatModel buildStreamingChatModel(OwnerAccount account, boolean isSummary) {
        String baseUrl = isSummary ? AccountExtUtils.getSummaryBaseUrl(account) : AccountExtUtils.getAiBaseUrl(account);
        String model = isSummary ? AccountExtUtils.getSummaryApiModel(account) : AccountExtUtils.getAiApiModel(account);
        String apiKey = isSummary ? AccountExtUtils.getSummaryApiKey(account) : AccountExtUtils.getAiApiKey(account);
        String temperatureVal = isSummary ? AccountExtUtils.getSummaryApiTemperature(account)
                : AccountExtUtils.getAiApiTemperature(account);
        Double temperature = Double.parseDouble(temperatureVal);

        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(model)
                .customHeaders(Map.of("Content-Type", "application/json;charset=UTF-8"))
                .temperature(temperature)
                .returnThinking(true)
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    /**
     * 转换Message列表为ChatMessage列表
     * 
     * @param historyMessages 历史消息列表
     * @return ChatMessage列表
     */
    public static List<ChatMessage> convertMessage(List<Message> historyMessages) {
        List<ChatMessage> chatMessages = new ArrayList<>();
        if (historyMessages != null) {
            for (Message message : historyMessages) {
                chatMessages.add(convertMessage(message));
            }
        }
        return chatMessages;
    }

    /**
     * 转换Message为ChatMessage
     * 
     * @param message 消息
     * @return ChatMessage
     */
    public static ChatMessage convertMessage(Message message) {
        ChatMessage chatMessage = null;
        if ("user".equals(message.getRole())) {
            chatMessage = new UserMessage(message.getContent());
        } else if ("assistant".equals(message.getRole())) {
            chatMessage = new AiMessage(message.getContent());
        } else if ("system".equals(message.getRole())) {
            chatMessage = new SystemMessage(message.getContent());
        }
        return chatMessage;
    }

}
