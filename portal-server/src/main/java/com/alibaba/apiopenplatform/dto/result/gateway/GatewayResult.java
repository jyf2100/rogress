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

package com.alibaba.apiopenplatform.dto.result.gateway;

import com.alibaba.apiopenplatform.dto.converter.OutputConverter;
import com.alibaba.apiopenplatform.entity.Gateway;
import com.alibaba.apiopenplatform.support.enums.GatewayType;
import com.alibaba.apiopenplatform.support.gateway.APIGConfig;
import com.alibaba.apiopenplatform.support.gateway.AdpAIGatewayConfig;
import com.alibaba.apiopenplatform.support.gateway.HigressConfig;
import com.alibaba.apiopenplatform.support.gateway.ApisixConfig;
import com.alibaba.apiopenplatform.support.gateway.ApsaraGatewayConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GatewayResult implements OutputConverter<GatewayResult, Gateway> {

    private String gatewayId;

    private GatewayType gatewayType;

    private String gatewayName;

    private APIGConfigResult apigConfig;

    private AdpAIGatewayConfigResult adpAIGatewayConfig;

    private HigressConfigResult higressConfig;

    private ApisixConfigResult apisixConfig;

    private ApsaraGatewayConfigResult apsaraGatewayConfig;

    private LocalDateTime createAt;

    @Override
    public GatewayResult convertFrom(Gateway source) {
        OutputConverter.super.convertFrom(source);
        if (source.getGatewayType().isAPIG() && !source.getGatewayType().equals(GatewayType.ADP_AI_GATEWAY) && !source.getGatewayType().equals(GatewayType.APSARA_GATEWAY)) {
            setApigConfig(new APIGConfigResult().convertFrom(source.getApigConfig()));
        } else if (source.getGatewayType().equals(GatewayType.ADP_AI_GATEWAY)) {
            setAdpAIGatewayConfig(new AdpAIGatewayConfigResult().convertFrom(source.getAdpAIGatewayConfig()));
        } else if (source.getGatewayType().equals(GatewayType.APSARA_GATEWAY)) {
            setApsaraGatewayConfig(new ApsaraGatewayConfigResult().convertFrom(source.getApsaraGatewayConfig()));
        } else if (source.getGatewayType().equals(GatewayType.APISIX)) {
            setApisixConfig(new ApisixConfigResult().convertFrom(source.getApisixConfig()));
        } else {
            setHigressConfig(new HigressConfigResult().convertFrom(source.getHigressConfig()));
        }
        return this;
    }

    @Data
    public static class APIGConfigResult implements OutputConverter<APIGConfigResult, APIGConfig> {
        private String region;
    }

    @Data
    public static class AdpAIGatewayConfigResult implements OutputConverter<AdpAIGatewayConfigResult, AdpAIGatewayConfig> {
        private String baseUrl;
        private Integer port;
        private String authSeed;
    }

    @Data
    public static class HigressConfigResult implements OutputConverter<HigressConfigResult, HigressConfig> {
        private String address;
        private String username;
    }

    @Data
    public static class ApisixConfigResult implements OutputConverter<ApisixConfigResult, ApisixConfig> {
        private String adminApiEndpoint;
        private Integer timeout;
    }

    @Data
    public static class ApsaraGatewayConfigResult implements OutputConverter<ApsaraGatewayConfigResult, ApsaraGatewayConfig> {
        private String endpoint;
        private String product;
        private String version;
    }
}
