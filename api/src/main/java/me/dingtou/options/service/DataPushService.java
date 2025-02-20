package me.dingtou.options.service;

import me.dingtou.options.constant.PushDataType;
import me.dingtou.options.model.PushData;

import java.util.function.Function;

/**
 * 数据推送服务
 *
 * @author qiyan
 */
public interface DataPushService {

    /**
     * 订阅推送数据
     *
     * @param requestId 请求ID
     * @param dataType  数据类型
     * @param callback  回调
     */
    void subscribe(String requestId, PushDataType dataType, Function<PushData, Void> callback);

    /**
     * 退订推送数据
     *
     * @param requestId 请求ID
     * @param dataType  数据类型
     */
    void unsubscribe(String requestId, PushDataType dataType);

}
