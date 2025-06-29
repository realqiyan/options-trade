package me.dingtou.options.service.copilot;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import me.dingtou.options.manager.OwnerManager;
import me.dingtou.options.model.Message;
import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.model.OwnerChatRecord;
import me.dingtou.options.service.AssistantService;
import me.dingtou.options.util.AccountExtUtils;
import me.dingtou.options.util.ChatClient;
import me.dingtou.options.util.TemplateRenderer;

@Slf4j
@Component
public class AgentCopilotServiceImpl implements CopilotService, InitializingBean {

    @Autowired
    private AssistantService assistantService;

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

        OwnerAccount ownerAccount = ownerManager.queryOwnerAccount(owner);
        if (ownerAccount == null) {
            failCallback.apply(new Message(null, sessionId, "assistant", "账号不存在", null));
            return sessionId;
        }

        // 构建包含MCP工具描述的系统提示词
        String systemPrompt = buildPrompt(message.getContent());
        List<Message> messages = new ArrayList<>();
        Message agentMessage = new Message(sessionId, "user", systemPrompt);
        messages.add(agentMessage);

        // 保存用户消息
        OwnerChatRecord userRecord = new OwnerChatRecord(owner,
                sessionId,
                String.valueOf(System.currentTimeMillis()),
                title,
                agentMessage.getRole(),
                agentMessage.getContent(),
                null);
        assistantService.addChatRecord(owner, sessionId, userRecord);

        // 最大循环次数防止无限循环
        int maxIterations = 10;
        int iteration = 0;
        String finalResponse = null;

        while (iteration++ < maxIterations && finalResponse == null) {
            // 3. 请求大模型
            ChatClient.ChatResponse chatResponse = sendChatRequest(ownerAccount, messages, callback);

            if (chatResponse == null || chatResponse.getContent() == null) {
                failCallback.apply(new Message(null, sessionId, "assistant", "模型请求失败", null));
                break;
            }

            // 4. 解析回复中是否包含MCP工具调用
            if (isToolCall(chatResponse.getContent())) {
                // 提取工具调用信息
                JSONObject toolCall = parseToolCall(chatResponse.getContent());
                if (toolCall != null) {
                    // 调用MCP服务
                    String toolResult = callMcpTool(toolCall.getString("tool"), toolCall.getJSONObject("arguments"));

                    // 保存模型回复（含工具调用）
                    OwnerChatRecord assistantRecord = new OwnerChatRecord(
                            owner,
                            sessionId,
                            chatResponse.getId(),
                            title,
                            "assistant",
                            chatResponse.getContent(),
                            chatResponse.getReasoningContent());
                    assistantService.addChatRecord(owner, sessionId, assistantRecord);

                    // 添加工具响应消息
                    Message toolMessage = new Message(
                            null,
                            sessionId,
                            "tool",
                            toolResult,
                            null);
                    messages.add(toolMessage);

                    // 5. 将MCP结果提交给大模型继续处理
                    continue;
                }
            }

            // 没有工具调用，返回最终结果
            finalResponse = chatResponse.getContent();

            // 保存最终回复
            OwnerChatRecord assistantRecord = new OwnerChatRecord(
                    owner,
                    sessionId,
                    chatResponse.getId(),
                    title,
                    "assistant",
                    finalResponse,
                    chatResponse.getReasoningContent());
            assistantService.addChatRecord(owner, sessionId, assistantRecord);
        }

        if (finalResponse == null) {
            failCallback.apply(new Message(null, sessionId, "assistant", "问题未解决", null));
        } else {
            callback.apply(new Message(null, sessionId, "assistant", finalResponse, null));
        }

        return sessionId;
    }

    private String buildPrompt(String content) {
        Map<String, Object> data = new HashMap<>();
        data.put("task", content);
        data.put("time", new Date().toString());
        // 渲染模板
        return TemplateRenderer.render("agent_system_prompt.ftl", data);
    }

    /**
     * 发送聊天请求（流式）
     */
    private ChatClient.ChatResponse sendChatRequest(OwnerAccount account,
            List<Message> messages,
            Function<Message, Void> callback) {
        String baseUrl = AccountExtUtils.getAiBaseUrl(account);
        String apiKey = AccountExtUtils.getAiApiKey(account);
        String model = AccountExtUtils.getAiApiModel(account);
        double temperature = Double.parseDouble(AccountExtUtils.getAiApiTemperature(account));

        try {
            CompletableFuture<ChatClient.ChatResponse> future = ChatClient.sendStreamChatRequest(
                    baseUrl, apiKey, model, temperature, messages,
                    new Consumer<ChatClient.ChatResponse>() {
                        @Override
                        public void accept(ChatClient.ChatResponse chunk) {
                            if (chunk.isChunk()) {
                                Message msg = new Message(
                                        chunk.getId(),
                                        messages.get(0).getSessionId(),
                                        "assistant",
                                        chunk.getContent(),
                                        chunk.getReasoningContent());
                                callback.apply(msg);
                            }
                        }
                    });
            return future.get(300, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("sendChatRequest: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 检查是否为工具调用
     */
    private boolean isToolCall(String content) {
        return content != null && content.trim().startsWith("{") && content.contains("\"tool\"");
    }

    /**
     * 解析工具调用
     */
    private JSONObject parseToolCall(String content) {
        try {
            return JSON.parseObject(content);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 调用MCP工具
     */
    private String callMcpTool(String toolName, JSONObject arguments) {
        // 实际调用MCP服务的逻辑
        // 使用OkHttpClient发送HTTP请求到MCP服务端点
        // 示例URL: http://mcp-server/tools/{toolName}
        // 请求体: arguments

        // 这里返回模拟响应
        return "工具调用成功: " + toolName + " 参数: " + arguments;
    }

    // 删除冲突的ChatResponse内部类

    @Override
    public void continuing(String owner,
            String sessionId,
            Message message,
            Function<Message, Void> callback,
            Function<Message, Void> failCallback) {

        // 任务完成后用户继续补充提问

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

        // 添加新消息
        Message newMessage = new Message(null, sessionId, "user", message.getContent(), null);
        messages.add(newMessage);

        // 获取会话标题
        // String title = records.get(0).getTitle();

    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // HttpClientSseClientTransport transport =
        // HttpClientSseClientTransport.builder("http:/127.0.0.1:8888").build();
        // McpSyncClient client = McpClient.sync(transport)
        // .requestTimeout(Duration.ofSeconds(10))
        // .capabilities(ClientCapabilities.builder()
        // .roots(true) // Enable roots capability
        // .build())
        // .build();
        // ListToolsResult listTools = client.listTools();
        // System.out.println(listTools);

    }

}
