package me.dingtou.options.service;

import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.constant.PushDataType;
import me.dingtou.options.event.AppEvent;
import me.dingtou.options.event.EventPublisher;
import me.dingtou.options.manager.OwnerManager;
import me.dingtou.options.manager.PushDataManager;
import me.dingtou.options.model.Owner;
import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.model.PushData;
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

    @Autowired
    private EventPublisher eventPublisher;

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
        Map<String, Function<PushData, Void>> callbackMap = DATA_PUSH_MAP.computeIfAbsent(dataType,
                k -> new ConcurrentHashMap<>());
        callbackMap.put(requestId, callback);
    }

    @Override
    public void unsubscribe(String requestId, PushDataType dataType) {
        DATA_PUSH_MAP.computeIfAbsent(dataType, k -> new ConcurrentHashMap<>()).remove(requestId);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // 初始化推送数据
        // 纽约时间 每秒推送一次
        initYncTimePush();
        // 策略股票价格
        initStockPricePush();
        // 订单推送
        initOrderPush();
    }

    /**
     * 初始化订单推送
     */
    public void initOrderPush() {
        try {
            List<Owner> allOwner = ownerManager.queryAllOwner();
            if (allOwner == null || allOwner.isEmpty()) {
                return;
            }

            pushDataManager.subscribeOrderPush(allOwner, (ownerOrder) -> {
                eventPublisher.publishEvent(new AppEvent(PushDataType.ORDER_PUSH, ownerOrder));
                DATA_PUSH_MAP.computeIfAbsent(PushDataType.ORDER_PUSH, k -> new ConcurrentHashMap<>())
                        .forEach((key, callback) -> {
                            PushData pushData = new PushData();
                            pushData.getData().put(PushDataType.ORDER_PUSH.getCode(), ownerOrder);
                            callback.apply(pushData);
                        });
                return null;
            });

            log.info("initOrderPush success.");
        } catch (Throwable e) {
            log.error("initOrderPush error. message:{}", e.getMessage());
        }
    }

    /**
     * 初始化策略股票价格推送
     */
    public void initStockPricePush() {
        try {
            List<OwnerAccount> ownerAccounts = ownerManager.queryAllOwnerAccount();
            if (ownerAccounts == null || ownerAccounts.isEmpty()) {
                return;
            }
            ownerAccounts.forEach(account -> {
                pushDataManager.subscribeSecurityPrice(account.getOwner(), (securityQuote) -> {
                    eventPublisher.publishEvent(new AppEvent(PushDataType.STOCK_PRICE, securityQuote));
                    DATA_PUSH_MAP.computeIfAbsent(PushDataType.STOCK_PRICE, k -> new ConcurrentHashMap<>())
                            .forEach((key, callback) -> {
                                PushData pushData = new PushData();
                                pushData.getData().put(PushDataType.STOCK_PRICE.getCode(), securityQuote);
                                callback.apply(pushData);
                            });
                    return null;
                });
            });
            log.info("initStockPricePush success.");
        } catch (Throwable e) {
            log.error("initStockPricePush error. message:{}", e.getMessage());
        }
    }

    /**
     * 初始化纽约时间推送
     */
    private void initYncTimePush() {
        // 定义日期时间格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        ZoneId zone = ZoneId.of("America/New_York");
        scheduler.scheduleAtFixedRate(() -> DATA_PUSH_MAP
                .computeIfAbsent(PushDataType.NYC_TIME, k -> new ConcurrentHashMap<>()).forEach((key, callback) -> {
                    // 获取纽约时间，并转换成yyyy-MM-dd HH:mm:ss格式的时间字符串
                    ZonedDateTime newYorkTime = ZonedDateTime.now(zone);
                    String time = newYorkTime.format(formatter);
                    PushData pushData = new PushData();
                    pushData.getData().put(PushDataType.NYC_TIME.getCode(), time);
                    callback.apply(pushData);
                }), 0, 1, TimeUnit.SECONDS);

    }

}
