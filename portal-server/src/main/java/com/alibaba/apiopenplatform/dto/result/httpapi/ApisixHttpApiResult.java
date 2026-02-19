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

package com.alibaba.apiopenplatform.dto.result.httpapi;

import com.alibaba.apiopenplatform.service.gateway.model.ApisixRoute;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * APISIX HTTP API 结果
 *
 * 表示通过普通 Route 配置的 HTTP API（非 MCP/Model）
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ApisixHttpApiResult extends APIResult {

    /**
     * 路由 ID
     */
    private String routeId;

    /**
     * 路由 URI
     */
    private String uri;

    /**
     * HTTP 方法列表
     */
    private List<String> methods;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 从 ApisixRoute 转换
     */
    public ApisixHttpApiResult convertFrom(ApisixRoute route) {
        // 设置基类字段
        this.setApiId(route.getId());
        this.setApiName(route.getName() != null && !route.getName().isEmpty()
                ? route.getName()
                : route.getId());

        // 设置扩展字段
        this.setRouteId(route.getId());
        this.setUri(route.getUri());
        this.setMethods(route.getMethods());
        this.setEnabled(route.isEnabled());

        return this;
    }
}
