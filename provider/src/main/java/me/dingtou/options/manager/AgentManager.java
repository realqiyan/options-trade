package me.dingtou.options.manager;

import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.dao.OwnerChatRecordDAO;
import me.dingtou.options.model.Message;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

/**
 * Agent 模式
 *
 * @author qiyan
 */
@Slf4j
@Component
public class AgentManager {

    @Autowired
    private OwnerManager ownerManager;

    @Autowired
    private OwnerChatRecordDAO ownerChatRecordDAO;

    /**
     * Agent 模式
     * 
     * @param owner
     * @param sessionId
     * @param title
     * @param messages
     * @param callback
     */
    public void agent(String owner,
            String sessionId,
            String title,
            List<Message> messages,
            Function<Message, Void> callback) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'agent'");
    }

}