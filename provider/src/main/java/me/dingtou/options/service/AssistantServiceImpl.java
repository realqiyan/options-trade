package me.dingtou.options.service;

import java.util.List;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.google.adk.agents.LlmAgent;
import com.google.adk.events.Event;
import com.google.adk.models.langchain4j.LangChain4j;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import com.google.genai.types.Part;

import dev.langchain4j.model.chat.ChatModel;
import io.reactivex.rxjava3.core.Flowable;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.constant.AccountExt;
import me.dingtou.options.dao.OwnerChatRecordDAO;
import me.dingtou.options.manager.OwnerManager;
import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.model.OwnerChatRecord;
import me.dingtou.options.util.LlmUtils;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AssistantServiceImpl implements AssistantService {

    @Autowired
    private OwnerManager ownerManager;
    @Autowired
    private OwnerChatRecordDAO ownerChatRecordDAO;

    private static final String SESSION_TITLE_DESC = "生成会话标题";
    private static final String SESSION_TITLE_INSTRUCTION = """
            你的任务是根据用户消息生成会话标题。

            要求：
            * 标题必须控制在20个字以内。
            * 标题尽可能的包含关键信息，例如：标的、交易日期。
            * 直接返回标题，不允许附加任何其他信息和符号。
            """;

    @Override
    public Map<String, Object> getSettings(String owner) {
        // 查询用户账号
        OwnerAccount ownerAccount = ownerManager.queryOwnerAccount(owner);
        if (ownerAccount == null) {
            // 返回默认设置
            Map<String, Object> defaultSettings = new HashMap<>();
            defaultSettings.put("mcpSettings", "");
            defaultSettings.put("temperature", 0.1);
            return defaultSettings;
        }

        String mcpSettings = ownerAccount.getExtValue(AccountExt.AI_MCP_SETTINGS, "");

        // 从账号扩展字段中获取设置
        Map<String, Object> settings = new HashMap<>();
        settings.put("mcpSettings", mcpSettings);
        settings.put("temperature", Double.parseDouble(
                ownerAccount.getExtValue(AccountExt.AI_API_TEMPERATURE, "0.1")));

        return settings;
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
    public boolean updateSettings(String owner, String mcpSettings, Double temperature) {
        // 查询用户账号
        OwnerAccount ownerAccount = ownerManager.queryOwnerAccount(owner);
        if (ownerAccount == null) {
            return false;
        }

        // 更新AI设置
        ownerAccount.setExtValue(AccountExt.AI_MCP_SETTINGS, mcpSettings);
        ownerAccount.setExtValue(AccountExt.AI_API_TEMPERATURE, String.valueOf(temperature));

        // 保存到数据库
        int rows = ownerManager.updateOwnerAccount(ownerAccount);
        return rows > 0;
    }

    @Override
    public Boolean addChatRecord(String owner, String sessionId, OwnerChatRecord record) {
        if (null == record) {
            return Boolean.FALSE;
        }
        record.setOwner(owner);
        record.setSessionId(sessionId);
        int row = ownerChatRecordDAO.insert(record);

        return row > 0;
    }

    @Override
    public OwnerChatRecord getRecordByMessageId(String owner, String messageId) {
        // 查询这条消息的会话ID
        LambdaQueryWrapper<OwnerChatRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OwnerChatRecord::getOwner, owner)
                .eq(OwnerChatRecord::getMessageId, messageId);
        return ownerChatRecordDAO.selectOne(queryWrapper);
    }

    @Override
    public String generateSessionTitle(String owner, String message) {
        OwnerAccount ownerAccount = ownerManager.queryOwnerAccount(owner);
        if (ownerAccount == null) {
            return "";
        }

        try {
            String name = "generate_title";
            ChatModel chatModel = LlmUtils.buildChatModel(ownerAccount, false);

            // 构建流式ChatModel
            LlmAgent titleAgent = LlmAgent.builder()
                    .name(name)
                    .description(SESSION_TITLE_DESC)
                    .model(new LangChain4j(chatModel))
                    .instruction(SESSION_TITLE_INSTRUCTION)
                    .build();

            InMemoryRunner runner = new InMemoryRunner(titleAgent);
            Session session = runner
                    .sessionService()
                    .createSession(name, owner)
                    .blockingGet();
            Content userMsg = Content.fromParts(Part.fromText(message));
            Flowable<Event> events = runner.runAsync(owner, session.id(), userMsg);
            return LlmUtils.filterThink(events.blockingFirst().stringifyContent());
        } catch (Exception e) {
            log.error("生成会话标题失败 message:{}", e.getMessage(), e);
            return "";
        }
    }
}
