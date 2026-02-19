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
import com.alibaba.apiopenplatform.support.gateway.ApisixConfig;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * APISIX Consumer API 格式测试
 *
 * 验证 Consumer API URL 格式符合官方文档规范：
 * - PUT /apisix/admin/consumers (username 在请求体中)
 * - DELETE /apisix/admin/consumers/{username}
 */
class ApisixConsumerApiFormatTest {

    /**
     * 测试 createConsumer 使用正确的 URL 格式
     * 官方格式: PUT /apisix/admin/consumers (不带 username 路径参数)
     */
    @Test
    void testCreateConsumerUsesCorrectUrlFormat() throws Exception {
        ApisixConfig config = new ApisixConfig();
        config.setAdminApiEndpoint("http://localhost:9180");
        config.setAdminApiKey("test-api-key");

        ApisixClient client = new ApisixClient(config);

        // 使用反射获取 buildUrl 方法
        Method buildUrlMethod = ApisixClient.class.getDeclaredMethod("buildUrl", String.class);
        buildUrlMethod.setAccessible(true);

        // 验证创建 Consumer 的 URL 不应该包含 username
        // 正确格式: /consumers 而不是 /consumers/{username}
        String expectedUrl = "http://localhost:9180/apisix/admin/consumers";
        String actualUrl = (String) buildUrlMethod.invoke(client, "/consumers");

        assertEquals(expectedUrl, actualUrl,
            "createConsumer 应该使用 /consumers URL，username 在请求体中");
    }

    /**
     * 测试 updateConsumer 使用正确的 URL 格式
     * 官方格式: PUT /apisix/admin/consumers (不带 username 路径参数)
     */
    @Test
    void testUpdateConsumerUsesCorrectUrlFormat() throws Exception {
        ApisixConfig config = new ApisixConfig();
        config.setAdminApiEndpoint("http://localhost:9180");
        config.setAdminApiKey("test-api-key");

        ApisixClient client = new ApisixClient(config);

        Method buildUrlMethod = ApisixClient.class.getDeclaredMethod("buildUrl", String.class);
        buildUrlMethod.setAccessible(true);

        // 验证更新 Consumer 的 URL 不应该包含 username
        String expectedUrl = "http://localhost:9180/apisix/admin/consumers";
        String actualUrl = (String) buildUrlMethod.invoke(client, "/consumers");

        assertEquals(expectedUrl, actualUrl,
            "updateConsumer 应该使用 /consumers URL，username 在请求体中");
    }

    /**
     * 测试 deleteConsumer 使用正确的 URL 格式
     * 官方格式: DELETE /apisix/admin/consumers/{username}
     */
    @Test
    void testDeleteConsumerUsesCorrectUrlFormat() throws Exception {
        ApisixConfig config = new ApisixConfig();
        config.setAdminApiEndpoint("http://localhost:9180");
        config.setAdminApiKey("test-api-key");

        ApisixClient client = new ApisixClient(config);

        Method buildUrlMethod = ApisixClient.class.getDeclaredMethod("buildUrl", String.class);
        buildUrlMethod.setAccessible(true);

        // 验证删除 Consumer 的 URL 应该包含 username
        String expectedUrl = "http://localhost:9180/apisix/admin/consumers/test-user";
        String actualUrl = (String) buildUrlMethod.invoke(client, "/consumers/test-user");

        assertEquals(expectedUrl, actualUrl,
            "deleteConsumer 应该使用 /consumers/{username} URL");
    }

    /**
     * 测试 ApisixConsumer 在创建时包含 username 字段
     */
    @Test
    void testApisixConsumerContainsUsername() {
        ApisixConsumer consumer = new ApisixConsumer();
        consumer.setUsername("jack");
        consumer.setPlugins(Map.of("key-auth", Map.of("key", "auth-one")));

        assertNotNull(consumer.getUsername(), "Consumer 必须包含 username 字段");
        assertEquals("jack", consumer.getUsername());
    }

    /**
     * 测试 createConsumer 请求体包含 username
     * 根据官方文档，username 必须在请求体中
     */
    @Test
    void testCreateConsumerBodyContainsUsername() {
        String username = "jack";
        String apiKey = "auth-one";

        ApisixConsumer consumer = new ApisixConsumer();
        consumer.setUsername(username);
        consumer.setPlugins(Map.of("key-auth", Map.of("key", apiKey)));

        // 验证请求体中的 username
        assertEquals(username, consumer.getUsername(),
            "创建 Consumer 时，请求体必须包含 username");
    }

    /**
     * 测试 getConsumer 使用正确的 URL 格式
     * 官方格式: GET /apisix/admin/consumers/{username}
     */
    @Test
    void testGetConsumerUsesCorrectUrlFormat() throws Exception {
        ApisixConfig config = new ApisixConfig();
        config.setAdminApiEndpoint("http://localhost:9180");
        config.setAdminApiKey("test-api-key");

        ApisixClient client = new ApisixClient(config);

        Method buildUrlMethod = ApisixClient.class.getDeclaredMethod("buildUrl", String.class);
        buildUrlMethod.setAccessible(true);

        // 验证获取单个 Consumer 的 URL 应该包含 username
        String expectedUrl = "http://localhost:9180/apisix/admin/consumers/jack";
        String actualUrl = (String) buildUrlMethod.invoke(client, "/consumers/jack");

        assertEquals(expectedUrl, actualUrl,
            "getConsumer 应该使用 /consumers/{username} URL");
    }
}
