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

package com.alibaba.apiopenplatform.service.gateway.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * APISIX Route 模型
 *
 * 表示 APISIX Admin API 返回的 Route 对象
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApisixRoute {

    /**
     * 路由 ID
     */
    private String id;

    /**
     * 路由名称
     */
    private String name;

    /**
     * 路由 URI
     */
    private String uri;

    /**
     * 路由 URIs (多个)
     */
    private List<String> uris;

    /**
     * 路由方法列表
     */
    private List<String> methods;

    /**
     * 插件配置
     */
    private Map<String, Object> plugins;

    /**
     * 上游配置
     */
    private Map<String, Object> upstream;

    /**
     * 上游节点
     */
    private Map<String, Object> upstreamNode;

    /**
     * 主机列表
     */
    private List<String> hosts;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 状态（启用/禁用）
     */
    private Boolean status;

    /**
     * 描述
     */
    private String desc;

    /**
     * 标签
     */
    private List<String> labels;

    /**
     * 创建时间
     */
    private Long create_time;

    /**
     * 更新时间
     */
    private Long update_time;

    /**
     * 检查是否包含 mcp-bridge 插件
     */
    @JsonIgnore
    public boolean hasMcpBridgePlugin() {
        return plugins != null && plugins.containsKey("mcp-bridge");
    }

    /**
     * 获取 mcp-bridge 插件配置
     */
    @JsonIgnore
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMcpBridgeConfig() {
        if (!hasMcpBridgePlugin()) {
            return null;
        }
        return (Map<String, Object>) plugins.get("mcp-bridge");
    }
}
