package me.dingtou.options.web;

import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.model.Message;
import me.dingtou.options.model.OwnerChatRecord;
import me.dingtou.options.service.AssistantService;
import me.dingtou.options.service.copilot.CopilotService;
import me.dingtou.options.util.EscapeUtils;
import me.dingtou.options.web.model.WebResult;
import me.dingtou.options.web.util.SessionUtils;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;

/**
 * AI 控制器
 *
 * @author qiyan
 */
@Slf4j
@RestController
public class WebAIController {

    private final ExecutorService executorService = Executors.newFixedThreadPool(10,
            new ThreadFactoryBuilder().setNameFormat("ai-chat-%d").build());

    private static final Pattern TASK = Pattern.compile("<task>([\\s\\S]*?)</task>", Pattern.DOTALL);
    private static final Pattern QUERY = Pattern.compile("<query>([\\s\\S]*?)</query>", Pattern.DOTALL);

    /**
     * 助手服务
     */
    private AssistantService assistantService;

    /**
     * copilot服务
     */
    private Map<String, CopilotService> copilotServiceMap = new HashMap<>();

    WebAIController(AssistantService assistantService, List<CopilotService> copilotServices) {
        // 初始化助手服务
        this.assistantService = assistantService;
        // 初始化copilot服务
        copilotServices
                .stream()
                .forEach(copilot -> copilotServiceMap.put(copilot.mode(), copilot));
    }

    /**
     * 建立连接
     *
     * @param requestId requestId
     * @return SseEmitter
     */
    @GetMapping("/ai/init")
    public SseEmitter init(@RequestParam(value = "requestId", required = true) String requestId) {
        String owner = SessionUtils.getCurrentOwner();
        return SessionUtils.connect(owner, requestId);
    }

    /**
     * 聊天
     *
     * @param requestId 请求ID
     * @param message   消息内容
     * @param title     标题（可选，股票+策略）
     * @param mode      模式
     * 
     * @return WebResult
     */
    @PostMapping("/ai/chat")
    public WebResult<String> chat(@RequestParam(value = "requestId", required = true) String requestId,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "message", required = true) String message,
            @RequestParam(value = "mode", required = true) String mode) {

        String userMsg = EscapeUtils.escapeHtml(message);
        String owner = SessionUtils.getCurrentOwner();
        String sessionId = UUID.randomUUID().toString();
        // 使用线程池提交
        SseEmitter connect = SessionUtils.getConnect(owner, requestId);
        Message chatMessage = new Message("user", userMsg);
        final String sessionTitle;
        if (StringUtils.isBlank(title)) {
            sessionTitle = assistantService.generateSessionTitle(owner, userMsg);
        } else {
            sessionTitle = title;
        }
        try {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        copilotServiceMap.get(mode).start(owner, sessionId, sessionTitle, chatMessage, msg -> {
                            try {
                                escapeHtml(msg);
                                connect.send(msg);
                            } catch (IOException e) {
                                log.error("send message error:", e.getMessage());
                            }
                            return null;
                        }, msg -> {
                            try {
                                escapeHtml(msg);
                                connect.send(msg);
                            } catch (IOException e) {
                                log.error("send message error:", e.getMessage());
                            }
                            return null;
                        });
                    } catch (Throwable e) {
                        log.error("Error: ", e);
                    }
                }
            });
        } catch (Exception e) {
            log.error("chat error, requestId: {}, message: {}", requestId, userMsg, e);
        }

        return WebResult.success(sessionId);
    }

    /**
     * 继续对话
     *
     * @param requestId 请求ID
     * @param sessionId 会话ID
     * @param message   新消息
     * @param mode      模式
     * @return WebResult
     */
    @PostMapping("/ai/continue")
    public WebResult<String> continueChat(@RequestParam(value = "requestId", required = true) String requestId,
            @RequestParam(value = "sessionId", required = true) String sessionId,
            @RequestParam(value = "message", required = true) String message,
            @RequestParam(value = "mode", required = true) String mode) {

        String userMsg = EscapeUtils.escapeHtml(message);
        String owner = SessionUtils.getCurrentOwner();

        // 使用线程池提交
        SseEmitter connect = SessionUtils.getConnect(owner, requestId);
        try {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        Message newMessage = new Message("user", userMsg);
                        copilotServiceMap.get(mode).continuing(owner, sessionId, newMessage, msg -> {
                            try {
                                escapeHtml(msg);
                                connect.send(msg);
                            } catch (IOException e) {
                                log.error("send message error:", e.getMessage());
                            }
                            return null;
                        }, msg -> {
                            try {
                                escapeHtml(msg);
                                connect.send(msg);
                            } catch (IOException e) {
                                log.error("send message error:", e.getMessage());
                            }
                            return null;
                        });
                    } catch (Throwable e) {
                        log.error("Error: ", e);
                    }
                }
            });
        } catch (Exception e) {
            log.error("continue chat error, requestId: {}, sessionId: {}, message: {}", requestId, sessionId, userMsg,
                    e);
        }

        return WebResult.success(sessionId);
    }

    /**
     * 获取所有会话ID列表
     *
     * @return 会话ID列表
     */
    @GetMapping("/ai/record/sessions")
    public WebResult<List<OwnerChatRecord>> listSessionIds() {
        try {
            String owner = SessionUtils.getCurrentOwner();
            // 获取最近200条会话ID列表
            return WebResult.success(assistantService.summaryChatRecord(owner, 200));
        } catch (Exception e) {
            log.error("获取会话ID列表失败", e);
            return WebResult.failure("获取会话ID列表失败: " + e.getMessage());
        }
    }

    /**
     * 根据会话ID获取沟通记录
     *
     * @param sessionId 会话ID
     * @return 沟通记录列表
     */
    @GetMapping("/ai/record/list")
    public WebResult<List<OwnerChatRecord>> listRecordsBySessionId(@RequestParam("sessionId") String sessionId) {
        try {
            String owner = SessionUtils.getCurrentOwner();
            List<OwnerChatRecord> records = assistantService.listRecordsBySessionId(owner, sessionId);
            if (records == null || records.isEmpty()) {
                return WebResult.success(Collections.emptyList());
            }

            // 屏蔽系统消息
            records.removeIf(e -> "system".equals(e.getRole()));

            // 如果是agent模式的第一条助手消息，提取task标签内容
            boolean isFirstUserMessage = true;
            for (OwnerChatRecord record : records) {
                // 检查是否是agent模式的第一条助手消息
                if ("user".equals(record.getRole()) && isFirstUserMessage) {
                    isFirstUserMessage = false;
                    Matcher taskMatcher = TASK.matcher(record.getContent());
                    while (taskMatcher.find()) {
                        // 找到最后一个
                        record.setContent(taskMatcher.group(1));
                    }
                    Matcher queryMatcher = QUERY.matcher(record.getContent());
                    while (queryMatcher.find()) {
                        // 找到最后一个
                        record.setContent(queryMatcher.group(1));
                    }
                    escapeHtml(record);
                    continue;
                }
                escapeHtml(record);
            }

            return WebResult.success(records);
        } catch (Exception e) {
            log.error("获取沟通记录失败", e);
            return WebResult.failure("获取沟通记录失败: " + e.getMessage());
        }
    }

    /**
     * 删除会话记录
     *
     * @param sessionId 会话ID
     * @return 是否成功
     */
    @DeleteMapping("/ai/record/delete")
    public WebResult<Boolean> deleteBySessionId(@RequestParam("sessionId") String sessionId) {
        try {
            String owner = SessionUtils.getCurrentOwner();
            boolean result = assistantService.deleteBySessionId(owner, sessionId);
            return WebResult.success(result);
        } catch (Exception e) {
            log.error("删除会话记录失败", e);
            return WebResult.failure("删除会话记录失败: " + e.getMessage());
        }
    }

    /**
     * 更新会话标题
     *
     * @param sessionId 会话ID
     * @param title     标题
     * @return 是否成功
     */
    @PostMapping("/ai/record/update-title")
    public WebResult<Boolean> updateSessionTitle(@RequestParam("sessionId") String sessionId,
            @RequestParam("title") String title) {
        try {
            String owner = SessionUtils.getCurrentOwner();
            boolean result = assistantService.updateSessionTitle(owner, sessionId, title);
            return WebResult.success(result);
        } catch (Exception e) {
            log.error("更新会话标题失败", e);
            return WebResult.failure("更新会话标题失败: " + e.getMessage());
        }
    }

    /**
     * 更新AI设置
     *
     * @param mcpSettings MCP服务器配置
     * @param temperature 温度参数
     * @return WebResult
     */
    /**
     * 获取AI设置
     *
     * @return WebResult
     */
    @GetMapping("/ai/settings")
    public WebResult<Map<String, Object>> getAISettings() {
        try {
            String owner = SessionUtils.getCurrentOwner();
            Map<String, Object> settings = assistantService.getSettings(owner);
            return WebResult.success(settings);
        } catch (Exception e) {
            log.error("获取AI设置失败", e);
            return WebResult.failure("获取AI设置失败: " + e.getMessage());
        }
    }

    @PostMapping("/ai/settings/update")
    public WebResult<Boolean> updateAISettings(@RequestBody Map<String, Object> params) {
        try {
            String owner = SessionUtils.getCurrentOwner();
            String mcpSettings = params.get("mcpSettings") != null
                    ? params.get("mcpSettings").toString()
                    : null;
            Double temperature = params.get("temperature") != null
                    ? Double.parseDouble(params.get("temperature").toString())
                    : null;

            if (temperature == null) {
                return WebResult.failure("temperature参数不能为空");
            }

            // 温度参数范围检查
            if (temperature < 0 || temperature > 1) {
                return WebResult.failure("温度参数必须在0-1之间");
            }

            boolean result = assistantService.updateSettings(owner, mcpSettings, temperature);
            return WebResult.success(result);
        } catch (Exception e) {
            log.error("更新AI设置失败", e);
            return WebResult.failure("更新AI设置失败: " + e.getMessage());
        }
    }

    /**
     * 转义HTML特殊字符
     * 
     * @param message 消息
     */
    private void escapeHtml(Message message) {
        if (null == message) {
            return;
        }
        message.setContent(EscapeUtils.escapeHtml(message.getContent()));
        message.setReasoningContent(EscapeUtils.escapeHtml(message.getReasoningContent()));
    }

}
