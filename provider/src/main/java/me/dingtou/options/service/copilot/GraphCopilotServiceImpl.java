package me.dingtou.options.service.copilot;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.alibaba.fastjson.JSON;

import me.dingtou.options.graph.fatory.GraphFactory;
import me.dingtou.options.manager.OwnerManager;
import me.dingtou.options.model.Message;
import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.service.mcp.KnowledgeMcpService;
import me.dingtou.options.util.SpringAiUtils;

/**
 * Ask模式
 */
@Slf4j
@Component("graphCopilotService")
public class GraphCopilotServiceImpl implements CopilotService {

    @Autowired
    private KnowledgeMcpService knowledgeMcpService;

    @Autowired
    private OwnerManager ownerManager;

    @Override
    public String mode() {
        return "graph";
    }

    @Override
    public String start(String owner,
            String sessionId,
            String title,
            Message message,
            Function<Message, Void> callback,
            Function<Message, Void> failCallback,
            Function<Message, Void> finalCallback) {
        log.info("[Graph] 开始新会话, owner={}, sessionId={}, title={}", owner, sessionId, title);

        // 验证账户
        OwnerAccount account = validateAccount(owner, failCallback);
        if (account == null) {
            return sessionId;
        }

        ChatModel chatModel = SpringAiUtils.buildChatModel(account, false);
        try {
            StateGraph copilotAgent = GraphFactory.copilotAgent(chatModel, knowledgeMcpService);
            RunnableConfig config = RunnableConfig.builder()
                    .threadId(sessionId)
                    .addMetadata("__callback__", callback)
                    .addMetadata("__failCallback__", failCallback)
                    .build();
            Map<String, Object> inputs = Map.of("input", message.getContent());
            copilotAgent.compile()
                    .stream(inputs, config)
                    .subscribe(new Consumer<NodeOutput>() {
                        @Override
                        public void accept(NodeOutput nodeOutput) {
                            if (nodeOutput instanceof StreamingOutput streamingOutput) {
                                Object originData = streamingOutput.getOriginData();
                                callback.apply(new Message("assistant", JSON.toJSONString(originData)));
                            } else {
                                callback.apply(new Message("assistant", JSON.toJSONString(nodeOutput)));
                            }
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) {
                            log.error("[Graph] 会话过程中发生错误, owner={}, sessionId={}", owner, sessionId, throwable);
                            failCallback.apply(new Message("assistant", throwable.getMessage()));
                        }
                    });
        } catch (GraphStateException e) {
            throw new RuntimeException("创建Copilot Agent失败", e);
        }
        return sessionId;
    }

    @Override
    public void continuing(String owner,
            String sessionId,
            Message message,
            Function<Message, Void> callback,
            Function<Message, Void> failCallback,
            Function<Message, Void> finalCallback) {
        log.info("[Graph] 继续会话, owner={}, sessionId={}", owner, sessionId);

        throw new UnsupportedOperationException("暂不支持继续会话");

    }

    /**
     * 验证账户是否存在
     * 
     * @param owner        账户所有者
     * @param failCallback 失败回调
     * @return 账户信息，如果不存在则返回null
     */
    private OwnerAccount validateAccount(String owner, Function<Message, Void> failCallback) {
        OwnerAccount account = ownerManager.queryOwnerAccount(owner);
        if (account == null) {
            log.error("[Graph] 账号不存在, owner={}", owner);
            failCallback.apply(new Message("assistant", "账号不存在"));
            return null;
        }
        return account;
    }

}