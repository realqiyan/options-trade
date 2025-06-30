package me.dingtou.options.service.copilot.processer;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.constant.AccountExt;
import me.dingtou.options.manager.OwnerManager;
import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.model.copilot.McpToolCallRequest;
import me.dingtou.options.model.copilot.ToolCallRequest;
import me.dingtou.options.service.AuthService;
import me.dingtou.options.service.copilot.ToolProcesser;
import me.dingtou.options.util.McpUtils;
import me.dingtou.options.util.TemplateRenderer;

/**
 * mcp工具处理
 */
@Component
@Slf4j
public class McpToolProcesser implements ToolProcesser {

    @Autowired
    private AuthService authService;

    @Autowired
    private OwnerManager ownerManager;

    @Override
    public boolean support(String content) {
        if (StringUtils.isBlank(content)) {
            return false;
        }
        return content.contains("<use_mcp_tool>");
    }

    @Override
    public ToolCallRequest parseToolRequest(String owner, String content) {
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
                toolCall = new McpToolCallRequest(owner, serverName, toolName, argsContent);
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
            if (null == client) {
                initMcpServers(mcpToolCallRequest.getOwner());
                client = McpUtils.getMcpClient(mcpToolCallRequest.getOwner(), mcpToolCallRequest.getServerName());
            }

            String arguments = mcpToolCallRequest.getArguments();
            Map params = JSON.parseObject(arguments, Map.class);

            CallToolResult result = client.callTool(new CallToolRequest(mcpToolCallRequest.getToolName(), params));
            TextContent content = (TextContent) result.content().get(0);

            return content.text();
        } catch (Exception e) {
            log.error("Failed to call server: {} tool: {} error: {}",
                    mcpToolCallRequest.getServerName(),
                    mcpToolCallRequest.getTool(),
                    e.getMessage(), e);
            return "调用工具失败:" + e.getMessage();
        }
    }

    private void initMcpServers(String owner) {

        // 初始化mcp服务器
        OwnerAccount ownerAccount = ownerManager.queryOwnerAccount(owner);
        String mcpSettings = ownerAccount.getExtValue(AccountExt.AI_MCP_SETTINGS, "");
        if (StringUtils.isBlank(mcpSettings)) {
            Map<String, Object> params = new HashMap<>();
            Date expireDate = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 365 * 1000L);
            params.put("jwt", authService.jwt(owner, expireDate));
            mcpSettings = TemplateRenderer.render("config_default_mcp_settings.ftl", params);
        }
        JSONObject mcpServers = JSON.parseObject(mcpSettings).getJSONObject("mcpServers");
        if (null == mcpServers) {
            return;
        }
        for (String serverName : mcpServers.keySet()) {
            JSONObject server = mcpServers.getJSONObject(serverName);
            String url = server.getString("url");
            Map<String, String> headers = server.getObject("headers", new TypeReference<Map<String, String>>() {
            });
            McpUtils.initMcpSseClient(owner, serverName, url, headers);
        }
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
