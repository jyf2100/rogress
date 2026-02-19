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
import com.alibaba.apiopenplatform.service.gateway.model.ApisixConsumer;
import com.alibaba.apiopenplatform.service.gateway.model.ApisixRoute;
import com.alibaba.apiopenplatform.support.gateway.ApisixConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
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

    // ==================== Route 管理 API ====================

    /**
     * 获取所有路由列表
     *
     * @return 路由列表
     */
    public List<ApisixRoute> listRoutes() {
        ApisixListResponse<ApisixRouteData> response = execute(
                "/routes",
                HttpMethod.GET,
                null,
                null,
                new ParameterizedTypeReference<ApisixListResponse<ApisixRouteData>>() {}
        );

        if (response == null || response.getList() == null) {
            return new ArrayList<>();
        }

        return response.getList().getNodes().stream()
                .map(node -> {
                    ApisixRoute route = convertToRoute(node.getValue());
                    route.setId(extractIdFromKey(node.getKey()));
                    return route;
                })
                .toList();
    }

    /**
     * 将 ApisixRouteData 转换为 ApisixRoute
     */
    private ApisixRoute convertToRoute(ApisixRouteData data) {
        ApisixRoute route = new ApisixRoute();
        route.setId(data.getId());
        route.setName(data.getName());
        route.setUri(data.getUri());
        route.setUris(data.getUris());
        route.setMethods(data.getMethods());
        route.setPlugins(data.getPlugins());
        route.setUpstream(data.getUpstream());
        route.setStatus(data.getStatus());
        return route;
    }

    /**
     * 获取单个路由详情
     *
     * @param routeId 路由 ID
     * @return 路由详情
     */
    public ApisixRoute getRoute(String routeId) {
        ApisixResponse<ApisixRoute> response = execute(
                "/routes/" + routeId,
                HttpMethod.GET,
                null,
                null,
                new ParameterizedTypeReference<ApisixResponse<ApisixRoute>>() {}
        );

        if (response == null) {
            return null;
        }

        ApisixRoute route = response.getValue();
        if (route != null) {
            route.setId(routeId);
        }
        return route;
    }

    /**
     * 创建路由
     *
     * @param routeId 路由 ID
     * @param route 路由配置
     * @return 创建的路由
     */
    public ApisixRoute createRoute(String routeId, ApisixRoute route) {
        execute(
                "/routes/" + routeId,
                HttpMethod.PUT,
                null,
                route,
                Void.class
        );
        return getRoute(routeId);
    }

    /**
     * 更新路由
     *
     * @param routeId 路由 ID
     * @param route 路由配置
     * @return 更新后的路由
     */
    public ApisixRoute updateRoute(String routeId, ApisixRoute route) {
        execute(
                "/routes/" + routeId,
                HttpMethod.PUT,
                null,
                route,
                Void.class
        );
        return getRoute(routeId);
    }

    /**
     * 删除路由
     *
     * @param routeId 路由 ID
     */
    public void deleteRoute(String routeId) {
        execute(
                "/routes/" + routeId,
                HttpMethod.DELETE,
                null,
                null,
                Void.class
        );
    }

    // ==================== Consumer 管理 API ====================

    /**
     * 获取所有消费者列表
     *
     * @return 消费者列表
     */
    public List<ApisixConsumer> listConsumers() {
        ApisixListResponse<ApisixConsumerData> response = execute(
                "/consumers",
                HttpMethod.GET,
                null,
                null,
                new ParameterizedTypeReference<ApisixListResponse<ApisixConsumerData>>() {}
        );

        if (response == null || response.getList() == null) {
            return new ArrayList<>();
        }

        return response.getList().getNodes().stream()
                .map(node -> {
                    ApisixConsumer consumer = convertToConsumer(node.getValue());
                    consumer.setUsername(extractIdFromKey(node.getKey()));
                    return consumer;
                })
                .toList();
    }

    /**
     * 获取单个消费者详情
     *
     * @param username 消费者用户名
     * @return 消费者详情
     */
    public ApisixConsumer getConsumer(String username) {
        ApisixResponse<ApisixConsumer> response = execute(
                "/consumers/" + username,
                HttpMethod.GET,
                null,
                null,
                new ParameterizedTypeReference<ApisixResponse<ApisixConsumer>>() {}
        );

        if (response == null) {
            return null;
        }

        ApisixConsumer consumer = response.getValue();
        if (consumer != null) {
            consumer.setUsername(username);
        }
        return consumer;
    }

    /**
     * 创建消费者
     *
     * @param username 消费者用户名
     * @param consumer 消费者配置
     * @return 创建的消费者
     */
    public ApisixConsumer createConsumer(String username, ApisixConsumer consumer) {
        execute(
                "/consumers/" + username,
                HttpMethod.PUT,
                null,
                consumer,
                Void.class
        );
        return getConsumer(username);
    }

    /**
     * 更新消费者
     *
     * @param username 消费者用户名
     * @param consumer 消费者配置
     * @return 更新后的消费者
     */
    public ApisixConsumer updateConsumer(String username, ApisixConsumer consumer) {
        execute(
                "/consumers/" + username,
                HttpMethod.PUT,
                null,
                consumer,
                Void.class
        );
        return getConsumer(username);
    }

    /**
     * 删除消费者
     *
     * @param username 消费者用户名
     */
    public void deleteConsumer(String username) {
        execute(
                "/consumers/" + username,
                HttpMethod.DELETE,
                null,
                null,
                Void.class
        );
    }

    /**
     * 检查消费者是否存在
     *
     * @param username 消费者用户名
     * @return 是否存在
     */
    public boolean consumerExists(String username) {
        try {
            ApisixConsumer consumer = getConsumer(username);
            return consumer != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 将 ApisixConsumerData 转换为 ApisixConsumer
     */
    private ApisixConsumer convertToConsumer(ApisixConsumerData data) {
        ApisixConsumer consumer = new ApisixConsumer();
        consumer.setUsername(data.getUsername());
        consumer.setPlugins(data.getPlugins());
        consumer.setDesc(data.getDesc());
        consumer.setLabels(data.getLabels());
        return consumer;
    }

    // ==================== 辅助方法 ====================

    /**
     * 从 key 中提取 ID
     * 例如: /apisix/routes/route_id -> route_id
     */
    private String extractIdFromKey(String key) {
        if (key == null) {
            return null;
        }
        String[] parts = key.split("/");
        return parts.length > 0 ? parts[parts.length - 1] : key;
    }

    // ==================== 响应模型类 ====================

    /**
     * APISIX 列表响应
     */
    @Data
    public static class ApisixListResponse<T> {
        private ApisixListNode<T> list;
    }

    /**
     * APISIX 列表节点
     */
    @Data
    public static class ApisixListNode<T> {
        private List<ApisixNode<T>> nodes;
    }

    /**
     * APISIX 节点
     */
    @Data
    public static class ApisixNode<T> {
        private String key;
        private T value;
    }

    /**
     * APISIX 单项响应
     */
    @Data
    public static class ApisixResponse<T> {
        private T value;
    }

    /**
     * 路由数据（用于列表响应）
     */
    @Data
    public static class ApisixRouteData {
        private String id;
        private String name;
        private String uri;
        private List<String> uris;
        private List<String> methods;
        private Map<String, Object> plugins;
        private Map<String, Object> upstream;
        private Boolean status;
    }

    /**
     * 消费者数据（用于列表响应）
     */
    @Data
    public static class ApisixConsumerData {
        private String username;
        private Map<String, Object> plugins;
        private String desc;
        private Map<String, String> labels;
    }
}
