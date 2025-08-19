package me.dingtou.options.config;

import me.dingtou.options.service.mcp.DataQueryMcpService;
import me.dingtou.options.service.mcp.KnowledgeMcpService;
import me.dingtou.options.service.mcp.OwnerQueryMcpService;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpConfiguration {
	@Bean
	public ToolCallbackProvider dataQueryTools(DataQueryMcpService dataQueryMcpService) {
		return MethodToolCallbackProvider.builder()
				.toolObjects(dataQueryMcpService)
				.build();
	}

	@Bean
	public ToolCallbackProvider ownerQueryTools(OwnerQueryMcpService ownerQueryMcpService) {
		return MethodToolCallbackProvider.builder()
				.toolObjects(ownerQueryMcpService)
				.build();
	}

	@Bean
	public ToolCallbackProvider knowledgeTools(KnowledgeMcpService knowledgeMcpService) {
		return MethodToolCallbackProvider.builder()
				.toolObjects(knowledgeMcpService)
				.build();
	}

}
