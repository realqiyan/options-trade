package me.dingtou.options.service.copilot;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Date;
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

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.PartialThinking;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
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
import me.dingtou.options.util.McpUtils;
import me.dingtou.options.util.TemplateRenderer;

/**
 * Agent模式，langchain4j不支持思考内容输出
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
        String firstMessage = buildSystemPrompt(owner, ownerCode, message.getContent());
        Message agentMessage = new Message("user", firstMessage);

        agentWork(account, title, agentMessage, callback, failCallback, sessionId, new ArrayList<>());

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

        agentWork(account, title, newMessage, callback, failCallback, sessionId, messages);
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
    private void agentWork(OwnerAccount account,
            String title,
            Message newMessage,
            Function<Message, Void> callback,
            Function<Message, Void> failCallback,
            String sessionId,
            List<Message> historyMessages) {

        // 最大循环次数防止无限循环
        int maxIterations = 10;
        int iteration = 0;

        List<ChatMessage> chatMessages = convertMessage(historyMessages);

        String owner = account.getOwner();
        // 保存用户消息
        saveChatRecord(owner, sessionId, title, newMessage);
        chatMessages.add(convertMessage(newMessage));

        StreamingChatModel chatModel = buildChatModel(account, false);
        StreamingChatModel summaryModel = buildChatModel(account, true);
        final StringBuffer finalResponse = new StringBuffer();
        final CountDownLatch[] latch = new CountDownLatch[1];
        // 是否需要切换summary模型
        final AtomicBoolean needSummary = new AtomicBoolean(false);
        while (iteration++ < maxIterations && finalResponse.isEmpty()) {
            final StringBuffer returnMessage = new StringBuffer();
            String messageId = "assistant" + System.currentTimeMillis();
            latch[0] = new CountDownLatch(1);
            // 切换模型
            StreamingChatModel model = needSummary.get() && null != summaryModel ? summaryModel : chatModel;
            model.chat(chatMessages, new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String partialResponse) {
                    returnMessage.append(partialResponse);
                    callback.apply(new Message(messageId, "assistant", partialResponse));
                }

                @Override
                public void onPartialThinking(PartialThinking partialThinking) {
                    Message thinkingMessage = new Message();
                    thinkingMessage.setMessageId(messageId);
                    thinkingMessage.setRole("assistant");
                    thinkingMessage.setReasoningContent(partialThinking.text());
                    callback.apply(thinkingMessage);
                }

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    // 保存模型回复
                    final String finalMsg = returnMessage.toString();
                    Message assistantMessage = new Message("assistant", finalMsg);
                    assistantMessage.setMessageId(messageId);
                    saveChatRecord(owner, sessionId, title, assistantMessage);
                    chatMessages.add(new AiMessage(finalMsg));

                    // 提取工具调用信息
                    ToolProcesser toolProcesser = findToolProcesser(finalMsg);
                    if (null == toolProcesser) {
                        // 没有工具调用，返回最终结果
                        finalResponse.append(finalMsg);
                        latch[0].countDown();
                        return;
                    }
                    List<ToolCallRequest> toolCalls = toolProcesser.parseToolRequest(owner, finalMsg);
                    if (toolCalls == null || toolCalls.isEmpty()) {
                        // 没有工具调用，也返回最终结果
                        finalResponse.append(finalMsg);
                        latch[0].countDown();
                        return;
                    }

                    // 调用工具
                    StringBuffer toolResultPrompt = new StringBuffer();
                    for (ToolCallRequest toolCall : toolCalls) {
                        String toolResult = null;
                        if (toolCall.isSummary()) {
                            needSummary.set(true);
                            toolResult = "Yes";
                        } else {
                            // 调用MCP服务
                            toolResult = toolProcesser.callTool(toolCall);
                        }
                        // 构建工具调用结果提示
                        toolResultPrompt.append(toolProcesser.buildResultPrompt(toolCall, toolResult));
                    }

                    // 保存工具调用结果
                    Message toolResultMessage = new Message("user", toolResultPrompt.toString());
                    saveChatRecord(owner, sessionId, title, toolResultMessage);
                    chatMessages.add(new UserMessage(toolResultPrompt.toString()));
                    // 添加工具响应消息
                    callback.apply(toolResultMessage);
                    // 将MCP结果提交给大模型继续处理
                    latch[0].countDown();
                    return;
                }

                @Override
                public void onError(Throwable error) {
                    failCallback.apply(new Message("assistant", "模型请求失败:" + error.getMessage()));
                    latch[0].countDown();
                }
            });
            // 使用过一次summary后切换成false
            needSummary.set(false);
            try {
                latch[0].await();
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
                "");
        record.setMessageId(message.getMessageId());
        assistantService.addChatRecord(owner, sessionId, record);
    }

    /**
     * 转换Message列表为ChatMessage列表
     * 
     * @param historyMessages 历史消息列表
     * @return ChatMessage列表
     */
    private List<ChatMessage> convertMessage(List<Message> historyMessages) {
        List<ChatMessage> chatMessages = new ArrayList<>();
        if (historyMessages != null) {
            for (Message message : historyMessages) {
                chatMessages.add(convertMessage(message));
            }
        }
        return chatMessages;
    }

    /**
     * 转换Message为ChatMessage
     * 
     * @param message 消息
     * @return ChatMessage
     */
    private ChatMessage convertMessage(Message message) {
        ChatMessage chatMessage = null;
        if ("user".equals(message.getRole())) {
            chatMessage = new UserMessage(message.getContent());
        } else if ("assistant".equals(message.getRole())) {
            chatMessage = new AiMessage(message.getContent());
        } else if ("system".equals(message.getRole())) {
            chatMessage = new SystemMessage(message.getContent());
        }
        return chatMessage;
    }

    /**
     * 构建流式ChatModel
     * 
     * @param account   账户信息
     * @param isSummary 是否是总结模型
     * @return 大模型对象
     */
    private StreamingChatModel buildChatModel(OwnerAccount account, boolean isSummary) {
        String baseUrl = isSummary ? AccountExtUtils.getSummaryBaseUrl(account) : AccountExtUtils.getAiBaseUrl(account);
        String model = isSummary ? AccountExtUtils.getSummaryApiModel(account) : AccountExtUtils.getAiApiModel(account);
        String apiKey = isSummary ? AccountExtUtils.getSummaryApiKey(account) : AccountExtUtils.getAiApiKey(account);
        String temperatureVal = isSummary ? AccountExtUtils.getSummaryApiTemperature(account)
                : AccountExtUtils.getAiApiTemperature(account);
        Double temperature = Double.parseDouble(temperatureVal);

        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(model)
                .customHeaders(Map.of("Content-Type", "application/json;charset=UTF-8"))
                .temperature(temperature)
                .returnThinking(true)
                .logRequests(true)
                .logResponses(true)
                .build();
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
        Date expireDate = new Date(System.currentTimeMillis() + 365 * 24 * 60 * 60 * 1000L);
        params.put("jwt", authService.jwt(ownerAccount.getOwner(), expireDate));
        mcpSettings = TemplateRenderer.render("config_default_mcp_settings.ftl", params);
        McpUtils.initMcpClient(ownerAccount.getOwner(), mcpSettings);
    }

    /**
     * 构建系统Prompt
     * 
     * @param owner     账户
     * @param ownerCode 账户编码
     * @param content   内容
     * @return 系统Prompt
     */
    private String buildSystemPrompt(String owner, String ownerCode, String content) {
        Map<String, Object> data = new HashMap<>();
        data.put("task", content);
        data.put("ownerCode", ownerCode);
        data.put("time", DateUtils.currentTime());

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

        List<OwnerKnowledge> knowledges = knowledgeManager.listKnowledges(owner);
        if (null != knowledges && !knowledges.isEmpty()) {
            knowledges.forEach(e -> {
                e.setContent(EscapeUtils.escapeXml(e.getContent()));
            });
            // 扩展策略
            List<OwnerKnowledge> strategys = knowledges.stream()
                    .filter(e -> OwnerKnowledge.KnowledgeStatus.ENABLED.getCode().equals(e.getStatus()))
                    .filter(e -> OwnerKnowledge.KnowledgeType.OPTIONS_STRATEGY.getCode().equals(e.getType()))
                    .toList();
            if (null != strategys && !strategys.isEmpty()) {
                data.put("strategys", strategys);
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