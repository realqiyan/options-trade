package me.dingtou.options.gateway.futu.executor.func;

import com.futu.openapi.pb.QotCommon;
import com.google.protobuf.GeneratedMessageV3;
import me.dingtou.options.gateway.futu.executor.QueryExecutor;
import me.dingtou.options.model.Security;

import java.util.ArrayList;
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
     * 需要订阅的类型
     *
     * @return 订阅类型
     */
    default List<Integer> getSubTypeList() {
        List<Integer> subTypeList = new ArrayList<>();
        subTypeList.add(QotCommon.SubType.SubType_Basic_VALUE);
        return subTypeList;
    }

    /**
     * 是否继续调用
     *
     * @return 是否继续调用
     */
    default boolean isContinue() {
        return true;
    }

    /**
     * 调用
     *
     * @param client 查询扩展
     * @return 请求序号
     */
    int call(QueryExecutor client);

    /**
     * 结果处理
     *
     * @param response 结果
     * @return 结果
     */
    R result(GeneratedMessageV3 response);
}
