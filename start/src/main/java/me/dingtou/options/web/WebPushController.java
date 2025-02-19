package me.dingtou.options.web;


import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.constant.PushDataType;
import me.dingtou.options.service.DataPushService;
import me.dingtou.options.web.util.SessionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
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
public class WebPushController {

    @Autowired
    private DataPushService dataPushService;

    /**
     * 建立连接
     *
     * @param requestId requestId
     * @return SseEmitter
     */
    @GetMapping("/connect")
    public SseEmitter connect(String requestId) {
        String owner = SessionUtils.getCurrentOwner();
        SseEmitter connect = SessionUtils.connect(owner, requestId);

        // 默认订阅时间
        dataPushService.subscribe(requestId, PushDataType.NYC_TIME, data -> {
            try {
                connect.send(data);
            } catch (IOException e) {
                log.error("sse send error", e);
                SessionUtils.close(owner, requestId);
                dataPushService.unsubscribe(requestId, PushDataType.NYC_TIME);
            }
            return null;
        });

        return connect;
    }


    /**
     * 关闭连接
     *
     * @param requestId requestId
     * @return true/false
     */
    @GetMapping("/close")
    public Boolean close(String requestId) {
        String owner = SessionUtils.getCurrentOwner();
        SessionUtils.close(owner, requestId);
        return Boolean.TRUE;
    }

}
