package me.dingtou.options.manager;

import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.dao.OwnerChatRecordDAO;
import me.dingtou.options.model.Message;

import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.model.OwnerChatRecord;
import me.dingtou.options.util.AccountExtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.util.List;
import java.util.function.Function;

/**
 * Agent 模式
 *
 * @author qiyan
 */
@Slf4j
@Component
public class AskManager {

    @Autowired
    private ChatManager chatManager;
    
    @Autowired
    private OwnerManager ownerManager;

    @Autowired
    private OwnerChatRecordDAO ownerChatRecordDAO;

    /**
     * Ask 模式
     * 
     * @param owner
     * @param sessionId
     * @param title
     * @param messages
     * @param callback
     */
    public void ask(String owner,
            String sessionId,
            String title,
            List<Message> messages,
            Function<Message, Void> callback) {
        OwnerAccount ownerAccount = ownerManager.queryOwnerAccount(owner);
        if (null == messages
                || messages.isEmpty()
                || null == ownerAccount
                || null == AccountExtUtils.getAiApiKey(ownerAccount)) {
            return;
        }

        // 检查消息列表中是否有已存在的消息ID
        for (Message message : messages) {
            if (message.getId() != null) {
                // 查询这条消息的会话ID
                LambdaQueryWrapper<OwnerChatRecord> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(OwnerChatRecord::getOwner, owner)
                        .eq(OwnerChatRecord::getMessageId, message.getId());
                OwnerChatRecord record = ownerChatRecordDAO.selectOne(queryWrapper);
                if (record != null) {
                    sessionId = record.getSessionId();
                    break;
                }
            }
        }

        // 保存用户新消息
        final String finalSessionId = sessionId;
        messages.stream().filter(message -> "user".equals(message.getRole()) && null == message.getId())
                .forEach(message -> {
                    OwnerChatRecord userRecord = new OwnerChatRecord(owner,
                            finalSessionId,
                            String.valueOf(System.currentTimeMillis()),
                            title,
                            "user",
                            message.getContent(),
                            null);
                    ownerChatRecordDAO.insert(userRecord);
                });

        // 使用ChatManager发送消息并获取响应
        ChatManager.ChatResult result = chatManager.sendChatMessage(ownerAccount, messages, callback);

        // 保存AI助手的完整回复
        if (!result.getContent().isEmpty()) {
            OwnerChatRecord assistantRecord = new OwnerChatRecord(
                    owner,
                    finalSessionId,
                    result.getMessageId(),
                    title,
                    "assistant",
                    result.getContent(),
                    result.getReasoningContent());
            ownerChatRecordDAO.insert(assistantRecord);
        }
    }

}