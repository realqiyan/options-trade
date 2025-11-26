package me.dingtou.options.graph.fatory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import me.dingtou.options.graph.common.ContextKeys;
import me.dingtou.options.graph.func.InputConvertFunction;
import me.dingtou.options.graph.func.OutputConvertFunction;
import me.dingtou.options.graph.node.SimpleLlmNode;
import me.dingtou.options.service.mcp.DataQueryMcpService;
import me.dingtou.options.service.mcp.KnowledgeMcpService;
import me.dingtou.options.service.mcp.OwnerQueryMcpService;

/**
 * 图工厂
 */
@Component("copilotGraphFactory")
public class CopilotGraphFactory implements GraphFactory {

        private static final String INTENT_PROMPT = """
                        你是期权交易助手,你当前的任务是分析用户输入并进行意图分类。

                        意图分类包含：
                        1. strategy: 涉及期权策略相关问题
                        2. trade: 非期权策略相关的股票和期权问题
                        3. other: 其他问题
                        意图返回: strategy, trade, other

                        返回要求：
                        意图只能选择以上分类中的一个，以纯文本返回，不要添加任何额外信息。
                        """;

        private static final String SUMMARY_PROMPT = """
                        你是期权交易助手，请基于掌握的信息进行总结。
                        """;

        @Autowired
        private KnowledgeMcpService knowledgeMcpService;
        @Autowired
        private DataQueryMcpService dataQueryMcpService;
        @Autowired
        private OwnerQueryMcpService ownerQueryMcpService;

        @Override
        public CompiledGraph buildGraph(ChatModel chatModel) throws GraphStateException {

                // 意图识别节点
                SimpleLlmNode intentNode = getIntentNode(chatModel);

                // 策略节点
                ReactAgent strategyAgent = getStrategyAgent(chatModel, knowledgeMcpService);

                // 研究节点
                ReactAgent copilotAgent = getCopilotAgent(chatModel, dataQueryMcpService, ownerQueryMcpService);

                // 总结节点
                SimpleLlmNode summaryNode = getSummaryNode(chatModel);

                // 聊天节点
                SimpleLlmNode chatNode = getChatNode(chatModel);

                KeyStrategyFactory keyStrategyFactory = () -> {
                        Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
                        keyStrategyMap.put(ContextKeys.NODE_START_TIME, new ReplaceStrategy());
                        keyStrategyMap.put(ContextKeys.INPUT, new ReplaceStrategy());
                        keyStrategyMap.put(ContextKeys.OWNER_CODE, new ReplaceStrategy());
                        keyStrategyMap.put("messages", new AppendStrategy());
                        keyStrategyMap.put("chat_messages", new AppendStrategy());
                        keyStrategyMap.put("intent", new ReplaceStrategy());
                        return keyStrategyMap;
                };

                StateGraph graph = new StateGraph(keyStrategyFactory)
                                .addNode("intent", AsyncNodeActionWithConfig.node_async(intentNode))
                                .addNode("chat", AsyncNodeActionWithConfig.node_async(chatNode))
                                .addNode("strategy", strategyAgent.asNode(true, false, "strategy"))
                                .addNode("copilot", copilotAgent.asNode(true, false, "copilot"))
                                .addNode("summary", AsyncNodeActionWithConfig.node_async(summaryNode));

                // graph.addEdge(StateGraph.START, "strategy");
                // graph.addEdge("strategy", StateGraph.END);

                graph.addEdge(StateGraph.START, "intent");
                Map<String, String> routeMap = Map.of(
                                "strategy", "strategy",
                                "trade", "copilot",
                                "other", "chat");
                graph.addConditionalEdges("intent",
                                AsyncEdgeAction.edge_async(state -> {
                                        String result = state.value("intent", "other");
                                        return routeMap.containsKey(result) ? result : "other";
                                }),
                                routeMap);
                graph.addEdge("strategy", "copilot");
                graph.addEdge("copilot", "summary");
                graph.addEdge("summary", StateGraph.END);
                graph.addEdge("chat", StateGraph.END);

                SaverConfig saverConfig = SaverConfig.builder()
                                .register(new MemorySaver())
                                .build();
                CompileConfig compileConfig = CompileConfig.builder()
                                .saverConfig(saverConfig)
                                .build();
                return graph.compile(compileConfig);

        }

        /**
         * 期权交易策略节点
         * 
         * @param chatModel           ChatModel
         * @param knowledgeMcpService KnowledgeMcpService
         * @return ReactAgent
         */
        private ReactAgent getStrategyAgent(ChatModel chatModel, KnowledgeMcpService knowledgeMcpService) {

                ToolCallback[] toolCallbacks = MethodToolCallbackProvider.builder()
                                .toolObjects(knowledgeMcpService)
                                .build()
                                .getToolCallbacks();

                ReactAgent strategyAgent = ReactAgent.builder()
                                .model(chatModel)
                                .name("strategy")
                                .instruction("""
                                                你是期权交易助手，你现在的任务是根据用户输入，使用工具获取期权交易策略。

                                                ## 用户信息

                                                * 用户token：{owner_code}
                                                * 用户输入：{input}
                                                """)
                                .tools(List.of(toolCallbacks))
                                .includeContents(false)
                                .enableLogging(true)
                                .build();
                return strategyAgent;
        }

        /**
         * 期权策略助理节点
         * 
         * @param chatModel           ChatModel
         * @param knowledgeMcpService KnowledgeMcpService
         * @return ReactAgent
         */
        private ReactAgent getCopilotAgent(ChatModel chatModel,
                        DataQueryMcpService dataQueryMcpService,
                        OwnerQueryMcpService ownerQueryMcpService) {
                ToolCallback[] toolCallbacks = MethodToolCallbackProvider.builder()
                                .toolObjects(dataQueryMcpService, ownerQueryMcpService)
                                .build()
                                .getToolCallbacks();

                ReactAgent copilotAgent = ReactAgent.builder()
                                .model(chatModel)
                                .name("copilot")
                                .instruction("""
                                                你是Qian，一名专注于股票与期权交易的对话助手。

                                                ## 期权交易策略要求

                                                1. 用户在咨询期权策略相关的问题时，需要先查询期权交易策略规则，获取规则后，必须严格基于规则详情重新制定任务计划。
                                                2. 所有推荐的期权标必须使用工具查询期权标的当前价格、隐含波动率、Delta、Theta、Gamma、未平仓合约数、当天交易量等信息。
                                                3. 提供移仓建议前，务必查询期权链信息后给出移仓建议，严格按照要求调整策略Delta，如果有必要可以同时查询多个不同到期日的期权链，给出最优移仓标的。

                                                ## delta说明

                                                1. **期权lotSize**：1张期权合约对应的股票数量
                                                * 重要说明：期权lotSize因标的物、市场和合约规格而异，在实际计算中必须通过工具查询获取准确值。
                                                * 举例说明：例如美股默认为100。
                                                2. **股票delta**：股票delta = 持有股票数 / 期权lotSize
                                                * 举例说明：例如100股BABA股票的delta = 100（持有股数）/100（期权lotSize） = 1, 100股股票delta为1。
                                                3. **期权delta**：每张期权合约都有对应的delta，买入为正数，卖出为负数
                                                * 举例说明：例如一张ATM delta0.45的期权，买入时delta=0.45，卖出时delta=-0.45。
                                                4. **策略delta**：策略delta = 持有股票数 / 期权lotSize + 持有合约数 * 期权delta
                                                * 举例说明：例如持有100股BABA股票，卖出1张ATM附近delta=0.45的期权合约，那么策略delta就是 `(100/100) + (1*-0.45) = 0.55`。
                                                5. **策略delta（归一化）**：策略delta（归一化） = 策略delta / (持股数 / 期权lotSize)
                                                * 目的说明：归一化策略delta用于标准化不同头寸规模的delta暴露，便于在不同持仓量间进行风险比较和风险管理。
                                                * 举例说明：例如持有200股BABA股票，卖出2张ATM附近delta=0.45的期权合约，那么策略delta就是 `(200/100) + (2*-0.45) = 1.1`，归一化后策略delta = 1.1 / (200/100) = 0.55。

                                                ## 期权交易关键规则

                                                * SELL CALL：股价 > 行权价 → 很可能被指派
                                                * SELL PUT：股价 < 行权价 → 很可能被指派
                                                * 价外期权：通常不会被指派
                                                - 价外期权定义：对于CALL期权，行权价 > 当前股价；对于PUT期权，行权价 < 当前股价

                                                ## 重要说明

                                                1. **信息来源要求**: 你能参考的信息包括所有历史对话记录，优先从历史对话上下文中获取完成任务所需信息，如果遇到所需信息无法获取，应首先评估是否可以通过工具查询获取，如无法通过工具获取则必须立即咨询用户，不做任何假设。
                                                2. **信息收集完成判断要求**: 你需要综合分析对话上下文中的所有信息，只有判断已经完成所有信息收集后才结束。
                                                3. **信息完整性要求**: 发现信息缺失，并且评估可以使用工具查询时，请立即使用工具，将信息补充完整。

                                                ## 用户信息

                                                * 用户token：{owner_code}
                                                * 用户输入：{input}

                                                """)
                                .tools(List.of(toolCallbacks))
                                .includeContents(true)
                                .enableLogging(false)
                                .build();
                return copilotAgent;
        }

        /**
         * 聊天节点
         * 
         * @param chatModel ChatModel
         * @return SimpleLlmNode
         */
        private SimpleLlmNode getChatNode(ChatModel chatModel) {
                InputConvertFunction inputConvert = new InputConvertFunction() {
                        @Override
                        public List<Message> apply(OverAllState state, RunnableConfig config) {
                                String input = state.value("input", "");
                                List<Message> chatMessages = state.value("chat_messages", new ArrayList<>());
                                SystemMessage system = SystemMessage.builder().text("你是一个聪明的人工智能助手。").build();
                                if (chatMessages.isEmpty()) {
                                        state.updateState(Map.of("chat_messages", system));
                                        chatMessages.add(system);
                                }
                                UserMessage user = UserMessage.builder().text(input).build();
                                state.updateState(Map.of("chat_messages", user));
                                chatMessages.add(user);

                                return chatMessages;
                        }
                };

                OutputConvertFunction outputConvert = new OutputConvertFunction() {
                        @Override
                        public Map<String, Object> apply(OverAllState state, RunnableConfig config, String result) {
                                AssistantMessage assistant = AssistantMessage.builder().content(result).build();
                                return Map.of("chat", result, "chat_messages", assistant);
                        }
                };

                return new SimpleLlmNode(chatModel,
                                "chat",
                                inputConvert,
                                outputConvert,
                                true);
        }

        /**
         * 总结节点
         * 
         * @param chatModel ChatModel
         * @return SimpleLlmNode
         */
        private SimpleLlmNode getSummaryNode(ChatModel chatModel) {

                InputConvertFunction inputConvert = new InputConvertFunction() {
                        @Override
                        public List<Message> apply(OverAllState state, RunnableConfig config) {
                                List<Message> messages = state.value("messages", new ArrayList<>());
                                UserMessage summaryMessage = UserMessage.builder().text(SUMMARY_PROMPT).build();
                                messages.add(summaryMessage);
                                return messages;
                        }
                };

                OutputConvertFunction outputConvert = new OutputConvertFunction() {
                        @Override
                        public Map<String, Object> apply(OverAllState state, RunnableConfig config, String result) {
                                return Map.of("summary", result);
                        }
                };

                return new SimpleLlmNode(chatModel,
                                "summary",
                                inputConvert,
                                outputConvert,
                                true);
        }

        /**
         * 意图识别节点
         * 
         * @param chatModel ChatModel
         * @return SimpleLlmNode
         */

        private SimpleLlmNode getIntentNode(ChatModel chatModel) {
                InputConvertFunction inputConvert = new InputConvertFunction() {
                        @Override
                        public List<Message> apply(OverAllState state, RunnableConfig config) {
                                String input = state.value("input", "");
                                SystemMessage system = SystemMessage.builder().text(INTENT_PROMPT).build();
                                UserMessage user = UserMessage.builder().text(input).build();
                                return List.of(system, user);
                        }
                };

                OutputConvertFunction outputConvert = new OutputConvertFunction() {
                        @Override
                        public Map<String, Object> apply(OverAllState state, RunnableConfig config, String result) {
                                String intent = "";
                                if (result != null && !result.isEmpty()) {
                                        intent = result.trim();
                                }
                                return Map.of("intent", intent);
                        }
                };

                return new SimpleLlmNode(chatModel,
                                "intent",
                                inputConvert,
                                outputConvert,
                                false);
        }

}
