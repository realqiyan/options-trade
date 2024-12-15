package me.dingtou.options.gateway.futu.executor.func;


import com.google.protobuf.GeneratedMessageV3;
import me.dingtou.options.gateway.futu.executor.QueryExecutor;
import me.dingtou.options.model.Security;

import java.util.Collections;
import java.util.List;

public interface QueryFunctionCall<R> {

    /**
     * 需要订阅的目标证券
     *
     * @return 目标证券
     */
    default List<Security> getSubSecurityList() {
        return Collections.emptyList();
    }

    /**
     * 调用
     *
     * @param client ft客户端
     */
    void call(QueryExecutor<R> client);

    /**
     * 结果处理
     *
     * @param response 结果
     * @return 结果
     */
    R result(GeneratedMessageV3 response);
}
