package me.dingtou.options.graph.node;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;

import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.graph.func.InputConvertFunction;
import me.dingtou.options.graph.func.OutputConvertFunction;
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
     * 输入处理函数
     */
    private final InputConvertFunction inputConvert;

    /**
     * 输出处理函数
     */
    private final OutputConvertFunction outputConvert;

    /**
     * 是否流式输出
     */
    private boolean isStream;

    /**
     * 构造函数
     * 
     * @param chatModel     ChatModel
     * @param name          节点名称
     * @param inputConvert  输入键
     * @param outputConvert 结果调用函数
     */
    public SimpleLlmNode(ChatModel chatModel,
            String name,
            InputConvertFunction inputConvert,
            OutputConvertFunction outputConvert,
            boolean isStream) {
        this.chatClient = ChatClient.create(chatModel);
        this.name = name;
        this.inputConvert = inputConvert;
        this.outputConvert = outputConvert;
        this.isStream = isStream;
    }

    @Override
    public Map<String, Object> apply(OverAllState state,
            RunnableConfig config,
            Function<Message, Void> callback,
            Function<Message, Void> failCallback) throws Exception {
        // String userMessage = (String) state.value(inputKey).orElse("");
        String messageId = name() + "_" + state.data().getOrDefault("__node_start_time__", System.currentTimeMillis());
        // 使用流式输出
        StringBuilder fullContent = new StringBuilder();
        List<org.springframework.ai.chat.messages.Message> messages = inputConvert.apply(state, config);

        Flux<String> contentFlux = chatClient.prompt().messages(messages).stream().chatResponse()
                .map(response -> {
                    String chunk = Optional.ofNullable(response)
                            .map(ChatResponse::getResult)
                            .map(Generation::getOutput)
                            .map(AbstractMessage::getText)
                            .orElse(null);
                    // 实时输出
                    if (isStream) {
                        callback.apply(new Message(messageId, "assistant", chunk));
                    }

                    fullContent.append(chunk);
                    return response;
                }).map(response -> {
                    String chunk = Optional.ofNullable(response)
                            .map(ChatResponse::getResult)
                            .map(Generation::getOutput)
                            .map(AbstractMessage::getText)
                            .orElse(null);
                    return chunk;
                });

        // 等待流完成并返回结果
        String result = contentFlux.collectList()
                .block()
                .stream()
                .collect(Collectors.joining());

        log.info("SimpleLlmNode {} result: {}", name, result);

        return outputConvert.apply(state, config, result);
    }

    @Override
    protected String name() {
        return this.name;
    }

}
