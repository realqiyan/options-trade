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

    public McpToolCallRequest(String owner) {
        super(owner, "use_mcp_tool");
    }

    public McpToolCallRequest(String owner, String serverName, String toolName, String argsContent) {
        this(owner);
        this.serverName = serverName;
        this.toolName = toolName;
        this.arguments = argsContent;
    }

}
