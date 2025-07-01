package me.dingtou.options.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.model.Message;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * OpenAI API聊天客户端管理器
 * 
 * @author qiyan
 */
@Slf4j
@Component
public class ChatClient {

    /**
     * 客户端缓存
     */
    private static final Cache<String, OkHttpClient> CLIENT_CACHE = CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build();

    /**
     * 默认超时时间（秒）
     */
    private static final int DEFAULT_TIMEOUT = 60;

    /**
     * chat/completions API路径
     */
    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";

    /**
     * 发送流式聊天请求
     */
    public static CompletableFuture<ChatResponse> sendStreamChatRequest(String baseUrl,
            String apiKey,
            String model,
            Double temperature,
            List<Message> messages,
            Consumer<ChatResponse> chunkConsumer) {

        CompletableFuture<ChatResponse> completableFuture = new CompletableFuture<>();
        StringBuilder contentBuilder = new StringBuilder();
        StringBuilder reasoningContentBuilder = new StringBuilder();
        String[] messageId = { null };

        try {
            OkHttpClient client = getClient(baseUrl);

            // 构建请求体
            JSONObject requestBody = buildRequestBody(messages, model, temperature, true);

            // 构建请求
            Request request = new Request.Builder()
                    .url(baseUrl + CHAT_COMPLETIONS_PATH)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody.toJSONString(), MediaType.parse("application/json")))
                    .build();

            // 发送请求
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    log.error("API流式请求失败: {}", e.getMessage(), e);
                    throw new RuntimeException("API流式请求失败", e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        String errorMessage = "API请求失败: " + response.code() + " body:" + response.body().string();
                        log.error(errorMessage);
                        // completableFuture 失败返回
                        completableFuture.completeExceptionally(new RuntimeException(errorMessage));
                        return;
                    }

                    try (ResponseBody responseBody = response.body()) {
                        if (responseBody == null) {
                            // completableFuture 失败返回
                            completableFuture.completeExceptionally(new RuntimeException("响应体为空"));
                            return;
                        }

                        // 处理SSE流
                        try (okio.BufferedSource source = responseBody.source()) {
                            while (!source.exhausted()) {
                                String line = source.readUtf8LineStrict();

                                // 过滤空行和SSE注释
                                if (line.isEmpty() || line.startsWith(":")) {
                                    continue;
                                }

                                // 处理SSE数据
                                if (line.startsWith("data: ")) {
                                    String data = line.substring(6);

                                    // 处理[DONE]标记
                                    if ("[DONE]".equals(data)) {
                                        break;
                                    }

                                    // 解析JSON数据
                                    JSONObject jsonChunk = JSON.parseObject(data);
                                    ChatResponse chunk = parseChatChunk(jsonChunk);

                                    // 保存消息ID
                                    if (messageId[0] == null && chunk.getId() != null) {
                                        messageId[0] = chunk.getId();
                                    }

                                    // 收集内容
                                    if (null != chunk.getContent()) {
                                        contentBuilder.append(chunk.getContent());
                                    }
                                    if (null != chunk.getReasoningContent()) {
                                        reasoningContentBuilder.append(chunk.getReasoningContent());
                                    }

                                    // 调用消费函数
                                    chunkConsumer.accept(chunk);
                                }
                            }
                            // 构建响应结果
                            ChatResponse chatResponse = new ChatResponse();
                            chatResponse.setId(messageId[0]);
                            chatResponse.setContent(contentBuilder.toString());
                            chatResponse.setReasoningContent(reasoningContentBuilder.toString());
                            completableFuture.complete(chatResponse);
                        }
                    }
                }
            });

        } catch (Exception e) {
            log.error("发送流式聊天请求失败: {}", e.getMessage(), e);
            throw new RuntimeException("发送流式聊天请求失败", e);
        }
        return completableFuture;

    }

    /**
     * 构建请求体
     *
     * @param messages    消息列表
     * @param model       模型
     * @param temperature 温度
     * @param stream      是否流式请求
     * 
     * @return 请求体JSON对象
     */
    private static JSONObject buildRequestBody(List<Message> messages,
            String model,
            double temperature,
            boolean stream) {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", model);
        requestBody.put("temperature", temperature);
        requestBody.put("stream", stream);

        JSONArray messagesArray = new JSONArray();

        // 添加用户消息
        for (Message message : messages) {
            JSONObject messageObj = new JSONObject();
            messageObj.put("role", message.getRole());
            messageObj.put("content", message.getContent());
            messagesArray.add(messageObj);
        }
        requestBody.put("messages", messagesArray);

        return requestBody;
    }

    /**
     * 解析聊天数据块
     *
     * @param jsonChunk JSON数据块
     * @return 聊天数据块
     */
    private static ChatResponse parseChatChunk(JSONObject jsonChunk) {
        ChatResponse chunk = new ChatResponse(true);
        chunk.setId(jsonChunk.getString("id"));

        JSONArray choices = jsonChunk.getJSONArray("choices");
        if (choices != null && !choices.isEmpty()) {
            JSONObject choice = choices.getJSONObject(0);
            JSONObject delta = choice.getJSONObject("delta");
            if (delta != null) {
                chunk.setContent(delta.getString("content"));
                chunk.setReasoningContent(delta.getString("reasoning_content"));
            }
        }

        return chunk;
    }

    /**
     * 获取OkHttpClient客户端
     *
     * @param baseUrl API基础URL
     * @return OkHttpClient实例
     */
    private static OkHttpClient getClient(String baseUrl) {
        try {
            return CLIENT_CACHE.get(baseUrl, () -> {
                return new OkHttpClient.Builder()
                        .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                        .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                        .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                        .build();
            });
        } catch (ExecutionException e) {
            throw new RuntimeException("获取OkHttpClient失败", e);
        }
    }

    /**
     * 聊天响应结果
     */
    @Data
    public static class ChatResponse {
        /**
         * 是否区块（流式部分结果）
         */
        private boolean chunk;
        /**
         * 消息ID
         */
        private String id;
        /**
         * 内容
         */
        private String content;
        /**
         * 推理内容
         */
        private String reasoningContent;

        public ChatResponse() {
            this.chunk = false;
        }

        public ChatResponse(boolean chunk) {
            this.chunk = chunk;
        }
    }

}