package me.dingtou.options.service.copilot.processer;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.model.copilot.McpToolCallRequest;
import me.dingtou.options.model.copilot.ToolCallRequest;
import me.dingtou.options.service.copilot.ToolProcesser;
import me.dingtou.options.util.TemplateRenderer;

/**
 * mcp工具处理
 */
@Component
@Slf4j
public class McpToolProcesser implements ToolProcesser {

    @Override
    public boolean support(String content) {
        if (StringUtils.isBlank(content)) {
            return false;
        }
        return content.contains("<use_mcp_tool>");
    }

    @Override
    public ToolCallRequest parseToolRequest(String content) {
        // 尝试解析use_mcp_tool
        try {
            ToolCallRequest toolCall = null;
            // 提取整个XML结构中的参数
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                    "<use_mcp_tool>[\\s\\S]*?<server_name>(.*?)</server_name>[\\s\\S]*?<tool_name>(.*?)</tool_name>[\\s\\S]*?<arguments>(.*?)</arguments>[\\s\\S]*?</use_mcp_tool>",
                    java.util.regex.Pattern.DOTALL);
            java.util.regex.Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                String serverName = matcher.group(1).trim();
                String toolName = matcher.group(2).trim();
                String argsContent = matcher.group(3).trim();
                toolCall = new McpToolCallRequest(serverName, toolName, argsContent);
                return toolCall;
            }
        } catch (Exception xmlException) {
            log.error("Failed to parse use_mcp_tool from XML: {}", xmlException.getMessage());
        }

        // 继续解析其他工具调用

        // 默认返回null
        log.error("Unrecognized tool call format: {}", content);
        return null;
    }

    @Override
    public String callTool(ToolCallRequest toolCallRequest) {
        if (!(toolCallRequest instanceof McpToolCallRequest)) {
            return "参数异常";
        }
        McpToolCallRequest mcpToolCallRequest = (McpToolCallRequest) toolCallRequest;

        // TODO 执行工具调用
        return "999";
    }

    @Override
    public String buildResultPrompt(ToolCallRequest toolRequest, String toolResult) {

        Map<String, Object> data = new HashMap<>();
        data.put("toolRequest", toolRequest);
        data.put("toolResult", toolResult);
        data.put("time", new Date().toString());
        // 渲染模板
        return TemplateRenderer.render("agent_mcp_tool_result_prompt.ftl", data);

    }

}
