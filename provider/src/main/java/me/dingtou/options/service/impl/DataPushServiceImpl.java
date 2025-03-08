package me.dingtou.options.service.impl;


import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.constant.PushDataType;
import me.dingtou.options.manager.OwnerManager;
import me.dingtou.options.manager.PushDataManager;
import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.model.PushData;
import me.dingtou.options.service.DataPushService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Function;

@Slf4j
@Service
public class DataPushServiceImpl implements DataPushService, InitializingBean {

    /**
     * PushDataType, Map<String, Function<PushData, Void>>
     */
    private final static Map<PushDataType, Map<String, Function<PushData, Void>>> DATA_PUSH_MAP = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);


    @Autowired
    private OwnerManager ownerManager;

    @Autowired
    private PushDataManager pushDataManager;


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
    public void afterPropertiesSet() throws Exception {
        //初始化推送数据
        initYncTimePush();
        initStrategyStockPricePush();
    }

    public void initStrategyStockPricePush() {
        try {
            List<OwnerAccount> ownerAccounts = ownerManager.queryAllOwnerAccount();
            if (ownerAccounts == null || ownerAccounts.isEmpty()) {
                return;
            }
            ownerAccounts.forEach(account -> {
                        pushDataManager.subscribeSecurityPrice(account.getOwner(), (securityQuote) -> {
                            DATA_PUSH_MAP.computeIfAbsent(PushDataType.STOCK_PRICE, k -> new ConcurrentHashMap<>()).forEach((key, callback) -> {
                                PushData pushData = new PushData();
                                pushData.getData().put(PushDataType.STOCK_PRICE.getCode(), securityQuote);
                                callback.apply(pushData);
                            });
                            return null;
                        });
                    }
            );
            log.info("initStrategyStockPricePush success.");
        } catch (Throwable e) {
            log.error("initStrategyStockPricePush error. message:{}", e.getMessage());
        }
    }

    private void initYncTimePush() {
        // 定义日期时间格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        ZoneId zone = ZoneId.of("America/New_York");
        scheduler.scheduleAtFixedRate(() -> DATA_PUSH_MAP.computeIfAbsent(PushDataType.NYC_TIME, k -> new ConcurrentHashMap<>()).forEach((key, callback) -> {
            // 获取纽约时间，并转换成yyyy-MM-dd HH:mm:ss格式的时间字符串
            ZonedDateTime newYorkTime = ZonedDateTime.now(zone);
            String time = newYorkTime.format(formatter);
            PushData pushData = new PushData();
            pushData.getData().put(PushDataType.NYC_TIME.getCode(), time);
            callback.apply(pushData);
        }), 0, 1, TimeUnit.SECONDS);

    }


}
