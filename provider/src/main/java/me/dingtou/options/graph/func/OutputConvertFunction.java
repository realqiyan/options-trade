package me.dingtou.options.graph.func;

import java.util.Map;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;

/**
 * 输入转换函数
 */
public interface OutputConvertFunction {

    /**
     * 输入转换函数
     * 
     * @param state  状态
     * @param config 配置
     * @return 消息列表
     */
    Map<String, Object> apply(OverAllState state, RunnableConfig config, String result);

}
