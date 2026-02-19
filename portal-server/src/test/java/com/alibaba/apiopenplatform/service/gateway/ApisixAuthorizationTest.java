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

import com.alibaba.apiopenplatform.entity.Gateway;
import com.alibaba.apiopenplatform.service.gateway.model.ApisixRoute;
import com.alibaba.apiopenplatform.support.consumer.ConsumerAuthConfig;
import com.alibaba.apiopenplatform.support.enums.GatewayType;
import com.alibaba.apiopenplatform.support.gateway.ApisixConfig;
import com.alibaba.apiopenplatform.support.product.ApisixRefConfig;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * APISIX Consumer 授权测试
 */
class ApisixAuthorizationTest {

    /**
     * 测试 ApisixRefConfig 类存在
     */
    @Test
    void testApisixRefConfigExists() {
        ApisixRefConfig config = new ApisixRefConfig();
        assertNotNull(config);
    }

    /**
     * 测试 ApisixRefConfig 有 routeId 字段
     */
    @Test
    void testApisixRefConfigHasRouteId() {
        ApisixRefConfig config = new ApisixRefConfig();
        config.setRouteId("test-route-id");

        assertEquals("test-route-id", config.getRouteId());
    }

    /**
     * 测试 ApisixRefConfig 有 mcpServerName 字段
     */
    @Test
    void testApisixRefConfigHasMcpServerName() {
        ApisixRefConfig config = new ApisixRefConfig();
        config.setMcpServerName("filesystem-mcp");

        assertEquals("filesystem-mcp", config.getMcpServerName());
    }

    /**
     * 测试 authorizeConsumer 方法存在且不抛出 UnsupportedOperationException
     */
    @Test
    void testAuthorizeConsumerMethodExists() {
        ApisixOperator operator = new ApisixOperator();

        Gateway gateway = createTestGateway();
        ApisixRefConfig refConfig = new ApisixRefConfig();
        refConfig.setRouteId("test-route");
        refConfig.setMcpServerName("test-mcp");

        try {
            operator.authorizeConsumer(gateway, "test-consumer", refConfig);
        } catch (UnsupportedOperationException e) {
            fail("authorizeConsumer should not throw UnsupportedOperationException");
        } catch (Exception e) {
            // 其他异常（如连接失败）是可以接受的
            assertTrue(e.getMessage().contains("Connection refused") ||
                       e.getMessage().contains("connect") ||
                       e.getMessage().contains("Failed") ||
                       e instanceof RuntimeException);
        }
    }

    /**
     * 测试 revokeConsumerAuthorization 方法存在且不抛出 UnsupportedOperationException
     */
    @Test
    void testRevokeConsumerAuthorizationMethodExists() {
        ApisixOperator operator = new ApisixOperator();

        Gateway gateway = createTestGateway();

        // 创建一个简单的授权配置
        ConsumerAuthConfig authConfig = ConsumerAuthConfig.builder()
                .build();

        try {
            operator.revokeConsumerAuthorization(gateway, "test-consumer", authConfig);
        } catch (UnsupportedOperationException e) {
            fail("revokeConsumerAuthorization should not throw UnsupportedOperationException");
        } catch (Exception e) {
            // 其他异常（如连接失败）是可以接受的
            assertTrue(e.getMessage() == null ||
                       e.getMessage().contains("Connection refused") ||
                       e.getMessage().contains("connect") ||
                       e.getMessage().contains("Failed") ||
                       e instanceof RuntimeException);
        }
    }

    /**
     * 测试 ApisixClient 有 updateRoute 方法
     */
    @Test
    void testApisixClientHasUpdateRouteMethod() {
        ApisixConfig config = new ApisixConfig();
        config.setAdminApiEndpoint("http://localhost:9180");
        config.setAdminApiKey("test-api-key");

        com.alibaba.apiopenplatform.service.gateway.client.ApisixClient client =
            new com.alibaba.apiopenplatform.service.gateway.client.ApisixClient(config);

        assertDoesNotThrow(() -> {
            client.getClass().getMethod("updateRoute", String.class, ApisixRoute.class);
        });
    }

    /**
     * 测试 ApisixRoute 有 setPlugins 方法
     */
    @Test
    void testApisixRouteHasSetPlugins() {
        ApisixRoute route = new ApisixRoute();
        route.setPlugins(Map.of("key-auth", Map.of("key", "test")));

        assertNotNull(route.getPlugins());
        assertTrue(route.getPlugins().containsKey("key-auth"));
    }

    /**
     * 测试健康检查 - fetchGatewayIps 方法
     */
    @Test
    void testFetchGatewayIps() {
        ApisixOperator operator = new ApisixOperator();
        Gateway gateway = createTestGateway();

        try {
            List<String> ips = operator.fetchGatewayIps(gateway);
            // 即使没有真实 APISIX，也不应该抛出 UnsupportedOperationException
            assertNotNull(ips);
        } catch (UnsupportedOperationException e) {
            fail("fetchGatewayIps should not throw UnsupportedOperationException");
        } catch (Exception e) {
            // 网络异常可以接受
            assertTrue(e instanceof RuntimeException);
        }
    }

    private Gateway createTestGateway() {
        Gateway gateway = new Gateway();
        gateway.setGatewayId("test-gateway-id");
        gateway.setGatewayType(GatewayType.APISIX);

        ApisixConfig apisixConfig = new ApisixConfig();
        apisixConfig.setAdminApiEndpoint("http://localhost:9180");
        apisixConfig.setAdminApiKey("test-api-key");

        gateway.setApisixConfig(apisixConfig);

        return gateway;
    }
}
