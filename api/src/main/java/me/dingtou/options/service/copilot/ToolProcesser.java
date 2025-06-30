package me.dingtou.options.service.copilot;

import me.dingtou.options.model.copilot.ToolCallRequest;

/**
 * 工具服务
 */
public interface ToolProcesser {

    /**
     * 是否支持
     * 
     * @param content 模型返回内容
     * @return 是否支持
     */
    boolean support(String content);

    /**
     * 从模型返回中解析工具调用信息
     * 
     * @param owner   用户
     * @param content 模型返回内容
     * 
     * @return 工具调用信息
     */
    ToolCallRequest parseToolRequest(String owner, String content);

    /**
     * 执行工具调用
     * 
     * @param toolRequest 工具调用信息
     * @return 工具调用结果
     */
    String callTool(ToolCallRequest toolRequest);

    /**
     * 根据结果构建工具调结果提示词
     * 
     * @param toolRequest 工具调用信息
     * @param toolResult  工具调用结果
     * 
     * @return 工具调结果提示词
     */
    String buildResultPrompt(ToolCallRequest toolRequest, String toolResult);

}
