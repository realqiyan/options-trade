package me.dingtou.options.model.copilot;

import java.util.Map;

import lombok.Data;

@Data
public class ToolCallRequest {

    /**
     * 内置的总结工具
     */
    public static final String SUMMARY_TOOL = "common.summary";


    /**
     * 工具名称
     */
    private String name;

    /**
     * 参数
     */
    private Map<String, Object> arguments;

    public ToolCallRequest(String name, Map<String, Object> arguments) {
        this.name = name;
        this.arguments = arguments;
    }
}
