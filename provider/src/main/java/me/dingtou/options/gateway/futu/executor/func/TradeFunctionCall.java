package me.dingtou.options.gateway.futu.executor.func;


import com.google.protobuf.GeneratedMessageV3;
import me.dingtou.options.gateway.futu.executor.TradeExecutor;

public interface TradeFunctionCall<R> {

    /**
     * 是否需要解锁
     *
     * @return 是否需要解锁
     */
    default boolean needUnlock() {
        return true;
    }

    /**
     * 调用
     *
     * @param client ft客户端
     */
    void call(TradeExecutor<R> client);

    /**
     * 结果处理
     *
     * @param response 结果
     * @return 结果
     */
    R result(GeneratedMessageV3 response);
}
