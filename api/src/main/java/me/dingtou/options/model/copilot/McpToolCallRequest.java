package me.dingtou.options.model.copilot;

import java.util.Map;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class McpToolCallRequest extends ToolCallRequest {

    /**
     * owner
     */
    private String owner;

    /**
     * 服务名称
     */
    private String serverName;
    /**
     * 工具名称
     */
    private String toolName;

    public McpToolCallRequest(String owner, String name, Map<String, Object> argsContent) {
        super(name, argsContent);
        this.owner = owner;
    }

    public McpToolCallRequest(String owner, String serverName, String toolName, Map<String, Object> argsContent) {
        this(owner, serverName + "." + toolName, argsContent);
        this.serverName = serverName;
        this.toolName = toolName;
    }

}
