package me.dingtou.options.service.copilot.processer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson2.JSON;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.model.copilot.McpToolCallRequest;
import me.dingtou.options.model.copilot.ToolCallRequest;
import me.dingtou.options.service.copilot.ToolProcesser;
import me.dingtou.options.util.DateUtils;
import me.dingtou.options.util.McpUtils;
import me.dingtou.options.util.TemplateRenderer;

/**
 * mcp工具处理
 */
@Component
@Slf4j
public class McpToolProcesser implements ToolProcesser {

    private static final Pattern pattern = Pattern.compile(
            "<use_mcp_tool>[\\s\\S]*?<server_name>(.*?)</server_name>[\\s\\S]*?<tool_name>(.*?)</tool_name>[\\s\\S]*?<arguments>(.*?)</arguments>[\\s\\S]*?</use_mcp_tool>",
            Pattern.DOTALL);

    @Override
    public boolean support(String content) {
        if (StringUtils.isBlank(content)) {
            return false;
        }
        return content.contains("<use_mcp_tool>");
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<ToolCallRequest> parseToolRequest(String owner, String content) {
        // 尝试解析use_mcp_tool
        List<ToolCallRequest> toolCalls = new ArrayList<>();
        try {
            ToolCallRequest toolCall = null;
            // 提取整个XML结构中的参数
            java.util.regex.Matcher matcher = pattern.matcher(content);
            while (matcher.find()) {
                String serverName = matcher.group(1).trim();
                String toolName = matcher.group(2).trim();
                String argsContent = matcher.group(3).trim();
                Map<String, Object> argsMap = JSON.parseObject(argsContent, Map.class);
                toolCall = new McpToolCallRequest(owner, serverName, toolName, argsMap);
                log.info("Find Tool {} -> {}", serverName, toolName);
                toolCalls.add(toolCall);
            }
        } catch (Exception xmlException) {
            log.error("Failed to parse use_mcp_tool from XML: {}", xmlException.getMessage());
        }
        return toolCalls;
    }

    @SuppressWarnings({ "unchecked" })
    @Override
    public String callTool(ToolCallRequest toolCallRequest) {
        if (!(toolCallRequest instanceof McpToolCallRequest)) {
            return "参数异常";
        }
        McpToolCallRequest mcpToolCallRequest = (McpToolCallRequest) toolCallRequest;
        try {

            McpSyncClient client = McpUtils.getMcpClient(mcpToolCallRequest.getOwner(),
                    mcpToolCallRequest.getServerName());

            Map params = mcpToolCallRequest.getArguments();

            CallToolResult result = client.callTool(new CallToolRequest(mcpToolCallRequest.getToolName(), params));
            TextContent content = (TextContent) result.content().get(0);
            String jsonText = content.text();

            log.info("callTool {}.{} -> result: {}",
                    mcpToolCallRequest.getServerName(),
                    mcpToolCallRequest.getName(),
                    jsonText);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(jsonText);
            if (node.isNumber()) {
                return new BigDecimal(jsonText).toPlainString();
            } else if (node.isObject() || node.isArray() || node.isBoolean()) {
                return jsonText;
            } else {
                return node.asText();
            }
        } catch (Exception e) {
            log.error("Failed to call server: {} tool: {} arguments: {} error: {}",
                    mcpToolCallRequest.getServerName(),
                    mcpToolCallRequest.getName(),
                    mcpToolCallRequest.getArguments(),
                    e.getMessage(), e);
            return "调用工具失败:" + e.getMessage();
        }
    }

    @Override
    public String buildResultPrompt(ToolCallRequest toolRequest, String toolResult) {

        Map<String, Object> data = new HashMap<>();
        data.put("toolRequest", toolRequest);
        data.put("toolResult", toolResult);
        data.put("time", DateUtils.currentTime());
        // 渲染模板
        return TemplateRenderer.render("agent_mcp_tool_result_prompt.ftl", data);

    }

}
