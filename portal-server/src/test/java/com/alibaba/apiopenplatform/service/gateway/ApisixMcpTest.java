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
import com.alibaba.apiopenplatform.dto.result.mcp.ApisixMCPServerResult;
import com.alibaba.apiopenplatform.dto.result.mcp.GatewayMCPServerResult;
import com.alibaba.apiopenplatform.entity.Gateway;
import com.alibaba.apiopenplatform.service.gateway.client.ApisixClient;
import com.alibaba.apiopenplatform.service.gateway.model.ApisixRoute;
import com.alibaba.apiopenplatform.support.enums.GatewayType;
import com.alibaba.apiopenplatform.support.gateway.ApisixConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * APISIX MCP 功能测试
 *
 * 测试 mcp-bridge 插件配置和 MCP Server 管理
 */
class ApisixMcpTest {

    private ApisixOperator operator;
    private Gateway gateway;

    @BeforeEach
    void setUp() {
        operator = new ApisixOperator();

        // 创建测试网关
        gateway = new Gateway();
        gateway.setGatewayType(GatewayType.APISIX);

        ApisixConfig config = new ApisixConfig();
        config.setAdminApiEndpoint("http://localhost:9180");
        config.setAdminApiKey("test-api-key");
        gateway.setApisixConfig(config);
    }

    /**
     * 测试 ApisixRoute 类存在
     */
    @Test
    void testApisixRouteExists() {
        ApisixRoute route = new ApisixRoute();
        assertNotNull(route);
    }

    /**
     * 测试 ApisixRoute 有 id 字段
     */
    @Test
    void testApisixRouteHasId() {
        ApisixRoute route = new ApisixRoute();
        route.setId("test-route");

        assertEquals("test-route", route.getId());
    }

    /**
     * 测试 ApisixRoute 有 uri 字段
     */
    @Test
    void testApisixRouteHasUri() {
        ApisixRoute route = new ApisixRoute();
        route.setUri("/mcp/filesystem/*");

        assertEquals("/mcp/filesystem/*", route.getUri());
    }

    /**
     * 测试 ApisixRoute 有 plugins 字段
     */
    @Test
    void testApisixRouteHasPlugins() {
        ApisixRoute route = new ApisixRoute();

        Map<String, Object> plugins = Map.of(
            "mcp-bridge", Map.of(
                "command", "/usr/local/bin/mcp-server-filesystem",
                "args", List.of("--root", "/data/files")
            )
        );
        route.setPlugins(plugins);

        assertNotNull(route.getPlugins());
        assertTrue(route.getPlugins().containsKey("mcp-bridge"));
    }

    /**
     * 测试 ApisixMCPServerResult 类存在
     */
    @Test
    void testApisixMCPServerResultExists() {
        ApisixMCPServerResult result = new ApisixMCPServerResult();
        assertNotNull(result);
    }

    /**
     * 测试 ApisixMCPServerResult 继承自 GatewayMCPServerResult
     */
    @Test
    void testApisixMCPServerResultExtendsGatewayMCPServerResult() {
        ApisixMCPServerResult result = new ApisixMCPServerResult();
        assertTrue(result instanceof GatewayMCPServerResult);
    }

    /**
     * 测试 ApisixMCPServerResult 有 mcpServerName 字段
     */
    @Test
    void testApisixMCPServerResultHasMcpServerName() {
        ApisixMCPServerResult result = new ApisixMCPServerResult();
        result.setMcpServerName("filesystem");

        assertEquals("filesystem", result.getMcpServerName());
    }

    /**
     * 测试 ApisixClient 有 listRoutes 方法
     */
    @Test
    void testApisixClientHasListRoutesMethod() {
        ApisixConfig config = new ApisixConfig();
        config.setAdminApiEndpoint("http://localhost:9180");
        config.setAdminApiKey("test-api-key");

        ApisixClient client = new ApisixClient(config);

        // 验证方法存在
        assertDoesNotThrow(() -> {
            client.getClass().getMethod("listRoutes");
        });
    }

    /**
     * 测试 ApisixClient 有 getRoute 方法
     */
    @Test
    void testApisixClientHasGetRouteMethod() {
        ApisixConfig config = new ApisixConfig();
        config.setAdminApiEndpoint("http://localhost:9180");
        config.setAdminApiKey("test-api-key");

        ApisixClient client = new ApisixClient(config);

        // 验证方法存在
        assertDoesNotThrow(() -> {
            client.getClass().getMethod("getRoute", String.class);
        });
    }

    /**
     * 测试 fetchMcpServers 方法不抛出 UnsupportedOperationException
     */
    @Test
    void testFetchMcpServersNotThrowUnsupportedOperationException() {
        // 此测试在没有实际 APISIX 服务时会因连接失败而抛出异常
        // 但我们验证的是方法不再抛出 UnsupportedOperationException
        try {
            operator.fetchMcpServers(gateway, 1, 10);
        } catch (UnsupportedOperationException e) {
            fail("fetchMcpServers should not throw UnsupportedOperationException");
        } catch (Exception e) {
            // 其他异常（如连接失败、认证失败）是可以接受的
            String message = e.getMessage();
            assertTrue(message == null ||
                       message.contains("Connection refused") ||
                       message.contains("connect") ||
                       message.contains("Failed") ||
                       message.contains("401") ||
                       message.contains("Unauthorized") ||
                       message.contains("wrong apikey"));
        }
    }
}
