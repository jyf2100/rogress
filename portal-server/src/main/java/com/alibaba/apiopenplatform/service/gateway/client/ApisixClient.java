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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * APISIX Admin API 客户端
 *
 * 用于与 APISIX Admin API 进行交互，支持 Route、Upstream、Plugin 等资源的管理
 */
@Slf4j
public class ApisixClient extends GatewayClient {

    private static final String ADMIN_API_PREFIX = "/apisix/admin";

    private final ApisixConfig config;

    public ApisixClient(ApisixConfig config) {
        this.config = config;
    }

    /**
     * 构建完整的 API URL
     *
     * @param path API 路径（如 /routes 或 routes）
     * @return 完整的 URL
     */
    public String buildUrl(String path) {
        String baseUrl = config.getAdminApiEndpoint();

        // 移除尾部斜杠
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        // 确保路径以斜杠开头
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        return baseUrl + ADMIN_API_PREFIX + path;
    }

    /**
     * 构建请求头，包含认证信息
     *
     * @return 包含 X-API-KEY 的请求头
     */
    public HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-KEY", config.getAdminApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Override
    public void close() {
        // 暂无资源需要关闭
    }
}
