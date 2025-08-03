package me.dingtou.options.service.copilot;

import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LlmAgent;
import com.google.adk.agents.RunConfig;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.adk.tools.mcp.McpTool;
import com.google.adk.tools.mcp.McpToolset;
import com.google.adk.tools.mcp.SseServerParameters;
import com.google.genai.types.Content;
import com.google.genai.types.Part;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import io.reactivex.rxjava3.core.Flowable;
import me.dingtou.options.constant.AccountExt;
import me.dingtou.options.manager.OwnerManager;
import me.dingtou.options.model.Message;
import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.service.AssistantService;
import me.dingtou.options.service.AuthService;
import me.dingtou.options.util.AccountExtUtils;
import me.dingtou.options.util.DateUtils;
import me.dingtou.options.util.McpUtils;
import me.dingtou.options.util.TemplateRenderer;

@Slf4j
@Component
public class AgentCopilotServiceV2Impl implements CopilotService {

    private static final String NAME = "Qian";

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

    private String getMcpSettings(OwnerAccount ownerAccount) {
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
        return mcpSettings;
        // McpUtils.initMcpClient(ownerAccount.getOwner(), mcpSettings);
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
        String mcpSettings = getMcpSettings(account);
        Map<String, SseServerParameters> sseServerParameters = McpUtils.getSseServerParameters(mcpSettings);

        // 构建包含MCP工具描述的系统提示词
        String firstMessage = buildPrompt(owner, ownerCode, message.getContent());

        try {
            String baseUrl = AccountExtUtils.getAiBaseUrl(account);
            String model = AccountExtUtils.getAiApiModel(account);
            String apiKey = AccountExtUtils.getAiApiKey(account);
            Double temperature = Double.parseDouble(AccountExtUtils.getAiApiTemperature(account));

            ChatModel chatModel = OpenAiChatModel.builder()
                    .baseUrl(baseUrl)
                    .apiKey(apiKey)
                    .modelName(model)
                    .temperature(temperature)
                    .logRequests(true)
                    .logResponses(true)
                    .build();

            BaseAgent rootAgent = initAgent(NAME, chatModel, sseServerParameters);
            InMemoryRunner runner = new InMemoryRunner(rootAgent);
            Session session = runner
                    .sessionService()
                    .createSession(NAME, owner)
                    .blockingGet();

            Content userMsg = Content.fromParts(Part.fromText(firstMessage));
            RunConfig runConfig = RunConfig.builder()
                    .setMaxLlmCalls(10)
                    .build();
            Flowable<Event> events = runner.runAsync(owner, session.id(), userMsg, runConfig);
            events.blockingForEach(event -> {
                callback.apply(
                        new Message(event.invocationId(), sessionId, "assistant", event.stringifyContent(), null));
            });
        } catch (Exception e) {
            log.error("[Agent] Agent执行失败,message:" + e.getMessage(), e);
            failCallback.apply(new Message(null, sessionId, "assistant", "Agent执行失败", null));
            return sessionId;
        }

        return sessionId;
    }

    private static BaseAgent initAgent(String name,
            ChatModel chatModel,
            Map<String, SseServerParameters> sseServerParameters)
            throws Exception {
        List<McpTool> tools = sseServerParameters.values().stream()
                .flatMap(e -> {
                    try {
                        return McpToolset.fromServer(e).get().getTools().stream();
                    } catch (Exception exception) {
                        log.error("[Agent] 初始化MCP工具失败,message:" + exception.getMessage(), exception);
                        return Stream.empty();
                    }
                })
                .collect(Collectors.toList());
        return LlmAgent.builder()
                .name(name)
                .tools(tools)
                .model(new com.google.adk.models.langchain4j.LangChain4j(chatModel))
                .build();
    }

    private String buildPrompt(String owner, String ownerCode, String content) {
        Map<String, Object> data = new HashMap<>();
        data.put("task", content);
        data.put("ownerCode", ownerCode);
        data.put("time", DateUtils.currentTime());
        // 渲染模板
        return TemplateRenderer.render("agent_system_prompt.ftl", data);
    }

    @Override
    public void continuing(String owner,
            String sessionId,
            Message message,
            Function<Message, Void> callback,
            Function<Message, Void> failCallback) {

        throw new UnsupportedOperationException("不支持继续会话");

    }

}
