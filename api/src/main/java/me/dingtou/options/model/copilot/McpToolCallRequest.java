package me.dingtou.options.model.copilot;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class McpToolCallRequest extends ToolCallRequest {

    /**
     * 服务名称
     */
    private String serverName;
    /**
     * 工具名称
     */
    private String toolName;
    /**
     * 参数
     */
    private String arguments;

    public McpToolCallRequest() {
        super("use_mcp_tool");
    }

    public McpToolCallRequest(String serverName, String toolName, String argsContent) {
        this();
        this.serverName = serverName;
        this.toolName = toolName;
        this.arguments = argsContent;
    }

}
