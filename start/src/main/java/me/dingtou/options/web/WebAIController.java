package me.dingtou.options.web;

import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.service.AIChatService;
import me.dingtou.options.web.model.WebResult;
import me.dingtou.options.web.util.SessionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * API 控制器
 *
 * @author qiyan
 */
@Slf4j
@RestController
public class WebAIController {

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
     * @param requestId requestId
     * @param message   message
     * @return WebResult
     */
    @PostMapping("/ai/chat")
    public WebResult<Boolean> chat(@RequestParam(value = "requestId", required = true) String requestId,
                                   @RequestParam(value = "message", required = true) String message) {
        String owner = SessionUtils.getCurrentOwner();
        SseEmitter connect = SessionUtils.getConnect(owner, requestId);
        aiChatService.chat(message, msg -> {
            try {
                connect.send(msg);
            } catch (IOException e) {
                log.error("send message error", e);
            }
            return null;
        });
        connect.complete();
        return WebResult.success(true);
    }

}
