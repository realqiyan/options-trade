package me.dingtou.options.util;

import java.util.Map;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;

import me.dingtou.options.model.OwnerAccount;

/**
 * Spring AI 工具类
 */
public class SpringAiUtils {

        /**
         * 构建ChatModel
         * 
         * @param account   账户信息
         * @param isSummary 是否是总结模型
         * @return 大模型对象
         */
        public static ChatModel buildChatModel(OwnerAccount account, boolean isSummary) {
                String baseUrl = isSummary ? AccountExtUtils.getSummaryBaseUrl(account)
                                : AccountExtUtils.getAiBaseUrl(account);
                String model = isSummary ? AccountExtUtils.getSummaryApiModel(account)
                                : AccountExtUtils.getAiApiModel(account);
                String apiKey = isSummary ? AccountExtUtils.getSummaryApiKey(account)
                                : AccountExtUtils.getAiApiKey(account);
                String temperatureVal = isSummary ? AccountExtUtils.getSummaryApiTemperature(account)
                                : AccountExtUtils.getAiApiTemperature(account);
                Double temperature = Double.parseDouble(temperatureVal);

                Integer maxTokens = AccountExtUtils.getAiApiMaxTokens(account);

                if (baseUrl.contains("api.deepseek.com")) {
                        DeepSeekApi deepSeekApi = DeepSeekApi.builder()
                                        .apiKey(apiKey)
                                        .build();

                        DeepSeekChatOptions deepSeekChatOptions = DeepSeekChatOptions.builder()
                                        .model(model)
                                        .temperature(temperature)
                                        .logprobs(true)
                                        .internalToolExecutionEnabled(true)
                                        .maxTokens(maxTokens)
                                        .build();

                        return DeepSeekChatModel.builder()
                                        .deepSeekApi(deepSeekApi)
                                        .defaultOptions(deepSeekChatOptions)
                                        .build();
                }

                OpenAiApi openAiApi = OpenAiApi.builder()
                                .baseUrl(baseUrl)
                                .completionsPath("/chat/completions")
                                .apiKey(apiKey)
                                .build();
                OpenAiChatOptions openAiChatOptions = OpenAiChatOptions.builder()
                                .model(model)
                                .temperature(temperature)
                                .httpHeaders(Map.of("Content-Type", "application/json;charset=UTF-8"))
                                .streamUsage(true)
                                .internalToolExecutionEnabled(true)
                                .maxTokens(maxTokens)
                                .build();

                return OpenAiChatModel.builder()
                                .openAiApi(openAiApi)
                                .defaultOptions(openAiChatOptions)
                                .build();

        }

}
