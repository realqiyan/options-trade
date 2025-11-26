package me.dingtou.options.graph.fatory;

import org.springframework.ai.chat.model.ChatModel;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

/**
 * 图工厂
 */
public interface GraphFactory {

        /**
         * 构建执行图
         * 
         * @param chatModel 模型
         * @return 执行图
         * @throws GraphStateException 图异常
         */
        CompiledGraph buildGraph(ChatModel chatModel) throws GraphStateException;
}
