package me.dingtou.options.service;

import java.util.List;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import me.dingtou.options.constant.AccountExt;
import me.dingtou.options.dao.OwnerChatRecordDAO;
import me.dingtou.options.manager.OwnerManager;
import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.model.OwnerChatRecord;
import me.dingtou.options.util.TemplateRenderer;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AssistantServiceImpl implements AssistantService {

    @Autowired
    private AuthService authService;
    @Autowired
    private OwnerManager ownerManager;
    @Autowired
    private OwnerChatRecordDAO ownerChatRecordDAO;

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
        if (StringUtils.isBlank(mcpSettings)) {
            Map<String, Object> params = new HashMap<>();
            Date expireDate = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 365 * 1000L);
            params.put("jwt", authService.jwt(ownerAccount.getOwner(), expireDate));
            mcpSettings = TemplateRenderer.render("config_default_mcp_settings.ftl", params);
        }

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
}
