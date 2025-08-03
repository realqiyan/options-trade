package me.dingtou.options.service.copilot;

import java.util.function.Function;

import me.dingtou.options.model.Message;

/**
 * copilot服务
 */
public interface CopilotService {

        /**
         * 获取模式
         * 
         * @return 模式
         */
        String mode();

        /**
         * 启动对话
         * 
         * @param owner        用户信息
         * @param sessionId    会话ID
         * @param title        对话标题
         * @param message      对话内容
         * @param callback     成功回调
         * @param failCallback 失败回调
         * 
         * @return 会话ID
         */
        String start(String owner,
                        String sessionId,
                        String title,
                        Message message,
                        Function<Message, Void> callback,
                        Function<Message, Void> failCallback);

        /**
         * 继续对话
         * 
         * @param owner        用户信息
         * @param sessionId    会话ID
         * @param message      对话内容
         * @param callback     成功回调
         * @param failCallback 失败回调
         * 
         * @return 会话ID
         */
        void continuing(String owner,
                        String sessionId,
                        Message message,
                        Function<Message, Void> callback,
                        Function<Message, Void> failCallback);

}
