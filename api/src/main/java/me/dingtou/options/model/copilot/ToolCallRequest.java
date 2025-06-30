package me.dingtou.options.model.copilot;

import lombok.Data;

@Data
public abstract class ToolCallRequest {

    /**
     * owner
     */
    private String owner;

    /**
     * 工具名称
     */
    private String tool;

    public ToolCallRequest(String owner, String tool) {
        this.owner = owner;
        this.tool = tool;
    }
}
