package me.dingtou.options.gateway.futu.executor.func;

import com.futu.openapi.FTAPI_Conn;
import me.dingtou.options.gateway.futu.executor.ReqContext;

public interface FunctionCall<F extends FTAPI_Conn, R> {
    /**
     * 调用
     *
     * @param client ft客户端
     */
    void call(F client);

    /**
     * 结果处理
     *
     * @param reqContext 请求标识
     * @return 结果
     */
    R result(ReqContext reqContext);
}
