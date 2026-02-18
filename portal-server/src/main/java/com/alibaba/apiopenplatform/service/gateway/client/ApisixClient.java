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

import com.alibaba.apiopenplatform.service.gateway.factory.HTTPClientFactory;
import com.alibaba.apiopenplatform.support.gateway.ApisixConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * APISIX Admin API 客户端
 *
 * 用于与 APISIX Admin API 进行交互，支持 Route、Upstream、Plugin 等资源的管理
 */
@Slf4j
public class ApisixClient extends GatewayClient {

    private static final String ADMIN_API_PREFIX = "/apisix/admin";

    private final ApisixConfig config;
    private final RestTemplate restTemplate;

    public ApisixClient(ApisixConfig config) {
        this.config = config;
        this.restTemplate = HTTPClientFactory.createRestTemplate();
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
     * 构建完整的 API URL（带查询参数）
     *
     * @param path API 路径
     * @param queryParams 查询参数
     * @return 完整的 URL
     */
    public String buildUrl(String path, Map<String, String> queryParams) {
        StringBuilder url = new StringBuilder(buildUrl(path));

        if (queryParams != null && !queryParams.isEmpty()) {
            url.append('?');
            queryParams.forEach((key, value) -> {
                if (url.charAt(url.length() - 1) != '?') {
                    url.append('&');
                }
                url.append(key).append('=').append(value);
            });
        }

        return url.toString();
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

    /**
     * 执行 HTTP 请求
     *
     * @param path API 路径
     * @param method HTTP 方法
     * @param queryParams 查询参数（可为 null）
     * @param body 请求体（可为 null）
     * @param responseType 返回类型
     * @return 响应结果
     */
    public <T, R> T execute(String path,
                            HttpMethod method,
                            Map<String, String> queryParams,
                            R body,
                            ParameterizedTypeReference<T> responseType) {
        try {
            String url = buildUrl(path, queryParams);
            HttpHeaders headers = buildHeaders();

            log.info("APISIX request: {} {}", method, url);

            ResponseEntity<T> response = restTemplate.exchange(
                    url,
                    method,
                    new HttpEntity<>(body, headers),
                    responseType
            );

            log.info("APISIX response: status={}", response.getStatusCode());

            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("HTTP error executing APISIX request: status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            log.error("Error executing APISIX request: {}", e.getMessage());
            throw new RuntimeException("Failed to execute APISIX request", e);
        }
    }

    /**
     * 执行 HTTP 请求（简化版本，使用 Class 类型）
     *
     * @param path API 路径
     * @param method HTTP 方法
     * @param queryParams 查询参数（可为 null）
     * @param body 请求体（可为 null）
     * @param responseType 返回类型
     * @return 响应结果
     */
    public <T, R> T execute(String path,
                            HttpMethod method,
                            Map<String, String> queryParams,
                            R body,
                            Class<T> responseType) {
        return execute(path, method, queryParams, body,
                ParameterizedTypeReference.forType(responseType));
    }

    @Override
    public void close() {
        HTTPClientFactory.closeClient(restTemplate);
    }
}
