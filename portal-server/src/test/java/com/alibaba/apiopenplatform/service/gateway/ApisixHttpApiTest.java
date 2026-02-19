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

import com.alibaba.apiopenplatform.dto.result.common.PageResult;
import com.alibaba.apiopenplatform.dto.result.httpapi.APIResult;
import com.alibaba.apiopenplatform.dto.result.httpapi.ApisixHttpApiResult;
import com.alibaba.apiopenplatform.entity.Gateway;
import com.alibaba.apiopenplatform.service.gateway.client.ApisixClient;
import com.alibaba.apiopenplatform.service.gateway.client.GatewayClient;
import com.alibaba.apiopenplatform.service.gateway.model.ApisixRoute;
import com.alibaba.apiopenplatform.support.enums.GatewayType;
import com.alibaba.apiopenplatform.support.gateway.ApisixConfig;
import com.alibaba.apiopenplatform.support.product.ApisixRefConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * APISIX HTTP API 功能测试
 */
@ExtendWith(MockitoExtension.class)
class ApisixHttpApiTest {

    @Mock
    private ApisixClient client;

    private ApisixOperator operator;
    private Gateway gateway;

    @BeforeEach
    void setUp() {
        operator = new ApisixOperator();
        gateway = createGateway();

        // 使用反射直接设置 clientCache
        Map<String, GatewayClient> clientCache = new ConcurrentHashMap<>();
        String clientKey = gateway.getApisixConfig().buildUniqueKey();
        clientCache.put(clientKey, client);
        ReflectionTestUtils.setField(operator, "clientCache", clientCache);
    }

    private Gateway createGateway() {
        Gateway gw = new Gateway();
        gw.setGatewayId("test-gateway-id");
        gw.setGatewayType(GatewayType.APISIX);
        ApisixConfig config = new ApisixConfig();
        config.setAdminApiEndpoint("http://localhost:9180");
        config.setAdminApiKey("test-key");
        gw.setApisixConfig(config);
        return gw;
    }

    private ApisixRoute createRoute(String id, String name, Map<String, Object> plugins) {
        ApisixRoute route = new ApisixRoute();
        route.setId(id);
        route.setName(name);
        route.setUri("/api/" + id + "/*");
        route.setMethods(List.of("GET", "POST"));
        route.setPlugins(plugins);
        route.setStatus(true);
        return route;
    }

    // ========================================
    // fetchHTTPAPIs Tests
    // ========================================

    @Test
    void shouldFetchHttpApisExcludingMcpAndModelRoutes() {
        // Given - 混合类型的路由
        ApisixRoute httpRoute = createRoute("http-route", "HTTP API", null);
        ApisixRoute mcpRoute = createRoute("mcp-route", "MCP Server",
                Map.of("mcp-bridge", Map.of("command", "test")));
        ApisixRoute modelRoute = createRoute("model-route", "Model API",
                Map.of("ai-proxy", Map.of("provider", "openai")));
        ApisixRoute httpRoute2 = createRoute("http-route-2", "HTTP API 2", Collections.emptyMap());

        when(client.listRoutes()).thenReturn(List.of(httpRoute, mcpRoute, modelRoute, httpRoute2));

        // When
        PageResult<APIResult> result = operator.fetchHTTPAPIs(gateway, 1, 10);

        // Then - 只返回 HTTP 路由，排除 MCP 和 Model
        assertEquals(2, result.getContent().size());
        assertEquals("http-route", result.getContent().get(0).getApiId());
        assertEquals("http-route-2", result.getContent().get(1).getApiId());
        assertEquals(2, result.getTotalElements());
    }

    @Test
    void shouldReturnEmptyListWhenNoHttpRoutes() {
        // Given - 只有 MCP 和 Model 路由
        ApisixRoute mcpRoute = createRoute("mcp-route", "MCP Server",
                Map.of("mcp-bridge", Map.of()));
        ApisixRoute modelRoute = createRoute("model-route", "Model API",
                Map.of("ai-proxy", Map.of()));

        when(client.listRoutes()).thenReturn(List.of(mcpRoute, modelRoute));

        // When
        PageResult<APIResult> result = operator.fetchHTTPAPIs(gateway, 1, 10);

        // Then
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void shouldPaginateHttpApis() {
        // Given - 25 个 HTTP 路由
        List<ApisixRoute> routes = new java.util.ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            routes.add(createRoute("route-" + i, "API " + i, null));
        }
        when(client.listRoutes()).thenReturn(routes);

        // When - 第 2 页，每页 10 条
        PageResult<APIResult> result = operator.fetchHTTPAPIs(gateway, 2, 10);

        // Then
        assertEquals(10, result.getContent().size());
        assertEquals("route-11", result.getContent().get(0).getApiId());
        assertEquals(25, result.getTotalElements());
        assertEquals(2, result.getNumber());
    }

    @Test
    void shouldReturnEmptyWhenPageExceedsTotal() {
        // Given - 只有 5 个路由
        List<ApisixRoute> routes = new java.util.ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            routes.add(createRoute("route-" + i, "API " + i, null));
        }
        when(client.listRoutes()).thenReturn(routes);

        // When - 请求第 10 页
        PageResult<APIResult> result = operator.fetchHTTPAPIs(gateway, 10, 10);

        // Then
        assertTrue(result.getContent().isEmpty());
        assertEquals(5, result.getTotalElements());
    }

    // ========================================
    // fetchRESTAPIs Tests
    // ========================================

    @Test
    void fetchRestApisShouldReturnSameAsFetchHttpApis() {
        // Given
        ApisixRoute httpRoute = createRoute("http-route", "HTTP API", null);
        when(client.listRoutes()).thenReturn(List.of(httpRoute));

        // When
        PageResult<APIResult> httpResult = operator.fetchHTTPAPIs(gateway, 1, 10);
        PageResult<APIResult> restResult = operator.fetchRESTAPIs(gateway, 1, 10);

        // Then
        assertEquals(httpResult.getContent().size(), restResult.getContent().size());
        assertEquals(httpResult.getTotalElements(), restResult.getTotalElements());
    }

    // ========================================
    // fetchAPIConfig Tests
    // ========================================

    @Test
    void shouldFetchApiConfig() {
        // Given
        ApisixRoute route = createRoute("test-route", "Test API", null);
        route.setUri("/api/v1/test/*");
        route.setMethods(List.of("GET", "POST", "PUT"));

        when(client.getRoute("test-route")).thenReturn(route);

        ApisixRefConfig refConfig = new ApisixRefConfig();
        refConfig.setRouteId("test-route");

        // When
        String config = operator.fetchAPIConfig(gateway, refConfig);

        // Then
        assertNotNull(config);
        assertTrue(config.contains("test-route"));
        assertTrue(config.contains("Test API"));
    }

    @Test
    void shouldThrowExceptionWhenRouteNotFound() {
        // Given
        when(client.getRoute("non-existent")).thenReturn(null);

        ApisixRefConfig refConfig = new ApisixRefConfig();
        refConfig.setRouteId("non-existent");

        // When & Then
        assertThrows(RuntimeException.class, () ->
                operator.fetchAPIConfig(gateway, refConfig));
    }
}
