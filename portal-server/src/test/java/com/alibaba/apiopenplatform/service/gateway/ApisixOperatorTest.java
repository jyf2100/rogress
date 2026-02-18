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

import com.alibaba.apiopenplatform.support.enums.GatewayType;
import com.alibaba.apiopenplatform.support.gateway.ApisixConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ApisixOperator 单元测试
 *
 * 测试 APISIX 网关操作器的基础功能
 */
class ApisixOperatorTest {

    /**
     * 测试 ApisixOperator 类存在且可以实例化
     */
    @Test
    void testApisixOperatorExists() {
        ApisixOperator operator = new ApisixOperator();

        assertNotNull(operator);
    }

    /**
     * 测试 ApisixOperator 继承自 GatewayOperator
     */
    @Test
    void testApisixOperatorExtendsGatewayOperator() {
        ApisixOperator operator = new ApisixOperator();

        assertTrue(operator instanceof GatewayOperator);
    }

    /**
     * 测试 getGatewayType 返回 APISIX
     */
    @Test
    void testGetGatewayType() {
        ApisixOperator operator = new ApisixOperator();

        assertEquals(GatewayType.APISIX, operator.getGatewayType());
    }

    /**
     * 测试 ApisixOperator 是 Spring Service（可被自动发现）
     */
    @Test
    void testApisixOperatorIsAnnotatedWithService() {
        // 验证类上有 @Service 注解
        assertTrue(ApisixOperator.class.isAnnotationPresent(org.springframework.stereotype.Service.class));
    }

    /**
     * 测试 fetchHTTPAPIs 抛出 UnsupportedOperationException
     * APISIX 初期不支持 HTTP API 管理
     */
    @Test
    void testFetchHTTPAPIsThrowsUnsupportedOperationException() {
        ApisixOperator operator = new ApisixOperator();

        assertThrows(UnsupportedOperationException.class, () ->
            operator.fetchHTTPAPIs(null, 1, 10));
    }

    /**
     * 测试 fetchRESTAPIs 抛出 UnsupportedOperationException
     * APISIX 初期不支持 REST API 管理
     */
    @Test
    void testFetchRESTAPIsThrowsUnsupportedOperationException() {
        ApisixOperator operator = new ApisixOperator();

        assertThrows(UnsupportedOperationException.class, () ->
            operator.fetchRESTAPIs(null, 1, 10));
    }

    /**
     * 测试 fetchAgentAPIs 返回 null
     */
    @Test
    void testFetchAgentAPIsReturnsNull() {
        ApisixOperator operator = new ApisixOperator();

        assertNull(operator.fetchAgentAPIs(null, 1, 10));
    }

    /**
     * 测试 fetchGateways 抛出 UnsupportedOperationException
     */
    @Test
    void testFetchGatewaysThrowsUnsupportedOperationException() {
        ApisixOperator operator = new ApisixOperator();

        assertThrows(UnsupportedOperationException.class, () ->
            operator.fetchGateways(null, 1, 10));
    }

    /**
     * 测试 fetchAPIConfig 抛出 UnsupportedOperationException
     */
    @Test
    void testFetchAPIConfigThrowsUnsupportedOperationException() {
        ApisixOperator operator = new ApisixOperator();

        assertThrows(UnsupportedOperationException.class, () ->
            operator.fetchAPIConfig(null, null));
    }

    /**
     * 测试 fetchAPI 抛出 UnsupportedOperationException
     */
    @Test
    void testFetchAPIThrowsUnsupportedOperationException() {
        ApisixOperator operator = new ApisixOperator();

        assertThrows(UnsupportedOperationException.class, () ->
            operator.fetchAPI(null, "test-api-id"));
    }

    /**
     * 测试 getDashboard 抛出 UnsupportedOperationException
     */
    @Test
    void testGetDashboardThrowsUnsupportedOperationException() {
        ApisixOperator operator = new ApisixOperator();

        assertThrows(UnsupportedOperationException.class, () ->
            operator.getDashboard(null, "default"));
    }

    /**
     * 测试 fetchGatewayIps 返回空列表
     */
    @Test
    void testFetchGatewayIpsReturnsEmptyList() {
        ApisixOperator operator = new ApisixOperator();

        assertTrue(operator.fetchGatewayIps(null).isEmpty());
    }
}
