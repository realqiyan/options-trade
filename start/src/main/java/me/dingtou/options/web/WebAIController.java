package me.dingtou.options.web;

import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.model.Message;
import me.dingtou.options.model.OwnerChatRecord;
import me.dingtou.options.service.AIChatService;
import me.dingtou.options.web.model.WebResult;
import me.dingtou.options.web.util.SessionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

    @Autowired
    private AIChatService aiChatService;

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
     * @return WebResult
     */
    @PostMapping("/ai/chat")
    public WebResult<String> chat(@RequestParam(value = "requestId", required = true) String requestId,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "message", required = true) String message) {
        String owner = SessionUtils.getCurrentOwner();
        String sessionId = UUID.randomUUID().toString();
        // 使用线程池提交
        SseEmitter connect = SessionUtils.getConnect(owner, requestId);
        Message chatMessage = new Message(null, "user", message, null);
        try {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    List<Message> messages = new ArrayList<>();
                    messages.add(chatMessage);
                    aiChatService.chat(owner, sessionId, title, messages, msg -> {
                        try {
                            connect.send(msg);
                        } catch (IOException e) {
                            log.error("send message error", e);
                        }
                        return null;
                    });
                    connect.complete();
                }
            });
        } catch (Exception e) {
            log.error("chat error, requestId: {}, message: {}", requestId, message, e);
        }

        return WebResult.success(sessionId);
    }

    /**
     * 继续对话
     *
     * @param requestId 请求ID
     * @param sessionId 会话ID
     * @param message   新消息
     * @return WebResult
     */
    @PostMapping("/ai/chat/continue")
    public WebResult<String> continueChat(@RequestParam(value = "requestId", required = true) String requestId,
            @RequestParam(value = "sessionId", required = true) String sessionId,
            @RequestParam(value = "message", required = true) String message) {
        String owner = SessionUtils.getCurrentOwner();

        // 使用线程池提交
        SseEmitter connect = SessionUtils.getConnect(owner, requestId);
        try {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    // 获取历史消息
                    List<OwnerChatRecord> records = aiChatService.listRecordsBySessionId(owner, sessionId);
                    if (records == null || records.isEmpty()) {
                        try {
                            connect.send(new Message(null, "assistant", "无法找到历史对话记录", null));
                        } catch (IOException e) {
                            log.error("send error message error", e);
                        }
                        return;
                    }

                    // 转换历史消息为Message对象
                    List<Message> messages = new ArrayList<>();
                    for (OwnerChatRecord record : records) {
                        Message chatMessage = new Message(record.getMessageId(),
                                record.getRole(),
                                record.getContent(),
                                record.getReasoningContent());
                        messages.add(chatMessage);
                    }

                    // 添加新消息
                    Message newMessage = new Message(null, "user", message, null);
                    messages.add(newMessage);

                    // 获取会话标题
                    String title = records.get(0).getTitle();

                    // 发送消息
                    aiChatService.chat(owner, sessionId, title, messages, msg -> {
                        try {
                            connect.send(msg);
                        } catch (IOException e) {
                            log.error("send message error", e);
                        }
                        return null;
                    });
                }
            });
        } catch (Exception e) {
            log.error("continue chat error, requestId: {}, sessionId: {}, message: {}", requestId, sessionId, message,
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
            return WebResult.success(aiChatService.summaryChatRecord(owner, 200));
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
            List<OwnerChatRecord> records = aiChatService.listRecordsBySessionId(owner, sessionId);
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
            boolean result = aiChatService.deleteBySessionId(owner, sessionId);
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
            boolean result = aiChatService.updateSessionTitle(owner, sessionId, title);
            return WebResult.success(result);
        } catch (Exception e) {
            log.error("更新会话标题失败", e);
            return WebResult.failure("更新会话标题失败: " + e.getMessage());
        }
    }

    /**
     * 更新AI设置
     *
     * @param systemPrompt 系统提示词
     * @param temperature  温度参数
     * @return WebResult
     */
    @PostMapping("/ai/settings/update")
    public WebResult<Boolean> updateAISettings(@RequestBody Map<String, Object> params) {
        try {
            String owner = SessionUtils.getCurrentOwner();
            String systemPrompt = params.get("systemPrompt") != null
                    ? params.get("systemPrompt").toString()
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

            boolean result = aiChatService.updateSettings(owner, systemPrompt, temperature);
            return WebResult.success(result);
        } catch (Exception e) {
            log.error("更新AI设置失败", e);
            return WebResult.failure("更新AI设置失败: " + e.getMessage());
        }
    }

}
