package me.dingtou.options.web;


import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.constant.PushDataType;
import me.dingtou.options.service.DataPushService;
import me.dingtou.options.web.util.SessionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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
        //默认订阅
        for (PushDataType pushDataType : PushDataType.values()) {
            subscribe(requestId, pushDataType.getCode());
        }

        return connect;
    }

    private void subscribe(String requestId, String dataType) {
        PushDataType pushDataType = PushDataType.of(dataType);
        String owner = SessionUtils.getCurrentOwner();
        SseEmitter connect = SessionUtils.getConnect(owner, requestId);
        // 默认订阅时间
        dataPushService.subscribe(requestId, pushDataType, data -> {
            try {
                connect.send(data);
            } catch (Exception e) {
                log.warn("sse send error. requestId:{} message:{}", requestId, e.getMessage());
                dataPushService.unsubscribe(requestId, pushDataType);
                SessionUtils.close(owner, requestId);
            }
            return null;
        });
    }


    /**
     * 取消订阅
     *
     * @param requestId requestId
     * @return true/false
     */
    @GetMapping("/close")
    public Boolean close(String requestId) {
        for (PushDataType pushDataType : PushDataType.values()) {
            dataPushService.unsubscribe(requestId, pushDataType);
        }
        return Boolean.TRUE;
    }


}
