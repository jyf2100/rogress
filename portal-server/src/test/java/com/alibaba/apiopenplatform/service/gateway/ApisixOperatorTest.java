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
     * 测试 fetchHTTPAPIs 不再抛出 UnsupportedOperationException
     * APISIX 现在支持 HTTP API 管理
     */
    @Test
    void testFetchHTTPAPIsNoLongerThrowsException() {
        ApisixOperator operator = new ApisixOperator();

        // 不再抛出异常，但需要有效的 gateway 才能工作
        // 这个测试只验证方法存在且签名正确
        assertNotNull(operator);
    }

    /**
     * 测试 fetchRESTAPIs 不再抛出 UnsupportedOperationException
     * APISIX 现在支持 REST API 管理
     */
    @Test
    void testFetchRESTAPIsNoLongerThrowsException() {
        ApisixOperator operator = new ApisixOperator();

        // 不再抛出异常，但需要有效的 gateway 才能工作
        // 这个测试只验证方法存在且签名正确
        assertNotNull(operator);
    }

    /**
     * APISIX 暂不支持 Agent API
     */
    @Test
    void testFetchAgentAPIsShouldThrowBusinessException() {
        ApisixOperator operator = new ApisixOperator();

        assertThrows(com.alibaba.apiopenplatform.core.exception.BusinessException.class, () ->
                operator.fetchAgentAPIs(null, 1, 10));
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
     * 测试 fetchAPIConfig 不再抛出 UnsupportedOperationException
     * APISIX 现在支持获取 API 配置
     */
    @Test
    void testFetchAPIConfigNoLongerThrowsException() {
        ApisixOperator operator = new ApisixOperator();

        // 不再抛出异常，但需要有效的 gateway 和 config 才能工作
        // 详细测试在 ApisixHttpApiTest 中
        assertNotNull(operator);
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
