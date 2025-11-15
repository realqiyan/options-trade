package me.dingtou.options.graph.fatory;

import java.util.Map;

import org.springframework.ai.chat.model.ChatModel;

import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

import me.dingtou.options.graph.node.SimpleLlmNode;

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

                StateGraph graph = new StateGraph()
                                .addNode("title", AsyncNodeAction.node_async(titleNode))
                                .addEdge(StateGraph.START, "title")
                                .addEdge("title", StateGraph.END);

                return graph;
        }

        /**
         * copilotAgent
         * 
         * @throws GraphStateException
         */
        public static StateGraph copilotAgent(ChatModel chatModel) throws GraphStateException {

                SimpleLlmNode intentNode = new SimpleLlmNode(chatModel,
                                "intent",
                                INTENT_PROMPT,
                                "input",
                                result -> {
                                        JSONObject object = JSON.parseObject(result);
                                        return Map.of("intent", object.getString("intent"));
                                });

                StateGraph graph = new StateGraph()
                                .addNode("intent", AsyncNodeAction.node_async(intentNode))
                                .addEdge(StateGraph.START, "intent")
                                .addEdge("intent", StateGraph.END);

                return graph;
        }

}
