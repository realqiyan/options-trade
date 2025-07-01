package me.dingtou.options.util;

import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema.ClientCapabilities;

/**
 * mcp工具类
 */
public class McpUtils {

    /**
     * mcp客户端 {owner:{server:Client}}
     */
    private static final Map<String, Map<String, McpSyncClient>> OWNER_MCP_SERVERS = new ConcurrentHashMap<>();

    /**
     * 获取mcp客户端
     * 
     * @param owner
     * @return
     */
    public static Map<String, McpSyncClient> getOwnerMcpClient(String owner) {
        return OWNER_MCP_SERVERS.get(owner);
    }

    /**
     * 获取mcp客户端
     * 
     * @param owner      用户
     * @param serverName mcp服务名称
     * @return
     */
    public static McpSyncClient getMcpClient(String owner, String serverName) {
        Map<String, McpSyncClient> mcpServers = OWNER_MCP_SERVERS.get(owner);
        if (null == mcpServers) {
            return null;
        }
        McpSyncClient mcpSyncClient = mcpServers.get(serverName);
        return mcpSyncClient;
    }

    /**
     * 初始化mcp客户端
     * 
     * @param owner       用户
     * @param mcpSettings mcp设置
     */
    public static void initMcpClient(String owner, String mcpSettings) {
        // 初始化mcp服务器
        JSONObject mcpServers = JSON.parseObject(mcpSettings).getJSONObject("mcpServers");
        if (null == mcpServers) {
            return;
        }
        for (String serverName : mcpServers.keySet()) {
            JSONObject server = mcpServers.getJSONObject(serverName);
            String url = server.getString("url");
            Map<String, String> headers = server.getObject("headers", new TypeReference<Map<String, String>>() {
            });
            initMcpSseClient(owner, serverName, url, headers);
        }
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

        Map<String, McpSyncClient> mcpServers = OWNER_MCP_SERVERS.get(owner);
        if (null == mcpServers) {
            mcpServers = new ConcurrentHashMap<>();
            OWNER_MCP_SERVERS.put(owner, mcpServers);
        }

        McpSyncClient mcpSyncClient = mcpServers.get(serverName);
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

        mcpServers.put(serverName, client);

        return client;
    }

}
