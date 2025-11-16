package me.dingtou.options.graph.func;

import java.util.List;

import org.springframework.ai.chat.messages.Message;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;

/**
 * 输入转换函数
 */
public interface InputConvertFunction {

    /**
     * 输入转换函数
     * @param state  状态
     * @param config 配置
     * @return 消息列表
     */
    List<Message> apply(OverAllState state,RunnableConfig config);

}
