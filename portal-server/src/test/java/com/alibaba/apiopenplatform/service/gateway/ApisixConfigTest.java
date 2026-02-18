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

import com.alibaba.apiopenplatform.converter.ApisixConfigConverter;
import com.alibaba.apiopenplatform.support.enums.GatewayType;
import com.alibaba.apiopenplatform.support.gateway.ApisixConfig;
import com.alibaba.apiopenplatform.support.gateway.GatewayConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * APISIX 网关配置测试
 *
 * 验证 APISIX 网关类型和配置类的正确性
 */
class ApisixConfigTest {

    /**
     * 测试 GatewayType 枚举包含 APISIX
     */
    @Test
    void testGatewayTypeContainsApisix() {
        // Act
        GatewayType type = GatewayType.APISIX;

        // Assert
        assertNotNull(type);
        assertEquals("APISIX", type.getType());
    }

    /**
     * 测试 GatewayType.APISIX 的辅助方法
     */
    @Test
    void testGatewayTypeApisixHelperMethods() {
        GatewayType type = GatewayType.APISIX;

        // APISIX 不是 Higress，也不是 APIG
        assertFalse(type.isHigress());
        assertFalse(type.isAPIG());
        assertFalse(type.isAIGateway());
        assertFalse(type.isAdpAIGateway());
        assertFalse(type.isApsaraGateway());
        assertTrue(type.isApisix());
    }

    /**
     * 测试 ApisixConfig 类存在且字段正确
     */
    @Test
    void testApisixConfigFields() {
        // Arrange
        ApisixConfig config = new ApisixConfig();

        // Act
        config.setAdminApiEndpoint("http://localhost:9180");
        config.setAdminApiKey("test-api-key");

        // Assert
        assertEquals("http://localhost:9180", config.getAdminApiEndpoint());
        assertEquals("test-api-key", config.getAdminApiKey());
    }

    /**
     * 测试 ApisixConfig 默认超时值
     */
    @Test
    void testApisixConfigDefaultTimeout() {
        ApisixConfig config = new ApisixConfig();

        // 默认超时应该是 30000ms
        assertEquals(30000, config.getTimeout());
    }

    /**
     * 测试 ApisixConfig 的 buildUniqueKey 方法
     */
    @Test
    void testApisixConfigBuildUniqueKey() {
        ApisixConfig config = new ApisixConfig();
        config.setAdminApiEndpoint("http://localhost:9180");
        config.setAdminApiKey("test-key");

        String uniqueKey = config.buildUniqueKey();

        assertNotNull(uniqueKey);
        assertTrue(uniqueKey.contains("http://localhost:9180"));
    }

    /**
     * 测试 GatewayConfig 包含 apisixConfig 字段
     */
    @Test
    void testGatewayConfigContainsApisixConfig() {
        GatewayConfig config = GatewayConfig.builder()
                .gatewayType(GatewayType.APISIX)
                .build();

        ApisixConfig apisixConfig = new ApisixConfig();
        apisixConfig.setAdminApiEndpoint("http://localhost:9180");

        config.setApisixConfig(apisixConfig);

        assertNotNull(config.getApisixConfig());
        assertEquals("http://localhost:9180", config.getApisixConfig().getAdminApiEndpoint());
    }

    /**
     * 测试 ApisixConfigConverter 存在
     */
    @Test
    void testApisixConfigConverterExists() {
        ApisixConfigConverter converter = new ApisixConfigConverter();
        assertNotNull(converter);
    }

    /**
     * 测试 ApisixConfigConverter 序列化和反序列化
     */
    @Test
    void testApisixConfigConverterRoundTrip() {
        ApisixConfigConverter converter = new ApisixConfigConverter();

        ApisixConfig original = new ApisixConfig();
        original.setAdminApiEndpoint("http://localhost:9180");
        original.setAdminApiKey("test-api-key");
        original.setTimeout(60000);

        // 序列化
        String json = converter.convertToDatabaseColumn(original);
        assertNotNull(json);
        assertTrue(json.contains("http://localhost:9180"));

        // 反序列化
        ApisixConfig restored = converter.convertToEntityAttribute(json);
        assertNotNull(restored);
        assertEquals("http://localhost:9180", restored.getAdminApiEndpoint());
        assertEquals("test-api-key", restored.getAdminApiKey());
        assertEquals(60000, restored.getTimeout());
    }
}
