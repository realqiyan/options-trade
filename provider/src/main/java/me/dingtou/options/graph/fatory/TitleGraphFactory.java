package me.dingtou.options.graph.fatory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import me.dingtou.options.graph.func.InputConvertFunction;
import me.dingtou.options.graph.func.OutputConvertFunction;
import me.dingtou.options.graph.node.SimpleLlmNode;

/**
 * 图工厂
 */
@Component("titleGraphFactory")
public final class TitleGraphFactory implements GraphFactory {

        private static final String GENERATE_TITLE_PROMPT = """
                        你的任务是根据用户消息生成会话标题。

                        要求：
                        * 标题必须控制在20个字以内。
                        * 标题尽可能的包含关键信息，例如：标的、交易日期。
                        * 直接返回标题，不允许附加任何其他信息和符号。
                        """;

        @Override
        public CompiledGraph buildGraph(ChatModel chatModel) throws GraphStateException {

                InputConvertFunction inputConvert = new InputConvertFunction() {
                        @Override
                        public List<Message> apply(OverAllState state, RunnableConfig config) {
                                String input = state.value("input", "");
                                SystemMessage system = SystemMessage.builder().text(GENERATE_TITLE_PROMPT).build();
                                UserMessage user = UserMessage.builder().text(input).build();
                                return List.of(system, user);
                        }
                };

                OutputConvertFunction outputConvert = new OutputConvertFunction() {
                        @Override
                        public Map<String, Object> apply(OverAllState state, RunnableConfig config, String result) {
                                return Map.of("title", result);
                        }
                };

                SimpleLlmNode generateTitleNode = new SimpleLlmNode(chatModel,
                                "generate_title",
                                inputConvert,
                                outputConvert,
                                false);

                KeyStrategyFactory keyStrategyFactory = () -> {
                        Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
                        keyStrategyMap.put("input", new ReplaceStrategy());
                        keyStrategyMap.put("title", new ReplaceStrategy());
                        return keyStrategyMap;
                };

                StateGraph graph = new StateGraph(keyStrategyFactory)
                                .addNode("generate_title", AsyncNodeActionWithConfig.node_async(generateTitleNode))
                                .addEdge(StateGraph.START, "generate_title")
                                .addEdge("generate_title", StateGraph.END);

                return graph.compile();
        }

}
