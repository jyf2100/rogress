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

package com.alibaba.apiopenplatform.service.gateway.client;

import com.alibaba.apiopenplatform.support.gateway.ApisixConfig;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ApisixClient 单元测试
 *
 * 测试 APISIX Admin API 客户端的基础功能
 */
class ApisixClientTest {

    /**
     * 测试 ApisixClient 类存在且可以实例化
     */
    @Test
    void testApisixClientExists() {
        ApisixConfig config = new ApisixConfig();
        config.setAdminApiEndpoint("http://localhost:9180");
        config.setAdminApiKey("test-api-key");

        ApisixClient client = new ApisixClient(config);

        assertNotNull(client);
    }

    /**
     * 测试 ApisixClient 继承自 GatewayClient
     */
    @Test
    void testApisixClientExtendsGatewayClient() {
        ApisixConfig config = new ApisixConfig();
        config.setAdminApiEndpoint("http://localhost:9180");
        config.setAdminApiKey("test-api-key");

        ApisixClient client = new ApisixClient(config);

        assertTrue(client instanceof GatewayClient);
    }

    /**
     * 测试 ApisixClient 有 close 方法
     */
    @Test
    void testApisixClientHasCloseMethod() {
        ApisixConfig config = new ApisixConfig();
        config.setAdminApiEndpoint("http://localhost:9180");
        config.setAdminApiKey("test-api-key");

        ApisixClient client = new ApisixClient(config);

        // 应该不抛出异常
        assertDoesNotThrow(() -> client.close());
    }

    /**
     * 测试 ApisixClient 构造函数接收 ApisixConfig
     */
    @Test
    void testApisixClientConstructorWithConfig() {
        ApisixConfig config = new ApisixConfig();
        config.setAdminApiEndpoint("http://apisix-admin:9180");
        config.setAdminApiKey("edd1c9f034335f136f87ad84b625c8f1");
        config.setTimeout(60000);

        ApisixClient client = new ApisixClient(config);

        assertNotNull(client);
    }

    // ==================== Task 1.2.2: HTTP 请求能力测试 ====================

    /**
     * 测试 buildUrl 方法 - 基础路径
     */
    @Test
    void testBuildUrl() {
        ApisixConfig config = new ApisixConfig();
        config.setAdminApiEndpoint("http://localhost:9180");
        config.setAdminApiKey("test-api-key");

        ApisixClient client = new ApisixClient(config);

        String url = client.buildUrl("/routes");

        assertEquals("http://localhost:9180/apisix/admin/routes", url);
    }

    /**
     * 测试 buildUrl 方法 - 带尾部斜杠的 endpoint
     */
    @Test
    void testBuildUrlWithTrailingSlash() {
        ApisixConfig config = new ApisixConfig();
        config.setAdminApiEndpoint("http://localhost:9180/");
        config.setAdminApiKey("test-api-key");

        ApisixClient client = new ApisixClient(config);

        String url = client.buildUrl("/routes");

        assertEquals("http://localhost:9180/apisix/admin/routes", url);
    }

    /**
     * 测试 buildUrl 方法 - 不带前导斜杠的 path
     */
    @Test
    void testBuildUrlWithoutLeadingSlash() {
        ApisixConfig config = new ApisixConfig();
        config.setAdminApiEndpoint("http://localhost:9180");
        config.setAdminApiKey("test-api-key");

        ApisixClient client = new ApisixClient(config);

        String url = client.buildUrl("routes");

        assertEquals("http://localhost:9180/apisix/admin/routes", url);
    }

    /**
     * 测试 buildHeaders 方法 - 包含 X-API-KEY
     */
    @Test
    void testBuildHeaders() {
        ApisixConfig config = new ApisixConfig();
        config.setAdminApiEndpoint("http://localhost:9180");
        config.setAdminApiKey("edd1c9f034335f136f87ad84b625c8f1");

        ApisixClient client = new ApisixClient(config);

        HttpHeaders headers = client.buildHeaders();

        assertNotNull(headers);
        assertTrue(headers.containsKey("X-API-KEY"));
        assertEquals("edd1c9f034335f136f87ad84b625c8f1", headers.getFirst("X-API-KEY"));
    }

    /**
     * 测试 buildHeaders 方法 - 包含 Content-Type
     */
    @Test
    void testBuildHeadersContentType() {
        ApisixConfig config = new ApisixConfig();
        config.setAdminApiEndpoint("http://localhost:9180");
        config.setAdminApiKey("test-api-key");

        ApisixClient client = new ApisixClient(config);

        HttpHeaders headers = client.buildHeaders();

        assertEquals(MediaType.APPLICATION_JSON_VALUE, headers.getFirst("Content-Type"));
    }
}
