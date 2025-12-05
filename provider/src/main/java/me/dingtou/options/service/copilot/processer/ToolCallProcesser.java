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

    private static final String USE_TOOL = "use_tool";

    private static final Pattern USE_TOOL_PATTERN = Pattern.compile("<use_tool>([\\s\\S]*?)</use_tool>",
            Pattern.DOTALL);

    @Override
    public boolean support(String content) {
        if (StringUtils.isBlank(content)) {
            return false;
        }
        return content.contains("<use_tool>");
    }

    public static void main(String[] args) {
        String text = """
                 </thinking> <use_tool> <use_tool> [{\"name\": \"common.summary\", \"arguments\": {}}] </use_tool> </use_tool>
                """;
        List<ToolCallRequest> toolCalls = new ToolCallProcesser().parseToolRequest("owner", text);
        System.out.println(toolCalls);
    }

    @Override
    public List<ToolCallRequest> parseToolRequest(String owner, String content) {
        List<ToolCallRequest> toolCalls = new ArrayList<>();
        try {
            java.util.regex.Matcher matcher = USE_TOOL_PATTERN.matcher(content);
            while (matcher.find()) {
                String callFunc = matcher.group(1).trim();
                /**
                 * <use_tool>
                 * {"name": "Tool name", "parameters": {"Parameter name": "Parameter content"}},
                 * </use_tool>
                 * </use_tool>
                 * [
                 * {"name": "Tool name", "parameters": {"Parameter name": "Parameter content"}},
                 * {"name": "...", "parameters": {"...": "...", "...": "..."}},
                 * ...
                 * ]
                 * </use_tool>
                 */

                // Check if it's an array of tool calls
                if (callFunc.startsWith("[")) {
                    // Handle multiple tool calls
                    List<JSONObject> jsonObjects = JSON.parseArray(callFunc, JSONObject.class);
                    for (JSONObject jsonObject : jsonObjects) {
                        toolCalls.add(createToolCall(owner, jsonObject));
                    }
                } else {
                    // Handle single tool call
                    JSONObject jsonObject = JSON.parseObject(callFunc);
                    toolCalls.add(createToolCall(owner, jsonObject));
                }
            }
        } catch (Exception xmlException) {
            log.error("Failed to parse use_tool: {}", content, xmlException);
            throw new IllegalArgumentException("Failed to parse use_tool: " + xmlException.getMessage());
        }
        return toolCalls;
    }

    private ToolCallRequest createToolCall(String owner, JSONObject jsonObject) {
        String name = jsonObject.getString("name");
        // 兼容大模型返回嵌套模式：[{"arguments":{"name":"common.summary","arguments":{}},"name":"use_tool"}]
        if (USE_TOOL.equals(name)) {
            jsonObject = jsonObject.getJSONObject("arguments");
            name = jsonObject.getString("name");
        }

        Map<String, Object> arguments = jsonObject.getObject("arguments", Map.class);

        int index = name.indexOf(".");
        // 工具名称 = 服务名.工具名
        // 服务名不能为空
        if (index <= 0) {
            throw new IllegalArgumentException("Invalid tool name: " + name);
        }
        String serverName = name.substring(0, index);
        String toolName = name.substring(index + 1);

        ToolCallRequest toolCall = new McpToolCallRequest(owner, serverName, toolName, arguments);
        log.info("Find Tool {} -> {}", serverName, toolName);
        return toolCall;
    }

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
