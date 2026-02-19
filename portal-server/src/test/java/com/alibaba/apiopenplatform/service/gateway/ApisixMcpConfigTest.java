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
import com.alibaba.apiopenplatform.support.enums.GatewayType;
import com.alibaba.apiopenplatform.support.gateway.ApisixConfig;
import com.alibaba.apiopenplatform.support.product.ApisixRefConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * APISIX MCP 配置测试
 *
 * 测试 fetchMcpConfig 方法
 */
class ApisixMcpConfigTest {

    private ApisixOperator operator;
    private Gateway gateway;

    @BeforeEach
    void setUp() {
        operator = new ApisixOperator();

        gateway = new Gateway();
        gateway.setGatewayType(GatewayType.APISIX);

        ApisixConfig config = new ApisixConfig();
        config.setAdminApiEndpoint("http://localhost:9180");
        config.setAdminApiKey("test-api-key");
        gateway.setApisixConfig(config);
    }

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
        config.setRouteId("test-route");

        assertEquals("test-route", config.getRouteId());
    }

    /**
     * 测试 ApisixRefConfig 有 mcpServerName 字段
     */
    @Test
    void testApisixRefConfigHasMcpServerName() {
        ApisixRefConfig config = new ApisixRefConfig();
        config.setMcpServerName("filesystem");

        assertEquals("filesystem", config.getMcpServerName());
    }

    /**
     * 测试 fetchMcpConfig 方法不抛出 UnsupportedOperationException
     */
    @Test
    void testFetchMcpConfigNotThrowUnsupportedOperationException() {
        ApisixRefConfig refConfig = new ApisixRefConfig();
        refConfig.setRouteId("test-route");

        try {
            operator.fetchMcpConfig(gateway, refConfig);
        } catch (UnsupportedOperationException e) {
            fail("fetchMcpConfig should not throw UnsupportedOperationException");
        } catch (Exception e) {
            // 其他异常（如连接失败、认证失败）是可以接受的
            String message = e.getMessage();
            assertTrue(message == null ||
                       message.contains("Connection refused") ||
                       message.contains("connect") ||
                       message.contains("Failed") ||
                       message.contains("401") ||
                       message.contains("Unauthorized") ||
                       message.contains("wrong apikey") ||
                       message.contains("404") ||
                       message.contains("Not Found"));
        }
    }
}
