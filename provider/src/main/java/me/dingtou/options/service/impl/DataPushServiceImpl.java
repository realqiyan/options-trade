package me.dingtou.options.service.impl;


import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.constant.PushDataType;
import me.dingtou.options.model.PushData;
import me.dingtou.options.service.DataPushService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;


import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

@Slf4j
@Service
public class DataPushServiceImpl implements DataPushService, InitializingBean {

    private final static Map<PushDataType, Map<String, Function<PushData, Void>>> DATA_PUSH_MAP = new ConcurrentHashMap<>();

    private final ExecutorService timerExecutor = Executors.newSingleThreadExecutor();


    @Override
    public void subscribe(String requestId, PushDataType dataType, Function<PushData, Void> callback) {
        Map<String, Function<PushData, Void>> callbackMap = DATA_PUSH_MAP.computeIfAbsent(dataType, k -> new ConcurrentHashMap<>());
        callbackMap.put(requestId, callback);
    }

    @Override
    public void unsubscribe(String requestId, PushDataType dataType) {
        DATA_PUSH_MAP.computeIfAbsent(dataType, k -> new ConcurrentHashMap<>()).remove(requestId);
    }

    @Override
    public void unsubscribe(String requestId) {
        DATA_PUSH_MAP.values().forEach(callbackMap -> callbackMap.remove(requestId));
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        //初始化推送数据
        initYncTimePush();
    }

    private void initYncTimePush() {
        timerExecutor.submit(new Runnable() {
            @Override
            public void run() {
                // 定义日期时间格式
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                ZoneId zone = ZoneId.of("America/New_York");
                while (true) {
                    try {
                        DATA_PUSH_MAP.computeIfAbsent(PushDataType.NYC_TIME, k -> new ConcurrentHashMap<>()).forEach((key, callback) -> {
                            // 获取纽约时间，并转换成yyyy-MM-dd HH:mm:ss格式的时间字符串
                            // 获取纽约时间的ZonedDateTime对象
                            ZonedDateTime newYorkTime = ZonedDateTime.now(zone);
                            // 将ZonedDateTime对象格式化为字符串
                            String time = newYorkTime.format(formatter);
                            PushData pushData = new PushData();
                            pushData.getData().put(PushDataType.NYC_TIME.getCode(), time);
                            callback.apply(pushData);
                        });
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        log.error("push NYC_TIME error", e);
                    }
                }
            }
        });
    }


}
