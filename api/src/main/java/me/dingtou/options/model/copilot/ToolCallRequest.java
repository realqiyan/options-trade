package me.dingtou.options.model.copilot;

import lombok.Data;

@Data
public abstract class ToolCallRequest {

    /**
     * 内置的总结工具
     */
    String SUMMARY_TOOL = "common.summary";

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

    public boolean isSummary() {
        return SUMMARY_TOOL.equals(this.tool);
    }
}
