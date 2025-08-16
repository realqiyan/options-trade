package me.dingtou.options.service.copilot;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.PartialThinking;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import me.dingtou.options.manager.OwnerManager;
import me.dingtou.options.model.Message;
import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.model.OwnerChatRecord;
import me.dingtou.options.service.AssistantService;
import me.dingtou.options.util.LlmUtils;

/**
 * Ask模式
 */
@Slf4j
@Component
public class AskCopilotServiceImpl implements CopilotService {

    @Autowired
    private AssistantService assistantService;

    @Autowired
    private OwnerManager ownerManager;

    @Override
    public String mode() {
        return "ask";
    }

    @Override
    public String start(String owner,
            String sessionId,
            String title,
            Message message,
            Function<Message, Void> callback,
            Function<Message, Void> failCallback) {
        log.info("[Ask] 开始新会话, owner={}, sessionId={}, title={}", owner, sessionId, title);

        // 验证账户
        OwnerAccount account = validateAccount(owner, failCallback);
        if (account == null) {
            return sessionId;
        }

        // 默认系统提示词也保存
        Message sysMsg = new Message("system", "You are a helpful assistant.");
        saveChatRecord(owner, sessionId, title, sysMsg);
        work(account, title, message, callback, failCallback, sessionId, List.of(sysMsg));

        return sessionId;
    }

    @Override
    public void continuing(String owner,
            String sessionId,
            Message message,
            Function<Message, Void> callback,
            Function<Message, Void> failCallback) {
        log.info("[Ask] 继续会话, owner={}, sessionId={}", owner, sessionId);

        // 验证账户
        OwnerAccount account = validateAccount(owner, failCallback);
        if (account == null) {
            return;
        }

        // 获取历史消息(user & assistant)
        List<OwnerChatRecord> records = assistantService.listRecordsBySessionId(owner, sessionId);
        if (records == null || records.isEmpty()) {
            failCallback.apply(new Message("assistant", "无法找到历史对话记录"));
            return;
        }

        // 转换历史消息为Message对象
        List<Message> messages = new ArrayList<>(records);

        // 获取会话标题
        String title = records.get(0).getTitle();

        work(account, title, message, callback, failCallback, sessionId, messages);
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
            log.error("[Ask] 账号不存在, owner={}", owner);
            failCallback.apply(new Message("assistant", "账号不存在"));
            return null;
        }
        return account;
    }

    /**
     * Agent Work
     * 
     * @param account         账户信息
     * @param title           会话标题
     * @param newMessage      新消息
     * @param callback        回调函数
     * @param failCallback    失败回调函数
     * @param sessionId       会话ID
     * @param historyMessages 历史消息列表
     */
    private void work(OwnerAccount account,
            String title,
            Message newMessage,
            Function<Message, Void> callback,
            Function<Message, Void> failCallback,
            String sessionId,
            List<Message> historyMessages) {

        List<ChatMessage> chatMessages = LlmUtils.convertMessage(historyMessages);

        String owner = account.getOwner();
        // 保存用户消息
        saveChatRecord(owner, sessionId, title, newMessage);
        chatMessages.add(LlmUtils.convertMessage(newMessage));

        StreamingChatModel chatModel = LlmUtils.buildChatModel(account, false);

        String messageId = "assistant" + System.currentTimeMillis();

        chatModel.chat(chatMessages, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                callback.apply(new Message(messageId, "assistant", partialResponse));
            }

            @Override
            public void onPartialThinking(PartialThinking partialThinking) {
                Message thinkingMessage = new Message();
                thinkingMessage.setMessageId(messageId);
                thinkingMessage.setRole("assistant");
                thinkingMessage.setReasoningContent(partialThinking.text());
                callback.apply(thinkingMessage);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                // 保存模型回复
                Message assistantMessage = new Message("assistant", completeResponse.aiMessage().text());
                assistantMessage.setReasoningContent(completeResponse.aiMessage().thinking());
                assistantMessage.setMessageId(messageId);
                saveChatRecord(owner, sessionId, title, assistantMessage);
            }

            @Override
            public void onError(Throwable error) {
                failCallback.apply(new Message("assistant", "模型请求失败:" + error.getMessage()));
            }
        });
    }

    /**
     * 保存聊天记录
     * 
     * @param owner     账户所有者
     * @param sessionId 会话ID
     * @param title     会话标题
     * @param message   消息
     */
    private void saveChatRecord(String owner, String sessionId, String title, Message message) {
        OwnerChatRecord record = new OwnerChatRecord(
                owner,
                sessionId,
                title,
                message.getRole(),
                message.getContent(),
                "");
        record.setMessageId(message.getMessageId());
        assistantService.addChatRecord(owner, sessionId, record);
    }

}