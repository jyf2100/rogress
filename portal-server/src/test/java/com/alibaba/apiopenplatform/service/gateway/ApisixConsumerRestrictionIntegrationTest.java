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
import com.alibaba.apiopenplatform.service.gateway.client.ApisixClient;
import com.alibaba.apiopenplatform.service.gateway.model.ApisixConsumer;
import com.alibaba.apiopenplatform.service.gateway.model.ApisixRoute;
import com.alibaba.apiopenplatform.support.consumer.ConsumerAuthConfig;
import com.alibaba.apiopenplatform.support.enums.GatewayType;
import com.alibaba.apiopenplatform.support.gateway.ApisixConfig;
import com.alibaba.apiopenplatform.support.product.ApisixRefConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * APISIX 订阅级访问控制集成测试（需要本地启动 APISIX）。
 *
 * 运行方式:
 * 1) export APISIX_ADMIN_ENDPOINT=http://localhost:9180
 * 2) export APISIX_ADMIN_KEY=edd1c9f034335f136f87ad84b625c8f1
 * 3) (可选) export APISIX_PROXY_ENDPOINT=http://localhost:9080
 * 4) mvn test -pl portal-server -Dtest=ApisixConsumerRestrictionIntegrationTest
 */
@EnabledIfEnvironmentVariable(named = "APISIX_ADMIN_ENDPOINT", matches = ".+")
@EnabledIfEnvironmentVariable(named = "APISIX_ADMIN_KEY", matches = ".+")
class ApisixConsumerRestrictionIntegrationTest {

    private static final String ENV_ADMIN_ENDPOINT = "APISIX_ADMIN_ENDPOINT";
    private static final String ENV_ADMIN_KEY = "APISIX_ADMIN_KEY";
    private static final String ENV_PROXY_ENDPOINT = "APISIX_PROXY_ENDPOINT";

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    private int idCounter = 0;

    private String generateTestId() {
        long timestamp = System.currentTimeMillis() % 10000000;
        return String.valueOf(timestamp * 100 + (++idCounter % 100));
    }

    @Test
    void shouldEnforceAndRevokeSubscriptionAccessOnRoute() throws Exception {
        String adminEndpoint = System.getenv(ENV_ADMIN_ENDPOINT);
        String adminKey = System.getenv(ENV_ADMIN_KEY);
        assertNotNull(adminEndpoint);
        assertNotNull(adminKey);

        String proxyEndpoint = System.getenv(ENV_PROXY_ENDPOINT);
        if (proxyEndpoint == null || proxyEndpoint.isBlank()) {
            proxyEndpoint = "http://localhost:9080";
        }

        ApisixConfig config = new ApisixConfig();
        config.setAdminApiEndpoint(adminEndpoint);
        config.setAdminApiKey(adminKey);
        config.setTimeout(30000);

        ApisixClient client = new ApisixClient(config);

        ApisixOperator operator = new ApisixOperator();
        Gateway gateway = new Gateway();
        gateway.setGatewayType(GatewayType.APISIX);
        gateway.setGatewayId("test-gateway");
        gateway.setApisixConfig(config);

        String routeId = generateTestId();
        String testPath = "/consumer-restriction-test-" + routeId;

        String consumer1 = "consumer-" + generateTestId();
        String consumer2 = "consumer-" + generateTestId();
        String key1 = "key-" + generateTestId();
        String key2 = "key-" + generateTestId();

        ApisixRefConfig refConfig = new ApisixRefConfig();
        refConfig.setRouteId(routeId);
        refConfig.setMcpServerName("ignored");

        boolean routeCreated = false;
        boolean consumer1Created = false;
        boolean consumer2Created = false;

        try {
            ApisixRoute route = new ApisixRoute();
            route.setName("consumer restriction e2e");
            route.setUri(testPath);
            route.setPriority(10000);
            route.setMethods(List.of("GET"));
            route.setPlugins(Map.of(
                    "proxy-rewrite", Map.of("uri", "/get")
            ));
            route.setUpstream(Map.of(
                    "type", "roundrobin",
                    "nodes", Map.of("httpbin:8080", 1)
            ));
            client.createRoute(routeId, route);
            routeCreated = true;

            client.createConsumer(consumer1, createConsumer(consumer1, key1));
            consumer1Created = true;
            client.createConsumer(consumer2, createConsumer(consumer2, key2));
            consumer2Created = true;

            ConsumerAuthConfig auth1 = operator.authorizeConsumer(gateway, consumer1, refConfig);
            assertNotNull(auth1);
            assertNotNull(auth1.getApisixAuthConfig());
            assertEquals(routeId, auth1.getApisixAuthConfig().getRouteId());

            assertEventuallyStatus(proxyEndpoint, testPath, key1, 200);
            assertEventuallyStatus(proxyEndpoint, testPath, key2, 403);

            ConsumerAuthConfig auth2 = operator.authorizeConsumer(gateway, consumer2, refConfig);
            assertNotNull(auth2);
            assertNotNull(auth2.getApisixAuthConfig());
            assertEquals(routeId, auth2.getApisixAuthConfig().getRouteId());

            assertEventuallyStatus(proxyEndpoint, testPath, key2, 200);

            operator.revokeConsumerAuthorization(gateway, consumer1, auth1);
            assertEventuallyStatus(proxyEndpoint, testPath, key1, 403);
            assertEventuallyStatus(proxyEndpoint, testPath, key2, 200);

            operator.revokeConsumerAuthorization(gateway, consumer2, auth2);
            assertEventuallyStatus(proxyEndpoint, testPath, key2, 403);
        } finally {
            if (consumer1Created) {
                safeDeleteConsumer(client, consumer1);
            }
            if (consumer2Created) {
                safeDeleteConsumer(client, consumer2);
            }
            if (routeCreated) {
                safeDeleteRoute(client, routeId);
            }
        }
    }

    private static ApisixConsumer createConsumer(String username, String apiKey) {
        ApisixConsumer consumer = new ApisixConsumer();
        consumer.setUsername(username);
        consumer.setPlugins(Map.of(
                "key-auth", Map.of("key", apiKey)
        ));
        consumer.setDesc("Integration test consumer");
        return consumer;
    }

    private static void safeDeleteConsumer(ApisixClient client, String username) {
        try {
            client.deleteConsumer(username);
        } catch (Exception ignored) {
            // ignore cleanup errors
        }
    }

    private static void safeDeleteRoute(ApisixClient client, String routeId) {
        try {
            client.deleteRoute(routeId);
        } catch (Exception ignored) {
            // ignore cleanup errors
        }
    }

    private static void assertEventuallyStatus(String proxyEndpoint, String path, String apiKey, int expectedStatus)
            throws Exception {
        long deadline = System.currentTimeMillis() + 5000;
        int lastStatus = -1;
        while (System.currentTimeMillis() < deadline) {
            lastStatus = requestStatus(proxyEndpoint, path, apiKey);
            if (lastStatus == expectedStatus) {
                return;
            }
            Thread.sleep(200);
        }
        assertEquals(expectedStatus, lastStatus);
    }

    private static int requestStatus(String proxyEndpoint, String path, String apiKey) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(proxyEndpoint + path))
                .timeout(Duration.ofSeconds(3))
                .header("apikey", apiKey)
                .GET()
                .build();

        HttpResponse<Void> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
        return response.statusCode();
    }
}
