/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.alibaba.apiopenplatform.service.gateway;

import cn.hutool.json.JSONUtil;
import com.alibaba.apiopenplatform.dto.result.agent.AgentAPIResult;
import com.alibaba.apiopenplatform.dto.result.common.DomainResult;
import com.alibaba.apiopenplatform.dto.result.common.PageResult;
import com.alibaba.apiopenplatform.dto.result.gateway.GatewayResult;
import com.alibaba.apiopenplatform.dto.result.httpapi.APIResult;
import com.alibaba.apiopenplatform.dto.result.httpapi.ApisixHttpApiResult;
import com.alibaba.apiopenplatform.dto.result.mcp.ApisixMCPServerResult;
import com.alibaba.apiopenplatform.dto.result.mcp.GatewayMCPServerResult;
import com.alibaba.apiopenplatform.dto.result.mcp.MCPConfigResult;
import com.alibaba.apiopenplatform.dto.result.model.ApisixModelResult;
import com.alibaba.apiopenplatform.dto.result.model.GatewayModelAPIResult;
import com.alibaba.apiopenplatform.dto.result.model.ModelConfigResult;
import com.alibaba.apiopenplatform.dto.result.httpapi.HttpRouteResult;
import com.alibaba.apiopenplatform.entity.Consumer;
import com.alibaba.apiopenplatform.entity.ConsumerCredential;
import com.alibaba.apiopenplatform.entity.Gateway;
import com.alibaba.apiopenplatform.service.gateway.client.ApisixClient;
import com.alibaba.apiopenplatform.service.gateway.model.ApisixConsumer;
import com.alibaba.apiopenplatform.service.gateway.model.ApisixRoute;
import com.alibaba.apiopenplatform.support.consumer.ConsumerAuthConfig;
import com.alibaba.apiopenplatform.support.consumer.ApisixAuthConfig;
import com.alibaba.apiopenplatform.support.enums.GatewayType;
import com.alibaba.apiopenplatform.support.gateway.ApisixConfig;
import com.alibaba.apiopenplatform.support.gateway.GatewayConfig;
import com.alibaba.apiopenplatform.support.product.ApisixRefConfig;
import com.aliyun.sdk.service.apig20240327.models.HttpApiApiInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * APISIX 网关操作器
 *
 * 实现 APISIX 网关的管理操作，包括路由、MCP Server、Consumer 等
 */
@Service
@Slf4j
public class ApisixOperator extends GatewayOperator<ApisixClient> {

    private static String normalizeRoutePathForPrefixMatch(String uri) {
        if (uri == null) {
            return null;
        }

        String path = uri.trim();
        if (path.endsWith("/*")) {
            path = path.substring(0, path.length() - 2);
        } else if (path.endsWith("*")) {
            path = path.substring(0, path.length() - 1);
        }

        if (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }

        return path;
    }

    @Override
    public GatewayType getGatewayType() {
        return GatewayType.APISIX;
    }

    @Override
    public PageResult<APIResult> fetchHTTPAPIs(Gateway gateway, int page, int size) {
        ApisixClient client = getClient(gateway);

        // 获取所有 Route，排除 mcp-bridge 和 ai-proxy 插件的路由
        List<ApisixRoute> routes = client.listRoutes();

        List<APIResult> httpApis = routes.stream()
                .filter(route -> !route.hasMcpBridgePlugin() && !route.hasAiProxyPlugin())
                .map(route -> new ApisixHttpApiResult().convertFrom(route))
                .collect(Collectors.toList());

        // 分页处理
        int total = httpApis.size();
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);

        if (fromIndex >= total) {
            return PageResult.of(Collections.emptyList(), page, size, total);
        }

        return PageResult.of(httpApis.subList(fromIndex, toIndex), page, size, total);
    }

    @Override
    public PageResult<APIResult> fetchRESTAPIs(Gateway gateway, int page, int size) {
        // REST APIs 与 HTTP APIs 相同，复用逻辑
        return fetchHTTPAPIs(gateway, page, size);
    }

    @Override
    public PageResult<? extends GatewayMCPServerResult> fetchMcpServers(Gateway gateway, int page, int size) {
        ApisixClient client = getClient(gateway);

        // 获取所有 Route，筛选带 mcp-bridge 插件的
        List<ApisixRoute> routes = client.listRoutes();

        List<ApisixMCPServerResult> mcpServers = routes.stream()
                .filter(ApisixRoute::hasMcpBridgePlugin)
                .map(route -> new ApisixMCPServerResult().convertFrom(route))
                .collect(Collectors.toList());

        // 分页处理
        int total = mcpServers.size();
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);

        if (fromIndex >= total) {
            return PageResult.of(Collections.emptyList(), page, size, total);
        }

        return PageResult.of(mcpServers.subList(fromIndex, toIndex), page, size, total);
    }

    @Override
    public PageResult<AgentAPIResult> fetchAgentAPIs(Gateway gateway, int page, int size) {
        return null;
    }

    @Override
    public PageResult<? extends GatewayModelAPIResult> fetchModelAPIs(Gateway gateway, int page, int size) {
        ApisixClient client = getClient(gateway);

        // 获取所有 Route，筛选带 ai-proxy 插件的
        List<ApisixRoute> routes = client.listRoutes();

        List<ApisixModelResult> models = routes.stream()
                .filter(ApisixRoute::hasAiProxyPlugin)
                .map(route -> new ApisixModelResult().convertFrom(route))
                .collect(Collectors.toList());

        // 分页处理
        int total = models.size();
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);

        if (fromIndex >= total) {
            return PageResult.of(Collections.emptyList(), page, size, total);
        }

        return PageResult.of(models.subList(fromIndex, toIndex), page, size, total);
    }

    @Override
    public String fetchAPIConfig(Gateway gateway, Object config) {
        ApisixRefConfig refConfig = (ApisixRefConfig) config;
        ApisixClient client = getClient(gateway);

        // 获取 Route 详情
        ApisixRoute route = client.getRoute(refConfig.getRouteId());
        if (route == null) {
            throw new RuntimeException("Route not found: " + refConfig.getRouteId());
        }

        String normalizedPath = normalizeRoutePathForPrefixMatch(route.getUri());

        // 构建域信息
        List<DomainResult> domains = Collections.singletonList(
                DomainResult.builder()
                        .domain("<apisix-gateway-ip>")
                        .protocol("http")
                        .build()
        );

        // 构建路由匹配信息
        HttpRouteResult.RouteMatchPath matchPath = HttpRouteResult.RouteMatchPath.builder()
                .value(normalizedPath)
                .type("PREFIX")
                .build();

        HttpRouteResult.RouteMatchResult routeMatchResult = HttpRouteResult.RouteMatchResult.builder()
                .methods(route.getMethods() != null ? route.getMethods() : Collections.singletonList("GET"))
                .path(matchPath)
                .build();

        HttpRouteResult httpRouteResult = new HttpRouteResult();
        httpRouteResult.setDomains(domains);
        httpRouteResult.setMatch(routeMatchResult);
        httpRouteResult.setDescription(route.getDesc());

        // 构建结果
        var result = new java.util.LinkedHashMap<String, Object>();
        result.put("apiId", route.getId());
        result.put("apiName", route.getName() != null ? route.getName() : route.getId());
        result.put("uri", normalizedPath);
        result.put("methods", route.getMethods());
        result.put("enabled", route.isEnabled());
        result.put("routes", Collections.singletonList(httpRouteResult));

        return JSONUtil.toJsonStr(result);
    }

    @Override
    public String fetchMcpConfig(Gateway gateway, Object conf) {
        ApisixRefConfig refConfig = (ApisixRefConfig) conf;
        ApisixClient client = getClient(gateway);

        // 获取 Route 详情
        ApisixRoute route = client.getRoute(refConfig.getRouteId());
        if (route == null) {
            throw new RuntimeException("Route not found: " + refConfig.getRouteId());
        }

        String normalizedPath = normalizeRoutePathForPrefixMatch(route.getUri());

        // 构建 MCP 配置结果
        MCPConfigResult result = new MCPConfigResult();
        result.setMcpServerName(refConfig.getMcpServerName());

        // MCP Server 配置
        MCPConfigResult.MCPServerConfig serverConfig = new MCPConfigResult.MCPServerConfig();
        serverConfig.setPath(normalizedPath);
        serverConfig.setDomains(Collections.singletonList(
                DomainResult.builder()
                        .domain("<apisix-gateway-ip>")
                        .protocol("http")
                        .build()
        ));
        result.setMcpServerConfig(serverConfig);

        // 解析 mcp-bridge 插件配置
        if (route.hasMcpBridgePlugin()) {
            Map<String, Object> mcpBridgeConfig = route.getMcpBridgeConfig();
            if (mcpBridgeConfig != null) {
                // 将 mcp-bridge 配置转为 JSON 字符串作为 tools
                result.setTools(JSONUtil.toJsonStr(mcpBridgeConfig));
            }
        }

        // 元数据
        MCPConfigResult.McpMetadata meta = new MCPConfigResult.McpMetadata();
        meta.setSource(GatewayType.APISIX.name());
        meta.setCreateFromType("MCP_BRIDGE");
        meta.setProtocol("SSE"); // mcp-bridge 默认使用 SSE
        result.setMeta(meta);

        return JSONUtil.toJsonStr(result);
    }

    @Override
    public String fetchAgentConfig(Gateway gateway, Object conf) {
        return "";
    }

    @Override
    public String fetchModelConfig(Gateway gateway, Object conf) {
        ApisixRefConfig refConfig = (ApisixRefConfig) conf;
        ApisixClient client = getClient(gateway);

        // 获取 Route 详情
        ApisixRoute route = client.getRoute(refConfig.getRouteId());
        if (route == null) {
            throw new RuntimeException("Route not found: " + refConfig.getRouteId());
        }

        String normalizedPath = normalizeRoutePathForPrefixMatch(route.getUri());

        // 构建域信息
        List<DomainResult> domains = Collections.singletonList(
                DomainResult.builder()
                        .domain("<apisix-gateway-ip>")
                        .protocol("http")
                        .build()
        );

        // 构建路由匹配信息
        HttpRouteResult.RouteMatchPath matchPath = HttpRouteResult.RouteMatchPath.builder()
                .value(normalizedPath)
                .type("PREFIX")
                .build();

        HttpRouteResult.RouteMatchResult routeMatchResult = HttpRouteResult.RouteMatchResult.builder()
                .methods(route.getMethods() != null ? route.getMethods() : Collections.singletonList("POST"))
                .path(matchPath)
                .build();

        HttpRouteResult httpRouteResult = new HttpRouteResult();
        httpRouteResult.setDomains(domains);
        httpRouteResult.setMatch(routeMatchResult);
        httpRouteResult.setDescription(route.getDesc());

        // 构建模型配置
        ModelConfigResult.ModelAPIConfig config = ModelConfigResult.ModelAPIConfig.builder()
                .aiProtocols(Collections.singletonList("OpenAI/V1"))
                .modelCategory("Text")
                .routes(Collections.singletonList(httpRouteResult))
                .build();

        ModelConfigResult result = new ModelConfigResult();
        result.setModelAPIConfig(config);

        return JSONUtil.toJsonStr(result);
    }

    @Override
    public PageResult<GatewayResult> fetchGateways(Object param, int page, int size) {
        throw new UnsupportedOperationException("APISIX gateway does not support fetching Gateways");
    }

    @Override
    public String createConsumer(Consumer consumer, ConsumerCredential credential, GatewayConfig config) {
        ApisixConfig apisixConfig = config.getApisixConfig();
        ApisixClient client = new ApisixClient(apisixConfig);

        String consumerId = consumer.getConsumerId();
        String apiKey = extractApiKey(credential);

        ApisixConsumer apisixConsumer = buildApisixConsumer(consumerId, apiKey);
        client.createConsumer(consumerId, apisixConsumer);

        return consumerId;
    }

    @Override
    public void updateConsumer(String consumerId, ConsumerCredential credential, GatewayConfig config) {
        ApisixConfig apisixConfig = config.getApisixConfig();
        ApisixClient client = new ApisixClient(apisixConfig);

        String apiKey = extractApiKey(credential);
        ApisixConsumer apisixConsumer = buildApisixConsumer(consumerId, apiKey);
        client.updateConsumer(consumerId, apisixConsumer);
    }

    @Override
    public void deleteConsumer(String consumerId, GatewayConfig config) {
        ApisixConfig apisixConfig = config.getApisixConfig();
        ApisixClient client = new ApisixClient(apisixConfig);
        client.deleteConsumer(consumerId);
    }

    @Override
    public boolean isConsumerExists(String consumerId, GatewayConfig config) {
        ApisixConfig apisixConfig = config.getApisixConfig();
        ApisixClient client = new ApisixClient(apisixConfig);
        return client.consumerExists(consumerId);
    }

    /**
     * 从 ConsumerCredential 中提取 API Key
     */
    private String extractApiKey(ConsumerCredential credential) {
        if (credential == null || credential.getApiKeyConfig() == null) {
            throw new IllegalArgumentException("API Key credential is required");
        }

        var apiKeyConfig = credential.getApiKeyConfig();
        var credentials = apiKeyConfig.getCredentials();
        if (credentials == null || credentials.isEmpty()) {
            throw new IllegalArgumentException("API Key is required");
        }

        return credentials.get(0).getApiKey();
    }

    /**
     * 构建 APISIX Consumer 配置
     */
    private ApisixConsumer buildApisixConsumer(String consumerId, String apiKey) {
        ApisixConsumer consumer = new ApisixConsumer();
        consumer.setUsername(consumerId);
        consumer.setPlugins(Map.of(
                "key-auth", Map.of("key", apiKey)
        ));
        return consumer;
    }

    @Override
    public ConsumerAuthConfig authorizeConsumer(Gateway gateway, String consumerId, Object refConfig) {
        ApisixRefConfig config = (ApisixRefConfig) refConfig;
        ApisixClient client = getClient(gateway);

        String routeId = config.getRouteId();
        if (routeId == null || routeId.isEmpty()) {
            throw new IllegalArgumentException("Route ID is required for authorization");
        }

        // 获取当前 Route 配置
        ApisixRoute route = client.getRoute(routeId);
        if (route == null) {
            throw new RuntimeException("Route not found: " + routeId);
        }

        // 获取或创建插件配置（始终使用可变 Map，避免来自 JSON / Map.of 的不可变实现）
        Map<String, Object> plugins = route.getPlugins() == null ?
                new HashMap<>() : new HashMap<>(route.getPlugins());

        // 1) 确保 key-auth 插件存在（用于 Consumer API Key 校验）
        plugins.putIfAbsent("key-auth", new HashMap<>());

        // 2) 配置 consumer-restriction 插件（whitelist by consumer_name），实现“订阅级授权”
        @SuppressWarnings("unchecked")
        Map<String, Object> restrictionConfig = plugins.get("consumer-restriction") instanceof Map ?
                new HashMap<>((Map<String, Object>) plugins.get("consumer-restriction")) :
                new HashMap<>();

        restrictionConfig.put("type", "consumer_name");

        Object whitelistObj = restrictionConfig.get("whitelist");
        List<String> whitelist = new ArrayList<>();
        if (whitelistObj instanceof List) {
            for (Object item : (List<?>) whitelistObj) {
                if (item != null) {
                    whitelist.add(String.valueOf(item));
                }
            }
        } else if (whitelistObj instanceof String) {
            whitelist.add((String) whitelistObj);
        }

        if (!whitelist.contains(consumerId)) {
            whitelist.add(consumerId);
        }

        restrictionConfig.put("whitelist", whitelist);
        plugins.put("consumer-restriction", restrictionConfig);

        // 更新 Route
        route.setPlugins(plugins);
        client.updateRoute(routeId, route);

        // 返回授权配置
        return ConsumerAuthConfig.builder()
                .apisixAuthConfig(ApisixAuthConfig.builder().routeId(routeId).build())
                .build();
    }

    @Override
    public void revokeConsumerAuthorization(Gateway gateway, String consumerId, ConsumerAuthConfig authConfig) {
        if (authConfig == null || authConfig.getApisixAuthConfig() == null) {
            return;
        }

        String routeId = authConfig.getApisixAuthConfig().getRouteId();
        if (routeId == null || routeId.isEmpty()) {
            return;
        }

        ApisixClient client = getClient(gateway);
        ApisixRoute route = client.getRoute(routeId);
        if (route == null) {
            return;
        }

        if (route.getPlugins() == null || !route.getPlugins().containsKey("consumer-restriction")) {
            return;
        }

        Map<String, Object> plugins = new HashMap<>(route.getPlugins());

        Object restrictionObj = plugins.get("consumer-restriction");
        if (!(restrictionObj instanceof Map)) {
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> restrictionConfig = new HashMap<>((Map<String, Object>) restrictionObj);

        Object whitelistObj = restrictionConfig.get("whitelist");
        if (!(whitelistObj instanceof List)) {
            return;
        }

        List<String> whitelist = new ArrayList<>();
        for (Object item : (List<?>) whitelistObj) {
            if (item != null) {
                whitelist.add(String.valueOf(item));
            }
        }

        boolean removed = whitelist.removeIf(item -> item != null && item.equals(consumerId));
        if (!removed) {
            return;
        }

        if (whitelist.isEmpty()) {
            plugins.remove("consumer-restriction");
        } else {
            restrictionConfig.put("whitelist", whitelist);
            plugins.put("consumer-restriction", restrictionConfig);
        }

        route.setPlugins(plugins);
        client.updateRoute(routeId, route);
    }

    @Override
    public HttpApiApiInfo fetchAPI(Gateway gateway, String apiId) {
        throw new UnsupportedOperationException("APISIX gateway does not support fetching API");
    }

    @Override
    public String getDashboard(Gateway gateway, String type) {
        throw new UnsupportedOperationException("APISIX gateway does not support getting dashboard");
    }

    @Override
    public List<String> fetchGatewayIps(Gateway gateway) {
        if (gateway == null || gateway.getApisixConfig() == null) {
            return Collections.emptyList();
        }

        ApisixClient client = getClient(gateway);

        try {
            // 通过获取路由列表来验证 Admin API 连接
            List<ApisixRoute> routes = client.listRoutes();
            log.info("APISIX health check passed, found {} routes", routes.size());

            // 返回配置的 Admin API endpoint
            ApisixConfig config = gateway.getApisixConfig();
            if (config != null && config.getAdminApiEndpoint() != null) {
                String endpoint = config.getAdminApiEndpoint();
                // 提取主机地址
                String host = endpoint.replaceFirst("^https?://", "").split(":")[0];
                return Collections.singletonList(host);
            }
        } catch (Exception e) {
            log.warn("APISIX health check failed: {}", e.getMessage());
        }

        return Collections.emptyList();
    }
}
