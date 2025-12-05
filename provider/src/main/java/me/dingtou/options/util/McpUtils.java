package me.dingtou.options.util;

import java.net.URL;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema.ClientCapabilities;
import lombok.extern.slf4j.Slf4j;

/**
 * mcp工具类
 */
@Slf4j
public class McpUtils {

    public static final String MCP_TYPE_SSE = "sse";
    public static final String MCP_TYPE_STREAMABLE_HTTP = "streamable-http";

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
        log.info("获取用户[{}]的Mcp客户端集合", owner);
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
        log.info("获取用户[{}]的服务[{}]的Mcp客户端", owner, serverName);
        Map<String, McpSyncClient> mcpServers = OWNER_MCP_SERVERS.get(owner);
        if (null == mcpServers) {
            log.warn("用户[{}]未配置任何Mcp服务", owner);
            return null;
        }
        McpSyncClient mcpSyncClient = mcpServers.get(serverName);
        if (mcpSyncClient == null) {
            log.warn("用户[{}]的服务[{}]不存在", owner, serverName);
        }
        return mcpSyncClient;
    }

    /**
     * 初始化mcp客户端
     * 
     * @param owner       用户
     * @param mcpSettings mcp设置
     */
    public static void initMcpClient(String owner, String mcpSettings) {
        if (owner == null || owner.isEmpty()) {
            log.error("初始化McpClient失败: 用户名为空");
            return;
        }
        Map<String, ServerParameters> serverParameters = getServerParameters(mcpSettings);
        for (Map.Entry<String, ServerParameters> entry : serverParameters.entrySet()) {
            String serverName = entry.getKey();
            ServerParameters serverParameter = entry.getValue();
            String type = serverParameter.type();
            if (type == null || type.isEmpty()) {
                log.error("服务[{}]初始化失败: type为空", serverName);
                continue;
            }
            try {
                initMcpSseClient(owner, serverName, serverParameter);
            } catch (Exception e) {
                log.error("服务[{}]初始化失败: {}", serverName, e.getMessage(), e);
            }

        }
        log.info("用户[{}]的Mcp客户端初始化完成", owner);

    }

    /**
     * 转换参数
     * 
     * @param mcpSettings mcp设置
     * @return 转换后的参数
     */
    public static Map<String, ServerParameters> getServerParameters(String mcpSettings) {
        if (mcpSettings == null || mcpSettings.isEmpty()) {
            log.error("初始化McpClient失败: mcpSettings为空");
            return Collections.emptyMap();
        }
        Map<String, ServerParameters> result = new HashMap<>();
        try {
            // 初始化mcp服务器
            JSONObject mcpServers = JSON.parseObject(mcpSettings).getJSONObject("mcpServers");
            if (null == mcpServers) {
                log.error("初始化McpClient失败: mcpServers配置为空");
                return Collections.emptyMap();
            }

            log.info("发现{}个Mcp服务需要初始化", mcpServers.size());
            for (String serverName : mcpServers.keySet()) {
                JSONObject server = mcpServers.getJSONObject(serverName);
                String type = server.getString("type");
                if (type == null || type.isEmpty()) {
                    log.error("服务[{}]初始化失败: type为空", serverName);
                    continue;
                }
                String url = server.getString("url");
                if (url == null || url.isEmpty()) {
                    log.error("服务[{}]初始化失败: URL为空", serverName);
                    continue;
                }
                Map<String, Object> headers = server.getObject("headers", new TypeReference<Map<String, Object>>() {
                });
                log.info("正在初始化Mcp服务: {} -> {}", serverName, url);
                ServerParameters serverParameters = ServerParameters.builder()
                        .type(type)
                        .url(url)
                        .headers(headers)
                        .build();
                result.put(serverName, serverParameters);
            }
            log.info("Mcp客户端初始化完成");
        } catch (Exception e) {
            log.error("初始化McpClient时发生异常", e);
        }
        return result;
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
     * @throws Exception
     */
    public synchronized static McpSyncClient initMcpSseClient(String owner,
            String serverName,
            ServerParameters serverParameter) throws Exception {

        Map<String, McpSyncClient> mcpServers = OWNER_MCP_SERVERS
                .computeIfAbsent(owner, k -> new ConcurrentHashMap<>());

        McpSyncClient mcpSyncClient = mcpServers.get(serverName);
        if (null != mcpSyncClient) {
            log.info("McpClient 已缓存 -> {}", serverName);
            return mcpSyncClient;
        }

        // Create a transport
        Builder requestBuilder = HttpRequest.newBuilder()
                .header("Content-Type", "application/json");

        var headers = serverParameter.headers();
        var type = serverParameter.type();
        var url = serverParameter.url();
        if (null != headers && !headers.isEmpty()) {
            for (Map.Entry<String, Object> entry : headers.entrySet()) {
                requestBuilder.header(entry.getKey(), entry.getValue().toString());
            }
        }

        McpClientTransport transport;
        URL serverUrl = new URL(url);
        String endpoint = serverUrl.getPath()
                + (null != serverUrl.getQuery() ? "?" + serverUrl.getQuery() : "");
        switch (type) {
            case MCP_TYPE_SSE:
                transport = HttpClientSseClientTransport.builder(url)
                        .sseEndpoint(endpoint)
                        .requestBuilder(requestBuilder)
                        .build();
                break;
            case MCP_TYPE_STREAMABLE_HTTP:
                try {
                    transport = HttpClientStreamableHttpTransport.builder(url)
                            .endpoint(endpoint)
                            .requestBuilder(requestBuilder)
                            .build();
                } catch (Exception e) {
                    log.error("McpClient初始化失败, url:{}", url, e);
                    throw new RuntimeException(e);
                }
                break;
            default:
                throw new IllegalArgumentException("不支持的Mcp类型: " + serverParameter.toString());
        }

        // Create a sync client with custom configuration
        McpSyncClient client = McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(60))
                .capabilities(ClientCapabilities.builder()
                        .roots(true)
                        .sampling()
                        .build())
                .build();
        // Initialize connection
        try {
            long startTime = System.currentTimeMillis();
            client.initialize();
            long cost = System.currentTimeMillis() - startTime;
            log.info("McpClient初始化成功, serverName: {}, 耗时: {}ms", serverName, cost);
        } catch (Exception e) {
            log.error("McpClient初始化失败, serverName: " + serverName, e);
            return null;
        }

        mcpServers.put(serverName, client);
        log.info("Mcp服务[{}]已注册到用户[{}]", serverName, owner);

        return client;
    }

    static class ServerParameters {

        /**
         * streamable-http / sse
         */
        private String type;
        /**
         * mcp服务地址
         */
        private String url;
        /**
         * 请求头
         */
        private Map<String, Object> headers;

        public static ServerParameters builder() {
            return new ServerParameters();
        }

        /**
         * streamable-http / sse
         * 
         * @return
         */
        public String type() {
            return this.type;
        }

        /**
         * mcp服务地址
         */
        public String url() {
            return this.url;
        }

        /**
         * 请求头
         */
        public Map<String, Object> headers() {
            return this.headers;
        }

        public ServerParameters type(String type) {
            this.type = type;
            return this;
        }

        public ServerParameters url(String url) {
            this.url = url;
            return this;
        }

        public ServerParameters headers(Map<String, Object> headers) {
            this.headers = headers;
            return this;
        }

        public ServerParameters build() {
            return this;
        }

        @Override
        public String toString() {
            return "SseServerParameters{" +
                    "type='" + type + '\'' +
                    ", url='" + url + '\'' +
                    ", headers=" + headers +
                    '}';
        }
    }

}
