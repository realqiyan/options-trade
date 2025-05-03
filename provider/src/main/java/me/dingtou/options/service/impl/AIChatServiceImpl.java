package me.dingtou.options.service.impl;

import java.util.List;
import java.util.function.Function;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import me.dingtou.options.constant.AccountExt;
import me.dingtou.options.dao.OwnerAccountDAO;
import me.dingtou.options.dao.OwnerChatRecordDAO;
import me.dingtou.options.manager.ChatManager;
import me.dingtou.options.manager.OwnerManager;
import me.dingtou.options.model.Message;
import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.model.OwnerChatRecord;

import me.dingtou.options.util.AccountExtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import me.dingtou.options.service.AIChatService;

@Service
public class AIChatServiceImpl implements AIChatService {

    @Autowired
    private OwnerChatRecordDAO ownerChatRecordDAO;
    @Autowired
    private ChatManager chatManager;
    @Autowired
    private OwnerManager ownerManager;
    @Autowired
    private OwnerAccountDAO ownerAccountDAO;

    @Override
    public void chat(String owner, String sessionId, String title, List<Message> messages,
            Function<Message, Void> callback) {
        OwnerAccount ownerAccount = ownerManager.queryOwnerAccount(owner);
        if (null == messages || messages.isEmpty() || null == ownerAccount
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

    @Override
    public List<OwnerChatRecord> summaryChatRecord(String owner, int limit) {
        // GROUP BY后的沟通记录
        return ownerChatRecordDAO.summaryChatRecord(owner, limit);
    }

    @Override
    public List<OwnerChatRecord> listRecordsBySessionId(String owner, String sessionId) {
        // 根据会话ID查询记录
        LambdaQueryWrapper<OwnerChatRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OwnerChatRecord::getOwner, owner)
                .eq(OwnerChatRecord::getSessionId, sessionId)
                .orderByAsc(OwnerChatRecord::getCreateTime);

        return ownerChatRecordDAO.selectList(wrapper);
    }

    @Override
    public boolean deleteBySessionId(String owner, String sessionId) {
        // 根据会话ID删除记录
        LambdaQueryWrapper<OwnerChatRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OwnerChatRecord::getOwner, owner)
                .eq(OwnerChatRecord::getSessionId, sessionId);

        return ownerChatRecordDAO.delete(wrapper) > 0;
    }

    @Override
    public boolean updateSessionTitle(String owner, String sessionId, String title) {
        // 更新会话标题
        LambdaUpdateWrapper<OwnerChatRecord> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(OwnerChatRecord::getOwner, owner)
                .eq(OwnerChatRecord::getSessionId, sessionId)
                .set(OwnerChatRecord::getTitle, title);

        return ownerChatRecordDAO.update(null, wrapper) > 0;
    }

    @Override
    public boolean updateSettings(String owner, String systemPrompt, Double temperature) {
        // 查询用户账号
        OwnerAccount ownerAccount = ownerManager.queryOwnerAccount(owner);
        if (ownerAccount == null) {
            return false;
        }
        
        // 更新AI设置
        ownerAccount.setExtValue(AccountExt.AI_SYSTEM_PROMPT, systemPrompt);
        ownerAccount.setExtValue(AccountExt.AI_API_TEMPERATURE, String.valueOf(temperature));
        
        // 保存到数据库
        int rows = ownerAccountDAO.updateById(ownerAccount);
        return rows > 0;
    }
}
