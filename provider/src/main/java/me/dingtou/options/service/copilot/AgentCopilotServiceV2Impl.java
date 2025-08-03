package me.dingtou.options.service.copilot;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
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
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import me.dingtou.options.constant.AccountExt;
import me.dingtou.options.manager.OwnerManager;
import me.dingtou.options.model.Message;
import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.model.OwnerChatRecord;
import me.dingtou.options.model.copilot.ToolCallRequest;
import me.dingtou.options.service.AssistantService;
import me.dingtou.options.service.AuthService;
import me.dingtou.options.util.AccountExtUtils;
import me.dingtou.options.util.DateUtils;
import me.dingtou.options.util.McpUtils;
import me.dingtou.options.util.TemplateRenderer;

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

    @Override
    public String mode() {
        return "agent";
    }

    @Override
    public String start(String owner,
            String title,
            Message message,
            Function<Message, Void> callback,
            Function<Message, Void> failCallback) {
        String sessionId = message.getSessionId();
        log.info("[Agent] 开始新会话, owner={}, sessionId={}, title={}", owner, sessionId, title);

        OwnerAccount account = ownerManager.queryOwnerAccount(owner);
        if (account == null) {
            log.error("[Agent] 账号不存在, owner={}", owner);
            failCallback.apply(new Message(null, sessionId, "assistant", "账号不存在", null));
            return sessionId;
        }

        // 编码owner
        String ownerCode = authService.encodeOwner(owner);

        // 初始化MCP服务
        initMcpServer(account);

        // 构建包含MCP工具描述的系统提示词
        String firstMessage = buildPrompt(owner, ownerCode, message.getContent());
        Message agentMessage = new Message(sessionId, "user", firstMessage);

        agentWork(account, title, agentMessage, callback, failCallback, sessionId, new ArrayList<>());

        return sessionId;
    }

    /**
     * agentWork
     * 
     * @param account         account
     * @param title           title
     * @param callback        callback
     * @param failCallback    failCallback
     * @param sessionId       sessionId
     * @param historyMessages historyMessages
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

        final List<Message> messages = new ArrayList<>();
        messages.addAll(historyMessages);

        String owner = account.getOwner();
        // 保存用户消息
        OwnerChatRecord userRecord = new OwnerChatRecord(owner,
                sessionId,
                String.valueOf(System.currentTimeMillis()),
                title,
                newMessage.getRole(),
                newMessage.getContent(),
                null);
        assistantService.addChatRecord(owner, sessionId, userRecord);
        messages.add(newMessage);

        StreamingChatModel chatModel = buildChatModel(account);

        List<ChatMessage> chatMessages = convertMessage(messages);
        final StringBuffer finalResponse = new StringBuffer();
        final CountDownLatch[] latch = new CountDownLatch[1];
        while (iteration++ < maxIterations && finalResponse.isEmpty()) {

            final StringBuffer returnMessage = new StringBuffer();
            String messageId = String.valueOf(System.currentTimeMillis());
            latch[0] = new CountDownLatch(1);
            chatModel.chat(chatMessages, new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String partialResponse) {
                    returnMessage.append(partialResponse);
                    callback.apply(new Message(messageId, sessionId, "assistant", partialResponse, null));
                }

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    // 保存模型回复
                    OwnerChatRecord assistantRecord = new OwnerChatRecord(
                            owner,
                            sessionId,
                            messageId,
                            title,
                            "assistant",
                            returnMessage.toString(),
                            "");
                    assistantService.addChatRecord(owner, sessionId, assistantRecord);

                    // 提取工具调用信息
                    ToolProcesser toolProcesser = findToolProcesser(returnMessage.toString());
                    if (null != toolProcesser) {
                        ToolCallRequest toolCall = toolProcesser.parseToolRequest(owner, returnMessage.toString());
                        if (toolCall != null) {
                            log.info("[Agent] 调用工具, sessionId={}, tool={}, request={}",
                                    sessionId, toolProcesser.getClass().getSimpleName(), toolCall);
                            // 调用MCP服务
                            String toolResult = toolProcesser.callTool(toolCall);
                            log.info("[Agent] 工具返回结果, sessionId={}, resultLength={}", sessionId, toolResult.length());

                            // 构建工具调用结果提示
                            String toolResultPrompt = toolProcesser.buildResultPrompt(toolCall, toolResult);

                            // 添加工具响应消息
                            Message toolMessage = new Message(
                                    String.valueOf(System.currentTimeMillis()),
                                    sessionId,
                                    "user",
                                    toolResultPrompt,
                                    null);

                            // 保存用户消息
                            OwnerChatRecord toolResultRecord = new OwnerChatRecord(owner,
                                    sessionId,
                                    toolMessage.getId(),
                                    title,
                                    toolMessage.getRole(),
                                    toolMessage.getContent(),
                                    null);
                            assistantService.addChatRecord(owner, sessionId, toolResultRecord);
                            messages.add(toolMessage);
                            callback.apply(toolMessage);
                            // 将MCP结果提交给大模型继续处理
                            latch[0].countDown();
                            return;
                        }
                    }

                    // 没有工具调用，返回最终结果
                    finalResponse.append(returnMessage);
                    log.info("[Agent] 生成最终响应, sessionId={}, responseLength={}", sessionId, finalResponse.length());
                    latch[0].countDown();
                }

                @Override
                public void onError(Throwable error) {
                    failCallback.apply(new Message(null, sessionId, "assistant", "模型请求失败:" + error.getMessage(), null));
                    latch[0].countDown();
                }
            });
            try {
                latch[0].await();
            } catch (InterruptedException e) {
                log.error("[Agent] 模型请求中断, sessionId={}", sessionId);
            }
        }

        // 处理最终结果
        if (finalResponse.isEmpty()) {
            failCallback.apply(new Message(null, sessionId, "assistant", "达到单次迭代上限，是否继续？", null));
        }
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
     * @param account 账户信息
     * @return 大模型对象
     */
    private StreamingChatModel buildChatModel(OwnerAccount account) {
        String baseUrl = AccountExtUtils.getAiBaseUrl(account);
        String model = AccountExtUtils.getAiApiModel(account);
        String apiKey = AccountExtUtils.getAiApiKey(account);
        Double temperature = Double.parseDouble(AccountExtUtils.getAiApiTemperature(account));

        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(model)
                .temperature(temperature)
                .logRequests(true)
                .logResponses(true)
                .build();
    }

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

    private String buildPrompt(String owner, String ownerCode, String content) {
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

        // 渲染模板
        return TemplateRenderer.render("agent_system_prompt.ftl", data);
    }

    /**
     * 寻找匹配的工具处理器
     */
    private ToolProcesser findToolProcesser(String content) {
        for (ToolProcesser toolProcesser : toolProcessers) {
            if (toolProcesser.support(content)) {
                return toolProcesser;
            }
        }
        return null;
    }

    @Override
    public void continuing(String owner,
            String sessionId,
            Message message,
            Function<Message, Void> callback,
            Function<Message, Void> failCallback) {
        log.info("[Agent] 继续会话, owner={}, sessionId={}", owner, sessionId);

        OwnerAccount account = ownerManager.queryOwnerAccount(owner);
        if (account == null) {
            failCallback.apply(new Message(null, sessionId, "assistant", "账号不存在", null));
            return;
        }

        // 获取历史消息(user & assistant)
        List<OwnerChatRecord> records = assistantService.listRecordsBySessionId(owner, sessionId);
        if (records == null || records.isEmpty()) {
            failCallback.apply(new Message(null, sessionId, "assistant", "无法找到历史对话记录", null));
            return;
        }

        // 转换历史消息为Message对象
        List<Message> messages = new ArrayList<>();
        for (OwnerChatRecord record : records) {
            Message chatMessage = new Message(record.getMessageId(),
                    record.getSessionId(),
                    record.getRole(),
                    record.getContent(),
                    record.getReasoningContent());
            messages.add(chatMessage);
        }

        // 获取会话标题
        String title = records.get(0).getTitle();

        // 添加新消息
        Message newMessage = new Message(null, sessionId, "user", message.getContent(), null);

        agentWork(account, title, newMessage, callback, failCallback, sessionId, messages);

    }

}
