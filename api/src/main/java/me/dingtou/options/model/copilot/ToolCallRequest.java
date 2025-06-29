package me.dingtou.options.model.copilot;

import lombok.Data;

@Data
public abstract class ToolCallRequest {

    /**
     * 工具名称
     */
    private String tool;

    public ToolCallRequest(String tool) {
        this.tool = tool;
    }
}
