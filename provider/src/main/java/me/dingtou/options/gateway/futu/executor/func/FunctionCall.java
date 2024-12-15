package me.dingtou.options.gateway.futu.executor.func;


import com.google.protobuf.GeneratedMessageV3;
import me.dingtou.options.gateway.futu.executor.SingleQueryExecutor;

public interface FunctionCall<R> {
    /**
     * 调用
     *
     * @param client ft客户端
     */
    void call(SingleQueryExecutor<R> client);

    /**
     * 结果处理
     *
     * @param resp 结果
     * @return 结果
     */
    R result(GeneratedMessageV3 resp);
}
