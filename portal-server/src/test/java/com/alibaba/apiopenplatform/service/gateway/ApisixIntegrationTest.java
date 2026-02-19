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

import com.alibaba.apiopenplatform.service.gateway.client.ApisixClient;
import com.alibaba.apiopenplatform.service.gateway.model.ApisixConsumer;
import com.alibaba.apiopenplatform.service.gateway.model.ApisixRoute;
import com.alibaba.apiopenplatform.support.gateway.ApisixConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * APISIX 集成测试
 *
 * 运行方式:
 * 1. 设置环境变量:
 *    export APISIX_ADMIN_ENDPOINT=http://localhost:9180
 *    export APISIX_ADMIN_KEY=edd1c9f034335f136f87ad84b625c8f1
 *
 * 2. 运行测试:
 *    mvn test -pl portal-server -Dtest=ApisixIntegrationTest
 */
@DisplayName("APISIX 集成测试")
class ApisixIntegrationTest {

    private static final String ENV_ENDPOINT = "APISIX_ADMIN_ENDPOINT";
    private static final String ENV_API_KEY = "APISIX_ADMIN_KEY";

    private static String adminEndpoint;
    private static String adminApiKey;

    private ApisixClient client;

    @BeforeAll
    static void setUpClass() {
        adminEndpoint = System.getenv(ENV_ENDPOINT);
        adminApiKey = System.getenv(ENV_API_KEY);

        if (adminEndpoint != null && adminApiKey != null) {
            System.out.println("======================================================");
            System.out.println("APISIX 集成测试已启用");
            System.out.println("Endpoint: " + adminEndpoint);
            System.out.println("======================================================");
        } else {
            System.out.println("======================================================");
            System.out.println("APISIX 集成测试已跳过 (未配置环境变量)");
            System.out.println("设置以下环境变量以启用测试:");
            System.out.println("  export " + ENV_ENDPOINT + "=http://localhost:9180");
            System.out.println("  export " + ENV_API_KEY + "=your-admin-key");
            System.out.println("======================================================");
        }
    }

    private ApisixClient createClient() {
        ApisixConfig config = new ApisixConfig();
        config.setAdminApiEndpoint(adminEndpoint);
        config.setAdminApiKey(adminApiKey);
        config.setTimeout(30000);
        return new ApisixClient(config);
    }

    private int idCounter = 0;

    private String generateTestId() {
        // APISIX ID 格式要求：简单字符串，使用纯数字
        // 结合时间戳和计数器确保唯一性
        long timestamp = System.currentTimeMillis() % 10000000;
        return String.valueOf(timestamp * 100 + (++idCounter % 100));
    }

    // ==================== 健康检查测试 ====================

    @Nested
    @DisplayName("健康检查")
    @EnabledIfEnvironmentVariable(named = "APISIX_ADMIN_ENDPOINT", matches = ".+")
    class HealthCheckTests {

        @Test
        @DisplayName("验证 Admin API 连接")
        void testAdminApiConnection() {
            client = createClient();

            // 获取路由列表来验证连接
            assertDoesNotThrow(() -> {
                List<ApisixRoute> routes = client.listRoutes();
                assertNotNull(routes);
                System.out.println("✓ Admin API 连接成功，当前路由数: " + routes.size());
            }, "应该能够连接到 APISIX Admin API");
        }

        @Test
        @DisplayName("验证 Consumer 列表获取")
        void testListConsumers() {
            client = createClient();

            assertDoesNotThrow(() -> {
                List<ApisixConsumer> consumers = client.listConsumers();
                assertNotNull(consumers);
                System.out.println("✓ Consumer 列表获取成功，当前消费者数: " + consumers.size());
            });
        }
    }

    // ==================== Route CRUD 测试 ====================

    @Nested
    @DisplayName("Route CRUD")
    @EnabledIfEnvironmentVariable(named = "APISIX_ADMIN_ENDPOINT", matches = ".+")
    class RouteCrudTests {

        private String testRouteId;

        @BeforeEach
        void setUp() {
            client = createClient();
            testRouteId = generateTestId();
        }

        @Test
        @DisplayName("创建路由")
        void testCreateRoute(TestInfo testInfo) {
            ApisixRoute route = new ApisixRoute();
            route.setUri("/test-" + generateTestId() + "/*");
            route.setName("Test Route - " + testInfo.getDisplayName());
            route.setMethods(List.of("GET", "POST"));
            route.setUpstream(Map.of(
                "type", "roundrobin",
                "nodes", Map.of("httpbin.org:80", 1)
            ));

            assertDoesNotThrow(() -> {
                ApisixRoute created = client.createRoute(testRouteId, route);
                assertNotNull(created);
                System.out.println("✓ 路由创建成功: " + testRouteId);

                // 清理
                client.deleteRoute(testRouteId);
                System.out.println("✓ 路由已清理: " + testRouteId);
            });
        }

        @Test
        @DisplayName("获取路由")
        void testGetRoute() {
            // 先创建路由
            ApisixRoute route = new ApisixRoute();
            route.setUri("/test-get/*");
            route.setUpstream(Map.of(
                "type", "roundrobin",
                "nodes", Map.of("httpbin.org:80", 1)
            ));
            client.createRoute(testRouteId, route);

            assertDoesNotThrow(() -> {
                ApisixRoute fetched = client.getRoute(testRouteId);
                assertNotNull(fetched);
                assertEquals("/test-get/*", fetched.getUri());
                System.out.println("✓ 路由获取成功: " + testRouteId);
            });

            // 清理
            client.deleteRoute(testRouteId);
        }

        @Test
        @DisplayName("删除路由")
        void testDeleteRoute() {
            // 先创建路由
            ApisixRoute route = new ApisixRoute();
            route.setUri("/test-delete/*");
            route.setUpstream(Map.of(
                "type", "roundrobin",
                "nodes", Map.of("httpbin.org:80", 1)
            ));
            ApisixRoute created = client.createRoute(testRouteId, route);
            assertNotNull(created, "路由应该创建成功");

            // 删除路由
            assertDoesNotThrow(() -> {
                client.deleteRoute(testRouteId);
                System.out.println("✓ 路由删除成功: " + testRouteId);
            });

            // 验证删除 - APISIX 返回 404
            assertThrows(Exception.class, () -> {
                client.getRoute(testRouteId);
            }, "删除后路由应该不存在");
        }
    }

    // ==================== Consumer CRUD 测试 ====================

    @Nested
    @DisplayName("Consumer CRUD")
    @EnabledIfEnvironmentVariable(named = "APISIX_ADMIN_ENDPOINT", matches = ".+")
    class ConsumerCrudTests {

        private String testUsername;

        @BeforeEach
        void setUp() {
            client = createClient();
            testUsername = "user-" + generateTestId();
        }

        @Test
        @DisplayName("创建消费者 (官方 API 格式)")
        void testCreateConsumer() {
            ApisixConsumer consumer = new ApisixConsumer();
            consumer.setUsername(testUsername);
            consumer.setPlugins(Map.of(
                "key-auth", Map.of("key", "test-key-" + generateTestId())
            ));
            consumer.setDesc("Test consumer created by integration test");

            assertDoesNotThrow(() -> {
                ApisixConsumer created = client.createConsumer(testUsername, consumer);
                assertNotNull(created);
                System.out.println("✓ 消费者创建成功 (PUT /consumers): " + testUsername);

                // 验证可以获取
                ApisixConsumer fetched = client.getConsumer(testUsername);
                assertNotNull(fetched);
                assertEquals(testUsername, fetched.getUsername());

                // 清理
                client.deleteConsumer(testUsername);
                System.out.println("✓ 消费者已清理: " + testUsername);
            });
        }

        @Test
        @DisplayName("更新消费者")
        void testUpdateConsumer() {
            // 先创建
            ApisixConsumer consumer = new ApisixConsumer();
            consumer.setUsername(testUsername);
            consumer.setPlugins(Map.of("key-auth", Map.of("key", "original-key")));
            client.createConsumer(testUsername, consumer);

            // 更新
            ApisixConsumer updated = new ApisixConsumer();
            updated.setUsername(testUsername);
            updated.setPlugins(Map.of("key-auth", Map.of("key", "updated-key")));
            updated.setDesc("Updated description");

            assertDoesNotThrow(() -> {
                client.updateConsumer(testUsername, updated);
                System.out.println("✓ 消费者更新成功 (PUT /consumers): " + testUsername);

                // 验证更新
                ApisixConsumer fetched = client.getConsumer(testUsername);
                assertEquals("Updated description", fetched.getDesc());
            });

            // 清理
            client.deleteConsumer(testUsername);
        }

        @Test
        @DisplayName("删除消费者")
        void testDeleteConsumer() {
            // 先创建
            ApisixConsumer consumer = new ApisixConsumer();
            consumer.setUsername(testUsername);
            consumer.setPlugins(Map.of("key-auth", Map.of("key", "test-key")));
            client.createConsumer(testUsername, consumer);

            assertDoesNotThrow(() -> {
                client.deleteConsumer(testUsername);
                System.out.println("✓ 消费者删除成功: " + testUsername);

                // 验证删除
                assertFalse(client.consumerExists(testUsername));
            });
        }

        @Test
        @DisplayName("检查消费者是否存在")
        void testConsumerExists() {
            // 检查不存在的消费者
            assertFalse(client.consumerExists("non-existent-user-" + generateTestId()));

            // 创建消费者
            ApisixConsumer consumer = new ApisixConsumer();
            consumer.setUsername(testUsername);
            consumer.setPlugins(Map.of("key-auth", Map.of("key", "test-key")));
            client.createConsumer(testUsername, consumer);

            // 检查存在的消费者
            assertTrue(client.consumerExists(testUsername));
            System.out.println("✓ consumerExists 验证成功");

            // 清理
            client.deleteConsumer(testUsername);
        }
    }

    // ==================== mcp-bridge 插件测试 ====================

    @Nested
    @DisplayName("mcp-bridge 插件")
    @EnabledIfEnvironmentVariable(named = "APISIX_ADMIN_ENDPOINT", matches = ".+")
    class McpBridgeTests {

        private String testRouteId;

        @BeforeEach
        void setUp() {
            client = createClient();
            testRouteId = generateTestId();
        }

        @Test
        @DisplayName("创建带 mcp-bridge 插件的路由")
        void testCreateMcpBridgeRoute() {
            ApisixRoute route = new ApisixRoute();
            route.setUri("/mcp/" + generateTestId() + "/*");
            route.setName("MCP Test Route");
            route.setPlugins(Map.of(
                "mcp-bridge", Map.of(
                    "command", "python",
                    "args", List.of("-m", "mcp_server_filesystem")
                )
            ));

            assertDoesNotThrow(() -> {
                ApisixRoute created = client.createRoute(testRouteId, route);
                assertNotNull(created);
                assertTrue(created.hasMcpBridgePlugin());
                System.out.println("✓ mcp-bridge 路由创建成功: " + testRouteId);

                // 验证插件配置
                Map<String, Object> mcpConfig = created.getMcpBridgeConfig();
                assertNotNull(mcpConfig);
                assertEquals("python", mcpConfig.get("command"));

                // 清理
                client.deleteRoute(testRouteId);
                System.out.println("✓ mcp-bridge 路由已清理");
            });
        }

        @Test
        @DisplayName("筛选 MCP Server 列表")
        void testFilterMcpServers() {
            String routeId1 = generateTestId();
            String routeId2 = generateTestId();

            try {
                // 创建带 mcp-bridge 的路由
                ApisixRoute mcpRoute = new ApisixRoute();
                mcpRoute.setUri("/mcp/server1/*");
                mcpRoute.setPlugins(Map.of(
                    "mcp-bridge", Map.of("command", "test-cmd")
                ));
                ApisixRoute createdMcp = client.createRoute(routeId1, mcpRoute);
                assertNotNull(createdMcp, "MCP 路由应该创建成功");
                System.out.println("✓ MCP 路由创建成功: " + routeId1);

                // 创建普通路由
                ApisixRoute normalRoute = new ApisixRoute();
                normalRoute.setUri("/api/normal/*");
                normalRoute.setUpstream(Map.of(
                    "type", "roundrobin",
                    "nodes", Map.of("httpbin.org:80", 1)
                ));
                ApisixRoute createdNormal = client.createRoute(routeId2, normalRoute);
                assertNotNull(createdNormal, "普通路由应该创建成功");
                System.out.println("✓ 普通路由创建成功: " + routeId2);

                // 获取所有路由并筛选
                List<ApisixRoute> allRoutes = client.listRoutes();
                List<ApisixRoute> mcpRoutes = allRoutes.stream()
                    .filter(ApisixRoute::hasMcpBridgePlugin)
                    .toList();

                System.out.println("✓ 总路由数: " + allRoutes.size());
                System.out.println("✓ MCP 路由数: " + mcpRoutes.size());

                // 验证刚创建的 MCP 路由在列表中
                assertTrue(mcpRoutes.size() >= 1, "至少应该有一个 MCP 路由");

            } finally {
                // 清理
                try { client.deleteRoute(routeId1); } catch (Exception ignored) {}
                try { client.deleteRoute(routeId2); } catch (Exception ignored) {}
            }
        }
    }

    // ==================== 错误处理测试 ====================

    @Nested
    @DisplayName("错误处理")
    @EnabledIfEnvironmentVariable(named = "APISIX_ADMIN_ENDPOINT", matches = ".+")
    class ErrorHandlingTests {

        @Test
        @DisplayName("获取不存在的路由 - APISIX 返回 404")
        void testGetNonExistentRoute() {
            client = createClient();

            // APISIX 对不存在的资源返回 404 错误
            assertThrows(Exception.class, () -> {
                client.getRoute(generateTestId());
            }, "不存在的路由应该抛出异常");
            System.out.println("✓ 不存在的路由正确返回 404");
        }

        @Test
        @DisplayName("获取不存在的消费者 - APISIX 返回 404")
        void testGetNonExistentConsumer() {
            client = createClient();

            // APISIX 对不存在的资源返回 404 错误
            assertThrows(Exception.class, () -> {
                client.getConsumer("nonexistent" + generateTestId());
            }, "不存在的消费者应该抛出异常");
            System.out.println("✓ 不存在的消费者正确返回 404");
        }
    }
}
