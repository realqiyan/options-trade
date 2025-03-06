package me.dingtou.options.service.impl;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import com.openai.client.OpenAIClient;
import com.openai.client.OpenAIClientAsync;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.client.okhttp.OpenAIOkHttpClientAsync;
import com.openai.core.JsonField;
import com.openai.core.JsonValue;
import com.openai.core.http.AsyncStreamResponse;
import com.openai.core.http.StreamResponse;
import com.openai.models.ChatCompletionChunk;
import com.openai.models.ChatCompletionCreateParams;
import com.openai.models.ChatModel;
import me.dingtou.options.config.ConfigUtils;
import me.dingtou.options.model.Message;
import org.springframework.stereotype.Service;

import me.dingtou.options.service.AIChatService;

@Service
public class AIChatServiceImpl implements AIChatService {


    private static final OpenAIClientAsync CLIENT;
    private static final String MODEL;
    private static final Double TEMPERATURE;

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


    @Override
    public void chat(String message, Function<Message, Void> callback) {

        ChatCompletionCreateParams createParams = ChatCompletionCreateParams.builder().model(MODEL).temperature(TEMPERATURE).maxCompletionTokens(8192).addUserMessage(message).build();

        CLIENT.chat().completions().createStreaming(createParams).subscribe(chatCompletionChunk -> {
            String id = chatCompletionChunk.id();
            chatCompletionChunk.choices().forEach(choice -> {
                JsonField<ChatCompletionChunk.Choice.Delta> deltaJsonField = choice._delta();
                Optional<Map<String, JsonValue>> object = deltaJsonField.asObject();
                if (object.isEmpty()) {
                    return;
                }
                object.get().forEach((key, value) -> {
                    if (null != value && !value.isNull() && value.asString().isPresent()) {
                        callback.apply(new Message(id, key, value.asString().get().toString()));
                    }
                });
            });
        }).onCompleteFuture().join();
    }

}
