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

import com.alibaba.apiopenplatform.core.utils.IdGenerator;
import com.alibaba.apiopenplatform.dto.params.gateway.ImportGatewayParam;
import com.alibaba.apiopenplatform.support.enums.GatewayType;
import com.alibaba.apiopenplatform.support.gateway.ApisixConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * APISIX 网关导入测试
 */
class ApisixImportGatewayTest {

    /**
     * 测试 ImportGatewayParam 支持 APISIX 配置
     */
    @Test
    void testImportGatewayParamSupportsApisix() {
        ImportGatewayParam param = new ImportGatewayParam();
        param.setGatewayName("APISIX 测试网关");
        param.setGatewayType(GatewayType.APISIX);
        param.setDescription("用于测试的 APISIX 网关");

        ApisixConfig apisixConfig = new ApisixConfig();
        apisixConfig.setAdminApiEndpoint("http://localhost:9180");
        apisixConfig.setAdminApiKey("test-api-key");
        apisixConfig.setTimeout(30000);
        param.setApisixConfig(apisixConfig);

        assertNotNull(param.getApisixConfig());
        assertEquals("http://localhost:9180", param.getApisixConfig().getAdminApiEndpoint());
    }

    /**
     * 测试 APISIX 网关配置验证
     */
    @Test
    void testApisixGatewayConfigValidation() {
        ImportGatewayParam param = new ImportGatewayParam();
        param.setGatewayName("APISIX 测试网关");
        param.setGatewayType(GatewayType.APISIX);

        ApisixConfig apisixConfig = new ApisixConfig();
        apisixConfig.setAdminApiEndpoint("http://localhost:9180");
        apisixConfig.setAdminApiKey("test-api-key");
        param.setApisixConfig(apisixConfig);

        // 验证配置已设置
        assertNotNull(param.getApisixConfig(), "APISIX 配置应该已设置");
        assertNotNull(param.getApisixConfig().getAdminApiEndpoint(), "Endpoint 应该已设置");
    }

    /**
     * 测试 APISIX 网关配置完整性
     */
    @Test
    void testApisixGatewayConfigComplete() {
        ImportGatewayParam param = new ImportGatewayParam();
        param.setGatewayName("APISIX 测试网关");
        param.setGatewayType(GatewayType.APISIX);

        ApisixConfig apisixConfig = new ApisixConfig();
        apisixConfig.setAdminApiEndpoint("http://localhost:9180");
        apisixConfig.setAdminApiKey("test-api-key");
        apisixConfig.setTimeout(60000);
        param.setApisixConfig(apisixConfig);

        // 验证所有配置项
        assertEquals("http://localhost:9180", param.getApisixConfig().getAdminApiEndpoint());
        assertEquals("test-api-key", param.getApisixConfig().getAdminApiKey());
        assertEquals(60000, param.getApisixConfig().getTimeout());
    }

    /**
     * 测试生成 APISIX 网关 ID
     */
    @Test
    void testGenApisixGatewayId() {
        String gatewayId = IdGenerator.genApisixGatewayId();

        assertNotNull(gatewayId);
        assertTrue(gatewayId.startsWith("apisix-"), "APISIX 网关 ID 应该以 'apisix-' 开头");
        assertTrue(gatewayId.length() > 7, "网关 ID 应该包含唯一标识");
    }

    /**
     * 测试 GatewayType.isApisix()
     */
    @Test
    void testGatewayTypeIsApisix() {
        assertTrue(GatewayType.APISIX.isApisix());
        assertFalse(GatewayType.HIGRESS.isApisix());
    }
}
