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
import com.alibaba.fastjson2.JSONObject;
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
public class ToolCallProcesser implements ToolProcesser {

    private static final Pattern pattern = Pattern.compile("<tool_call>([\\s\\S]*?)</tool_call>", Pattern.DOTALL);

    @Override
    public boolean support(String content) {
        if (StringUtils.isBlank(content)) {
            return false;
        }
        return content.contains("<tool_call>");
    }

    @Override
    public List<ToolCallRequest> parseToolRequest(String owner, String content) {
        List<ToolCallRequest> toolCalls = new ArrayList<>();
        try {
            ToolCallRequest toolCall = null;
            java.util.regex.Matcher matcher = pattern.matcher(content);
            while (matcher.find()) {
                String callFunc = matcher.group(1).trim();
                // {"name": <function-name>, "arguments": <args-json-object>}
                JSONObject jsonObject = JSON.parseObject(callFunc);
                String name = jsonObject.getString("name");
                String arguments = jsonObject.getString("arguments");

                int index = name.indexOf(".");
                String serverName = name.substring(0, index);
                String toolName = name.substring(index + 1);

                toolCall = new McpToolCallRequest(owner, serverName, toolName, arguments);
                log.info("Find Tool {} -> {}", serverName, toolName);
                toolCalls.add(toolCall);
            }
        } catch (Exception xmlException) {
            log.error("Failed to parse use_mcp_tool from XML: {}", xmlException.getMessage());
        }
        return toolCalls;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public String callTool(ToolCallRequest toolCallRequest) {
        if (!(toolCallRequest instanceof McpToolCallRequest)) {
            return "参数异常";
        }
        McpToolCallRequest mcpToolCallRequest = (McpToolCallRequest) toolCallRequest;
        try {

            McpSyncClient client = McpUtils.getMcpClient(mcpToolCallRequest.getOwner(),
                    mcpToolCallRequest.getServerName());

            String arguments = mcpToolCallRequest.getArguments();
            Map params = JSON.parseObject(arguments, Map.class);

            CallToolResult result = client.callTool(new CallToolRequest(mcpToolCallRequest.getToolName(), params));
            TextContent content = (TextContent) result.content().get(0);
            String jsonText = content.text();

            log.info("callTool {}.{} -> result: {}",
                    mcpToolCallRequest.getServerName(),
                    mcpToolCallRequest.getTool(),
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
                    mcpToolCallRequest.getTool(),
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
