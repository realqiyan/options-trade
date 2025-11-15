package me.dingtou.options.graph.fatory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;

import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

import me.dingtou.options.graph.node.SimpleLlmNode;
import me.dingtou.options.service.mcp.KnowledgeMcpService;

/**
 * 图工厂
 */
public final class GraphFactory {

        private GraphFactory() {
        }

        private static final String TITLE_PROMPT = """
                        你的任务是根据用户消息生成会话标题。

                        要求：
                        * 标题必须控制在20个字以内。
                        * 标题尽可能的包含关键信息，例如：标的、交易日期。
                        * 直接返回标题，不允许附加任何其他信息和符号。
                        """;

        private static final String INTENT_PROMPT = """
                        你是期权交易工具options-trade的人工智能助手，你的任务是分析用户输入并进行意图分类。

                        意图分类包含：期权问题（option）、其他问题（other）
                        意图返回: option, other

                        以JSON格式返回: {"intent": "..."}
                        """;

        private static final String SUMMARY_PROMPT = """
                        你是期权交易工具options-trade的人工智能助手，请基于掌握的信息进行总结。
                        """;

        /**
         * 注册会话标题生成图
         * 
         * @throws GraphStateException
         */
        public static StateGraph generateTitleGraph(ChatModel chatModel) throws GraphStateException {

                SimpleLlmNode titleNode = new SimpleLlmNode(chatModel,
                                "title",
                                TITLE_PROMPT,
                                "input",
                                result -> Map.of("title", result));

                KeyStrategyFactory keyStrategyFactory = () -> {
                        Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
                        keyStrategyMap.put("input", new ReplaceStrategy());
                        keyStrategyMap.put("title", new ReplaceStrategy());
                        return keyStrategyMap;
                };

                StateGraph graph = new StateGraph(keyStrategyFactory)
                                .addNode("title", AsyncNodeActionWithConfig.node_async(titleNode))
                                .addEdge(StateGraph.START, "title")
                                .addEdge("title", StateGraph.END);

                return graph;
        }

        /**
         * copilotAgent
         * 
         * @throws GraphStateException
         */
        public static StateGraph copilotAgent(ChatModel chatModel, KnowledgeMcpService knowledgeMcpService)
                        throws GraphStateException {

                SimpleLlmNode intentNode = new SimpleLlmNode(chatModel,
                                "intent",
                                INTENT_PROMPT,
                                "input",
                                result -> {
                                        JSONObject object = JSON.parseObject(result);
                                        return Map.of("intent", object.getString("intent"));
                                });

                SimpleLlmNode summaryNode = new SimpleLlmNode(chatModel,
                                "summary",
                                SUMMARY_PROMPT,
                                "input",
                                result -> {
                                        return Map.of("summary", result);
                                });

                ToolCallback[] toolCallbacks = MethodToolCallbackProvider.builder()
                                .toolObjects(knowledgeMcpService)
                                .build()
                                .getToolCallbacks();

                ReactAgent strategyAgent = ReactAgent.builder()
                                .model(chatModel)
                                .name("strategy")
                                .systemPrompt("你是期权交易工具options-trade的人工智能助手，你的任务是根据用户输入，使用工具获取期权交易策略。")
                                .tools(List.of(toolCallbacks))
                                .outputKey("strategy")
                                .build();

                KeyStrategyFactory keyStrategyFactory = () -> {
                        Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
                        keyStrategyMap.put("input", new ReplaceStrategy());
                        keyStrategyMap.put("intent", new ReplaceStrategy());
                        keyStrategyMap.put("strategy", new ReplaceStrategy());
                        keyStrategyMap.put("research", new ReplaceStrategy());
                        keyStrategyMap.put("summary", new ReplaceStrategy());
                        return keyStrategyMap;
                };

                StateGraph graph = new StateGraph(keyStrategyFactory)
                                .addNode("intent", AsyncNodeActionWithConfig.node_async(intentNode))
                                .addNode("strategy", strategyAgent.asNode(true, true, "strategy"))
                                .addNode("summary", AsyncNodeActionWithConfig.node_async(summaryNode));

                graph.addEdge(StateGraph.START, "intent");
                graph.addConditionalEdges("intent",
                                AsyncEdgeAction.edge_async(state -> {
                                        return (String) state.value("intent").orElse("other");
                                }),
                                Map.of(
                                                "option", "strategy",
                                                "other", "summary"));
                graph.addEdge("strategy", "summary");
                graph.addEdge("summary", StateGraph.END);

                return graph;
        }

}
