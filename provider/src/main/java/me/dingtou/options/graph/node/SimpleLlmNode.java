package me.dingtou.options.graph.node;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;

import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.model.Message;
import reactor.core.publisher.Flux;

/**
 * 通用简单的LLM节点
 */
@Slf4j
public class SimpleLlmNode extends BaseNode {

    /**
     * chatClient
     */
    private final ChatClient chatClient;

    /**
     * 节点名称
     */
    private final String name;

    /**
     * systemPrompt
     */
    private final String systemPrompt;

    /**
     * inputKey
     */
    private final String inputKey;

    /**
     * resultCall
     */
    private final Function<String, Map<String, Object>> resultCall;

    /**
     * 构造函数
     * 
     * @param chatModel    ChatModel
     * @param name         节点名称
     * @param systemPrompt 系统提示
     * @param inputKey     输入键
     * @param resultCall   结果调用函数
     */
    public SimpleLlmNode(ChatModel chatModel,
            String name,
            String systemPrompt,
            String inputKey,
            Function<String, Map<String, Object>> resultCall) {
        this.chatClient = ChatClient.create(chatModel);
        this.name = name;
        this.systemPrompt = systemPrompt;
        this.inputKey = inputKey;
        this.resultCall = resultCall;
    }

    @Override
    public Map<String, Object> apply(OverAllState state, RunnableConfig config) throws Exception {
        String userMessage = (String) state.value(inputKey).orElse("");
        String messageId = UUID.randomUUID().toString();
        // 使用流式输出
        StringBuilder fullContent = new StringBuilder();
        Flux<String> contentFlux = chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .stream()
                .content().map(chunk -> {
                    // 实时输出
                    callback(state, config, new Message(messageId, "assistant", chunk));
                    fullContent.append(chunk);
                    return chunk;
                });

        // 等待流完成并返回结果
        String result = contentFlux.collectList()
                .block()
                .stream()
                .collect(Collectors.joining());

        return resultCall.apply(result);
    }

    @Override
    protected String name() {
        return this.name;
    }

}
