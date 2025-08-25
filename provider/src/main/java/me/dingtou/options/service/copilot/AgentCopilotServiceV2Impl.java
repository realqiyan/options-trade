package me.dingtou.options.service.copilot;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson2.JSON;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.PartialThinking;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import me.dingtou.options.constant.AccountExt;
import me.dingtou.options.manager.KnowledgeManager;
import me.dingtou.options.manager.OwnerManager;
import me.dingtou.options.model.Message;
import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.model.OwnerChatRecord;
import me.dingtou.options.model.OwnerKnowledge;
import me.dingtou.options.model.copilot.ToolCallRequest;
import me.dingtou.options.service.AssistantService;
import me.dingtou.options.service.AuthService;
import me.dingtou.options.util.AccountExtUtils;
import me.dingtou.options.util.DateUtils;
import me.dingtou.options.util.EscapeUtils;
import me.dingtou.options.util.LlmUtils;
import me.dingtou.options.util.McpUtils;
import me.dingtou.options.util.TemplateRenderer;

/**
 * Agent模式
 */
@Slf4j
@Component
public class AgentCopilotServiceV2Impl implements CopilotService {

    @Autowired
    private List<ToolProcesser> toolProcessers;

    @Autowired
    private AssistantService assistantService;

    @Autowired
    private AuthService authService;

    @Autowired
    private OwnerManager ownerManager;

    @Autowired
    private KnowledgeManager knowledgeManager;

    @Override
    public String mode() {
        return "agent_v2";
    }

    @Override
    public String start(String owner,
            String sessionId,
            String title,
            Message message,
            Function<Message, Void> callback,
            Function<Message, Void> failCallback) {
        log.info("[Agent] 开始新会话, owner={}, sessionId={}, title={}", owner, sessionId, title);

        // 验证账户
        OwnerAccount account = validateAccount(owner, failCallback);
        if (account == null) {
            return sessionId;
        }

        // 编码owner
        String ownerCode = authService.encodeOwner(owner);

        // 初始化MCP服务
        initMcpServer(account);

        // 构建包含MCP工具描述的系统提示词
        String sysMsg = buildSystemPrompt(account, ownerCode);
        Message systemMessage = new Message("system", sysMsg);
        saveChatRecord(owner, sessionId, title, systemMessage);

        String userMsg = buildContinuePrompt(owner, ownerCode, message.getContent());
        Message userMessage = new Message("user", userMsg);

        work(account, title, userMessage, callback, failCallback, sessionId, List.of(systemMessage));

        return sessionId;
    }

    @Override
    public void continuing(String owner,
            String sessionId,
            Message message,
            Function<Message, Void> callback,
            Function<Message, Void> failCallback) {
        log.info("[Agent] 继续会话, owner={}, sessionId={}", owner, sessionId);

        // 验证账户
        OwnerAccount account = validateAccount(owner, failCallback);
        if (account == null) {
            return;
        }

        // 获取历史消息(user & assistant)
        List<OwnerChatRecord> records = assistantService.listRecordsBySessionId(owner, sessionId);
        if (records == null || records.isEmpty()) {
            failCallback.apply(new Message("assistant", "无法找到历史对话记录"));
            return;
        }

        // 转换历史消息为Message对象
        List<Message> messages = new ArrayList<>(records);

        // 获取会话标题
        String title = records.get(0).getTitle();

        // 编码owner
        String ownerCode = authService.encodeOwner(owner);

        // 初始化MCP服务（避免基于老会话继续沟通找不到客户端的问题）
        initMcpServer(account);

        // 构建系统上下文
        String continueMessage = buildContinuePrompt(owner, ownerCode, message.getContent());

        // 添加新消息
        Message newMessage = new Message("user", continueMessage);

        work(account, title, newMessage, callback, failCallback, sessionId, messages);
    }

    /**
     * 验证账户是否存在
     * 
     * @param owner        账户所有者
     * @param failCallback 失败回调
     * @return 账户信息，如果不存在则返回null
     */
    private OwnerAccount validateAccount(String owner, Function<Message, Void> failCallback) {
        OwnerAccount account = ownerManager.queryOwnerAccount(owner);
        if (account == null) {
            log.error("[Agent] 账号不存在, owner={}", owner);
            failCallback.apply(new Message("assistant", "账号不存在"));
            return null;
        }
        return account;
    }

    /**
     * Agent Work
     * 
     * @param account         账户信息
     * @param title           会话标题
     * @param newMessage      新消息
     * @param callback        回调函数
     * @param failCallback    失败回调函数
     * @param sessionId       会话ID
     * @param historyMessages 历史消息列表
     */
    private void work(OwnerAccount account,
            String title,
            Message newMessage,
            Function<Message, Void> callback,
            Function<Message, Void> failCallback,
            String sessionId,
            List<Message> historyMessages) {

        // 最大循环次数防止无限循环
        int maxIterations = 15;
        int iteration = 0;

        List<ChatMessage> chatMessages = LlmUtils.convertMessage(historyMessages);

        String owner = account.getOwner();
        // 保存用户消息
        saveChatRecord(owner, sessionId, title, newMessage);
        chatMessages.add(LlmUtils.convertMessage(newMessage));

        StreamingChatModel chatModel = LlmUtils.buildStreamingChatModel(account, false);
        StreamingChatModel summaryModel = LlmUtils.buildStreamingChatModel(account, true);
        final StringBuffer finalResponse = new StringBuffer();
        // 是否需要切换summary模型
        final AtomicBoolean needSummary = new AtomicBoolean(false);
        while (iteration++ < maxIterations && finalResponse.isEmpty()) {
            final StringBuffer returnMessage = new StringBuffer();
            final StringBuffer reasoningMessage = new StringBuffer();
            String messageId = "assistant" + System.currentTimeMillis();
            final CountDownLatch latch = new CountDownLatch(1);
            // 切换模型
            StreamingChatModel model = needSummary.get() && null != summaryModel ? summaryModel : chatModel;
            log.info("Title: {}  current iteration: {}  chatMessages size: {}", title, iteration, chatMessages.size());
            model.chat(chatMessages, new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String partialResponse) {
                    returnMessage.append(partialResponse);
                    callback.apply(new Message(messageId, "assistant", partialResponse));
                }

                @Override
                public void onPartialThinking(PartialThinking partialThinking) {
                    reasoningMessage.append(partialThinking.text());
                    Message thinkingMessage = new Message();
                    thinkingMessage.setMessageId(messageId);
                    thinkingMessage.setRole("assistant");
                    thinkingMessage.setReasoningContent(partialThinking.text());
                    callback.apply(thinkingMessage);
                }

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    // 保存模型回复
                    String finalMsg = returnMessage.toString();

                    if (StringUtils.isBlank(finalMsg) && completeResponse.aiMessage().hasToolExecutionRequests()) {
                        finalMsg = convertToolCall(completeResponse.aiMessage().toolExecutionRequests());
                        // returnMessage 为空时客户端未收到任何命令，此时直接输出工具调用请求
                        callback.apply(new Message(messageId, "assistant", finalMsg));
                        log.info("hasToolExecutionRequests finalMsg: {}", finalMsg);
                    }

                    Message assistantMessage = new Message("assistant", finalMsg);
                    assistantMessage.setMessageId(messageId);
                    if (!reasoningMessage.isEmpty()) {
                        assistantMessage.setReasoningContent(reasoningMessage.toString());
                    }
                    saveChatRecord(owner, sessionId, title, assistantMessage);
                    chatMessages.add(new AiMessage(finalMsg));

                    // 提取工具调用信息
                    ToolProcesser toolProcesser = findToolProcesser(finalMsg);
                    if (null == toolProcesser) {
                        // 没有工具调用，返回最终结果
                        finalResponse.append(finalMsg);
                        latch.countDown();
                        return;
                    }

                    // 调用工具
                    StringBuffer toolResultPrompt = new StringBuffer();
                    try {
                        List<ToolCallRequest> toolCalls = toolProcesser.parseToolRequest(owner, finalMsg);
                        if (toolCalls == null || toolCalls.isEmpty()) {
                            // 没有工具调用，也返回最终结果
                            finalResponse.append(finalMsg);
                            latch.countDown();
                            return;
                        }

                        for (ToolCallRequest toolCall : toolCalls) {
                            String toolResult = null;
                            if (ToolCallRequest.SUMMARY_TOOL.equals(toolCall.getName())) {
                                needSummary.set(true);
                                toolResult = "Yes";
                            } else {
                                // 调用MCP服务
                                toolResult = toolProcesser.callTool(toolCall);
                            }
                            // 构建工具调用结果提示
                            toolResultPrompt.append(toolProcesser.buildResultPrompt(toolCall, toolResult));
                        }
                    } catch (Exception e) {
                        log.error("ToolProcesser error: {}", e.getMessage(), e);
                        // 工具调用异常也反馈给模型
                        toolResultPrompt.append(e.getMessage());
                    }

                    // 保存工具调用结果
                    Message toolResultMessage = new Message("user", toolResultPrompt.toString());
                    saveChatRecord(owner, sessionId, title, toolResultMessage);
                    chatMessages.add(new UserMessage(toolResultPrompt.toString()));
                    // 添加工具响应消息
                    callback.apply(toolResultMessage);
                    // 将MCP结果提交给大模型继续处理
                    latch.countDown();
                    return;
                }

                @SuppressWarnings({ "unchecked", "rawtypes" })
                private String convertToolCall(List<ToolExecutionRequest> toolExecutionRequests) {
                    if (null == toolExecutionRequests || toolExecutionRequests.isEmpty()) {
                        return "<tool_call>[]</tool_call>";
                    }
                    List<ToolCallRequest> callRequests = new ArrayList<>();
                    for (ToolExecutionRequest toolExecutionRequest : toolExecutionRequests) {
                        String name = toolExecutionRequest.name();
                        String args = toolExecutionRequest.arguments();
                        Map argsMap = com.alibaba.fastjson2.JSON.parseObject(args, Map.class);
                        ToolCallRequest callReq = new ToolCallRequest(name, argsMap);
                        callRequests.add(callReq);
                    }
                    return String.format("<tool_call>%s</tool_call>", JSON.toJSONString(callRequests));
                }

                @Override
                public void onError(Throwable error) {
                    failCallback.apply(new Message("assistant", "模型请求失败:" + error.getMessage()));
                    latch.countDown();
                }
            });
            // 使用过一次summary后切换成false
            needSummary.set(false);
            try {
                latch.await();
            } catch (InterruptedException e) {
                log.error("[Agent] 模型请求中断, sessionId={}", sessionId);
            }
        }

        // 处理最终结果
        if (finalResponse.isEmpty()) {
            failCallback.apply(new Message("assistant", "达到单次迭代上限，是否继续？"));
        }
    }

    /**
     * 保存聊天记录
     * 
     * @param owner     账户所有者
     * @param sessionId 会话ID
     * @param title     会话标题
     * @param message   消息
     */
    private void saveChatRecord(String owner, String sessionId, String title, Message message) {
        OwnerChatRecord record = new OwnerChatRecord(
                owner,
                sessionId,
                title,
                message.getRole(),
                message.getContent(),
                message.getReasoningContent());
        record.setMessageId(message.getMessageId());
        assistantService.addChatRecord(owner, sessionId, record);
    }

    /**
     * 初始化MCP服务
     * 
     * @param ownerAccount 账户信息
     */
    private void initMcpServer(OwnerAccount ownerAccount) {
        log.info("[mcp] initMcpServer, owner={}", ownerAccount.getOwner());
        String mcpSettings = ownerAccount.getExtValue(AccountExt.AI_MCP_SETTINGS, "");
        if (StringUtils.isNotBlank(mcpSettings)) {
            log.info("[mcp] initMcpServer, owner={} mcpSettings={}", ownerAccount.getOwner(), mcpSettings);
            McpUtils.initMcpClient(ownerAccount.getOwner(), mcpSettings);
        }
        // 默认配置初始化
        Map<String, Object> params = new HashMap<>();
        mcpSettings = TemplateRenderer.render("config_default_mcp_settings.ftl", params);
        McpUtils.initMcpClient(ownerAccount.getOwner(), mcpSettings);
    }

    /**
     * 构建系统Prompt
     * 
     * @param account   账户
     * @param ownerCode 账户编码
     * @return 系统Prompt
     */
    private String buildSystemPrompt(OwnerAccount account, String ownerCode) {
        Map<String, Object> data = new HashMap<>();
        data.put("ownerCode", ownerCode);
        data.put("time", DateUtils.currentTime());

        String owner = account.getOwner();
        // 获取所有MCP服务
        Map<String, McpSyncClient> ownerMcpClient = McpUtils.getOwnerMcpClient(owner);
        if (null != ownerMcpClient) {
            List<Map<String, Object>> servers = new ArrayList<>();
            for (Map.Entry<String, McpSyncClient> server : ownerMcpClient.entrySet()) {
                String serverName = server.getKey();
                McpSyncClient mcpClient = server.getValue();
                ListToolsResult listTools = mcpClient.listTools();
                List<Tool> tools = listTools.tools();
                List<Map<String, Object>> toolList = new ArrayList<>();
                for (Tool tool : tools) {
                    Map<String, Object> toolInfo = new HashMap<>();
                    toolInfo.put("name", tool.name());
                    toolInfo.put("description", tool.description());
                    toolInfo.put("inputSchema", JSON.toJSONString(tool.inputSchema()));
                    toolList.add(toolInfo);
                }
                Map<String, Object> serverInfo = new HashMap<>();
                serverInfo.put("name", serverName);
                serverInfo.put("tools", toolList);
                servers.add(serverInfo);
            }
            data.put("servers", servers);
        }

        // 是否使用系统策略
        data.put("useSystemStrategies", AccountExtUtils.getUseSystemStrategies(account));

        List<OwnerKnowledge> knowledges = knowledgeManager.listKnowledges(owner);
        if (null != knowledges && !knowledges.isEmpty()) {
            knowledges.forEach(e -> {
                e.setContent(EscapeUtils.escapeXml(e.getContent()));
            });
            // 扩展策略
            List<OwnerKnowledge> strategies = knowledges.stream()
                    .filter(e -> OwnerKnowledge.KnowledgeStatus.ENABLED.getCode().equals(e.getStatus()))
                    .filter(e -> OwnerKnowledge.KnowledgeType.OPTIONS_STRATEGY.getCode().equals(e.getType()))
                    .toList();
            if (null != strategies && !strategies.isEmpty()) {
                data.put("strategies", strategies);
            }
            // 用户自定义规则
            List<OwnerKnowledge> rules = knowledges.stream()
                    .filter(e -> OwnerKnowledge.KnowledgeStatus.ENABLED.getCode().equals(e.getStatus()))
                    .filter(e -> OwnerKnowledge.KnowledgeType.RULES.getCode().equals(e.getType()))
                    .toList();
            if (null != rules && !rules.isEmpty()) {
                data.put("rules", rules);
            }
        }

        // 渲染模板
        return TemplateRenderer.render("agent_system_prompt.ftl", data);
    }

    /**
     * 构建继续对话的Prompt
     * 
     * @param owner     账户
     * @param ownerCode 账户编码
     * @param content   内容
     * @return 继续对话的Prompt
     */
    private String buildContinuePrompt(String owner, String ownerCode, String content) {
        Map<String, Object> data = new HashMap<>();
        data.put("task", content);
        data.put("ownerCode", ownerCode);
        data.put("time", DateUtils.currentTime());
        // 渲染模板
        return TemplateRenderer.render("agent_continue_prompt.ftl", data);
    }

    /**
     * 寻找匹配的工具处理器
     * 
     * @param content 内容
     * @return 工具处理器
     */
    private ToolProcesser findToolProcesser(String content) {
        for (ToolProcesser toolProcesser : toolProcessers) {
            if (toolProcesser.support(content)) {
                return toolProcesser;
            }
        }
        return null;
    }
}