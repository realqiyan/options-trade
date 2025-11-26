package me.dingtou.options.service.copilot;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.alibaba.fastjson.JSON;

import me.dingtou.options.graph.common.ContextKeys;
import me.dingtou.options.graph.fatory.GraphFactory;
import me.dingtou.options.manager.OwnerManager;
import me.dingtou.options.model.Message;
import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.service.AuthService;
import me.dingtou.options.util.SpringAiUtils;
import reactor.core.publisher.Flux;

/**
 * Ask模式
 */
@Slf4j
@Component("graphCopilotService")
public class GraphCopilotServiceImpl implements CopilotService {

    private final Map<String, CompiledGraph> copilotGraphMap = new ConcurrentHashMap<>();

    @Autowired
    @Qualifier("copilotGraphFactory")
    private GraphFactory copilotGraphFactory;

    @Autowired
    private AuthService authService;

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
        work(true, account, sessionId, message, callback, failCallback, finalCallback);
        return sessionId;
    }

    private void work(boolean isNew,
            OwnerAccount account,
            String sessionId,
            Message message,
            Function<Message, Void> callback,
            Function<Message, Void> failCallback,
            Function<Message, Void> finalCallback) {

        try {

            // 编码owner
            String ownerCode = authService.encodeOwner(account.getOwner());
            CompiledGraph copilotAgent = getCopilotGraph(account);
            RunnableConfig config = RunnableConfig.builder()
                    .threadId(sessionId)
                    .addMetadata("__callback__", callback)
                    .addMetadata("__failCallback__", failCallback)
                    .build();

            Flux<NodeOutput> stream;
            Map<String, Object> inputs = Map.of(ContextKeys.INPUT, message.getContent(),
                    ContextKeys.OWNER_CODE, ownerCode);
            if (isNew) {
                stream = copilotAgent.stream(inputs, config);
            } else {
                StateSnapshot stateSnapshot = copilotAgent.getState(config);
                OverAllState state = stateSnapshot.state();
                state.updateState(inputs);
                stream = copilotAgent.streamFromInitialNode(state, config);
            }

            stream.subscribe(new Consumer<NodeOutput>() {
                @Override
                public void accept(NodeOutput nodeOutput) {
                    Message message = processMessage(nodeOutput);
                    if (null != message) {
                        callback.apply(message);
                    }
                }
            }, new Consumer<Throwable>() {
                @Override
                public void accept(Throwable throwable) {
                    log.error("[Graph] 会话过程中发生错误, owner={}, sessionId={}", account.getOwner(), sessionId,
                            throwable);
                    failCallback.apply(new Message("assistant", throwable.getMessage()));
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("执行Copilot Agent失败", e);
        }
    }

    /**
     * 获取Copilot Agent
     * 
     * @param account 用户账户
     * @return Copilot Agent
     * @throws GraphStateException 图状态异常
     */
    private CompiledGraph getCopilotGraph(OwnerAccount account) throws GraphStateException {
        return copilotGraphMap.computeIfAbsent(account.getOwner(),
                key -> {
                    try {
                        ChatModel chatModel = SpringAiUtils.buildChatModel(account, false);
                        return copilotGraphFactory.buildGraph(chatModel);
                    } catch (GraphStateException e) {
                        throw new RuntimeException("创建Copilot Agent失败", e);
                    }
                });
    }

    /**
     * 处理节点输出
     * 
     * @param nodeOutput 节点输出
     * @return 处理后的消息
     */
    protected Message processMessage(NodeOutput nodeOutput) {
        String agent = nodeOutput.agent();
        String node = nodeOutput.node();

        Long startTime;
        try {
            startTime = nodeOutput.state().value(ContextKeys.NODE_START_TIME, Long.class)
                    .orElse(System.currentTimeMillis());
        } catch (Exception e) {
            startTime = System.currentTimeMillis();
        }

        String messageId = String.format("%s_%s_%d", agent, node, startTime);
        if (nodeOutput instanceof StreamingOutput streamingOutput) {
            Object originData = streamingOutput.getOriginData();
            if (originData instanceof ChatResponse chatResponse) {
                AssistantMessage output = chatResponse.getResult().getOutput();
                if (output.hasToolCalls()) {
                    StringBuffer toolMsg = new StringBuffer();
                    output.getToolCalls().forEach(tc -> {
                        toolMsg.append(tc.name()).append("(").append(tc.arguments()).append(")").append("\n");
                    });
                    return new Message(messageId, "assistant", toolMsg.toString());
                } else if (null != output.getText()) {
                    return new Message(messageId, "assistant", output.getText());
                } else {
                    log.warn("[Graph] 节点 {}.{} 输出为空", agent, node);
                }
            } else if (null != originData) {
                return new Message(messageId, "assistant", JSON.toJSONString(originData));
            } else {
                log.warn("[Graph] 未知节点 {}.{}", agent, node);
            }

        } else {
            boolean start = nodeOutput.isSTART();
            boolean end = nodeOutput.isEND();
            if (start) {
                return new Message(messageId, "assistant", "节点 " + agent + "." + node + " 开始");
            } else if (end) {
                return new Message(messageId, "assistant", "节点 " + agent + "." + node + " 结束");
            }
        }

        return null;
    }

    @Override
    public void continuing(String owner,
            String sessionId,
            Message message,
            Function<Message, Void> callback,
            Function<Message, Void> failCallback,
            Function<Message, Void> finalCallback) {
        log.info("[Graph] 继续会话, owner={}, sessionId={}", owner, sessionId);

        // 验证账户
        OwnerAccount account = validateAccount(owner, failCallback);
        if (account == null) {
            return;
        }
        work(false, account, sessionId, message, callback, failCallback, finalCallback);

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