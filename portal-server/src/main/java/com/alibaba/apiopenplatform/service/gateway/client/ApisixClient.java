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
import lombok.extern.slf4j.Slf4j;

/**
 * APISIX Admin API 客户端
 *
 * 用于与 APISIX Admin API 进行交互，支持 Route、Upstream、Plugin 等资源的管理
 */
@Slf4j
public class ApisixClient extends GatewayClient {

    private final ApisixConfig config;

    public ApisixClient(ApisixConfig config) {
        this.config = config;
    }

    @Override
    public void close() {
        // 暂无资源需要关闭
    }
}
