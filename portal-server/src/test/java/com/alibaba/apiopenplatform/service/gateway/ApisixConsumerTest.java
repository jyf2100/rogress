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

import com.alibaba.apiopenplatform.entity.Consumer;
import com.alibaba.apiopenplatform.entity.ConsumerCredential;
import com.alibaba.apiopenplatform.service.gateway.client.ApisixClient;
import com.alibaba.apiopenplatform.service.gateway.model.ApisixConsumer;
import com.alibaba.apiopenplatform.support.enums.GatewayType;
import com.alibaba.apiopenplatform.support.gateway.ApisixConfig;
import com.alibaba.apiopenplatform.support.gateway.GatewayConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * APISIX Consumer 管理测试
 */
class ApisixConsumerTest {

    /**
     * 测试 ApisixConsumer 类存在
     */
    @Test
    void testApisixConsumerExists() {
        ApisixConsumer consumer = new ApisixConsumer();
        assertNotNull(consumer);
    }

    /**
     * 测试 ApisixConsumer 有 username 字段
     */
    @Test
    void testApisixConsumerHasUsername() {
        ApisixConsumer consumer = new ApisixConsumer();
        consumer.setUsername("test-user");

        assertEquals("test-user", consumer.getUsername());
    }

    /**
     * 测试 ApisixConsumer 有 plugins 字段
     */
    @Test
    void testApisixConsumerHasPlugins() {
        ApisixConsumer consumer = new ApisixConsumer();
        consumer.setPlugins(java.util.Map.of(
            "key-auth", java.util.Map.of("key", "test-key")
        ));

        assertNotNull(consumer.getPlugins());
        assertTrue(consumer.getPlugins().containsKey("key-auth"));
    }

    /**
     * 测试 ApisixClient 有 listConsumers 方法
     */
    @Test
    void testApisixClientHasListConsumersMethod() {
        ApisixConfig config = new ApisixConfig();
        config.setAdminApiEndpoint("http://localhost:9180");
        config.setAdminApiKey("test-api-key");

        ApisixClient client = new ApisixClient(config);

        assertDoesNotThrow(() -> {
            client.getClass().getMethod("listConsumers");
        });
    }

    /**
     * 测试 ApisixClient 有 getConsumer 方法
     */
    @Test
    void testApisixClientHasGetConsumerMethod() {
        ApisixConfig config = new ApisixConfig();
        config.setAdminApiEndpoint("http://localhost:9180");
        config.setAdminApiKey("test-api-key");

        ApisixClient client = new ApisixClient(config);

        assertDoesNotThrow(() -> {
            client.getClass().getMethod("getConsumer", String.class);
        });
    }

    /**
     * 测试 ApisixClient 有 createConsumer 方法
     */
    @Test
    void testApisixClientHasCreateConsumerMethod() {
        ApisixConfig config = new ApisixConfig();
        config.setAdminApiEndpoint("http://localhost:9180");
        config.setAdminApiKey("test-api-key");

        ApisixClient client = new ApisixClient(config);

        assertDoesNotThrow(() -> {
            client.getClass().getMethod("createConsumer", String.class, ApisixConsumer.class);
        });
    }

    /**
     * 测试 ApisixClient 有 deleteConsumer 方法
     */
    @Test
    void testApisixClientHasDeleteConsumerMethod() {
        ApisixConfig config = new ApisixConfig();
        config.setAdminApiEndpoint("http://localhost:9180");
        config.setAdminApiKey("test-api-key");

        ApisixClient client = new ApisixClient(config);

        assertDoesNotThrow(() -> {
            client.getClass().getMethod("deleteConsumer", String.class);
        });
    }

    /**
     * 测试 ApisixOperator.createConsumer 不抛出 UnsupportedOperationException
     */
    @Test
    void testCreateConsumerNotThrowUnsupportedOperationException() {
        ApisixOperator operator = new ApisixOperator();

        GatewayConfig config = GatewayConfig.builder()
                .gatewayType(GatewayType.APISIX)
                .build();
        ApisixConfig apisixConfig = new ApisixConfig();
        apisixConfig.setAdminApiEndpoint("http://localhost:9180");
        apisixConfig.setAdminApiKey("test-api-key");
        config.setApisixConfig(apisixConfig);

        Consumer consumer = new Consumer();
        consumer.setConsumerId("test-consumer");

        ConsumerCredential credential = new ConsumerCredential();
        // 设置 API Key 凭证
        com.alibaba.apiopenplatform.support.consumer.ApiKeyConfig apiKeyConfig =
            new com.alibaba.apiopenplatform.support.consumer.ApiKeyConfig();
        apiKeyConfig.setCredentials(java.util.List.of(
            new com.alibaba.apiopenplatform.support.consumer.ApiKeyConfig.ApiKeyCredential()
        ));
        apiKeyConfig.getCredentials().get(0).setApiKey("test-api-key");
        credential.setApiKeyConfig(apiKeyConfig);

        try {
            operator.createConsumer(consumer, credential, config);
        } catch (UnsupportedOperationException e) {
            fail("createConsumer should not throw UnsupportedOperationException");
        } catch (Exception e) {
            // 其他异常（如连接失败、参数校验失败）是可以接受的
            assertTrue(e.getMessage().contains("Connection refused") ||
                       e.getMessage().contains("connect") ||
                       e.getMessage().contains("Failed") ||
                       e instanceof IllegalArgumentException);
        }
    }
}
