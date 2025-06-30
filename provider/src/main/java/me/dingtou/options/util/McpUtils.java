package me.dingtou.options.util;

import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema.ClientCapabilities;

/**
 * mcp工具类
 */
public class McpUtils {

    private static final Map<String, McpSyncClient> MCP_SERVERS = new ConcurrentHashMap<>();

    /**
     * 获取mcp客户端
     * 
     * @param owner      用户
     * @param serverName mcp服务名称
     * @return
     */
    public static McpSyncClient getMcpClient(String owner, String serverName) {
        String key = buildKey(owner, serverName);
        McpSyncClient mcpSyncClient = MCP_SERVERS.get(key);
        return mcpSyncClient;
    }

    /**
     * 初始化mcp客户端
     * 
     * @param owner      用户
     * @param serverName mcp服务名称
     * @param url        mcp服务地址
     * @param headers    请求头
     * 
     * @return McpSyncClient
     */
    public synchronized static McpSyncClient initMcpSseClient(String owner,
            String serverName,
            String url,
            Map<String, String> headers) {

        String key = buildKey(owner, serverName);
        McpSyncClient mcpSyncClient = MCP_SERVERS.get(key);
        if (null != mcpSyncClient) {
            return mcpSyncClient;
        }

        // Create a transport
        Builder requestBuilder = HttpRequest.newBuilder()
                .header("Content-Type", "application/json");

        if (null != headers && !headers.isEmpty()) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                requestBuilder.header(entry.getKey(), entry.getValue());
            }
        }

        HttpClientSseClientTransport transport = HttpClientSseClientTransport.builder(url)
                .requestBuilder(requestBuilder)
                .build();
        // Create a sync client with custom configuration
        McpSyncClient client = McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(30))
                .capabilities(ClientCapabilities.builder()
                        .roots(true)
                        .sampling()
                        .build())
                .build();
        // Initialize connection
        client.initialize();

        MCP_SERVERS.put(key, client);

        return client;
    }

    /**
     * 构建key
     * 
     * @param owner      用户
     * @param serverName mcp服务名称
     * @return key
     */
    private static String buildKey(String owner, String serverName) {
        return owner + ":" + serverName;
    }

}
