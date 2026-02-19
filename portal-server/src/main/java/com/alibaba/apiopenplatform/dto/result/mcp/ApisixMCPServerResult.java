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

package com.alibaba.apiopenplatform.dto.result.mcp;

import com.alibaba.apiopenplatform.dto.converter.OutputConverter;
import com.alibaba.apiopenplatform.service.gateway.model.ApisixRoute;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * APISIX MCP Server 结果
 *
 * 表示通过 mcp-bridge 插件配置的 MCP Server
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ApisixMCPServerResult extends GatewayMCPServerResult
        implements OutputConverter<ApisixMCPServerResult, ApisixRoute> {

    /**
     * 路由 ID
     */
    private String routeId;

    /**
     * 路由 URI
     */
    private String uri;

    /**
     * 传输类型（stdio, sse, http）
     */
    private String transportType;

    /**
     * MCP Server 命令
     */
    private String command;

    /**
     * 命令参数
     */
    private String args;

    /**
     * 路由状态
     */
    private Boolean enabled;

    @Override
    public ApisixMCPServerResult convertFrom(ApisixRoute route) {
        this.setRouteId(route.getId());
        this.setUri(route.getUri());
        this.setEnabled(route.getStatus() != null && route.getStatus());

        // 从 URI 中提取 MCP Server 名称
        // 例如: /mcp/filesystem/* -> filesystem
        String uri = route.getUri();
        if (uri != null && uri.contains("/mcp/")) {
            String[] parts = uri.split("/");
            if (parts.length >= 3) {
                this.setMcpServerName(parts[2]);
            }
        }

        // 如果名称为空，使用路由 ID
        if (this.getMcpServerName() == null || this.getMcpServerName().isEmpty()) {
            this.setMcpServerName(route.getId());
        }

        // 解析 mcp-bridge 插件配置
        if (route.hasMcpBridgePlugin()) {
            var mcpConfig = route.getMcpBridgeConfig();
            if (mcpConfig != null) {
                this.setCommand((String) mcpConfig.get("command"));

                Object argsObj = mcpConfig.get("args");
                if (argsObj != null) {
                    this.setArgs(argsObj.toString());
                }

                // 传输类型通常由 mcp-bridge 插件自动检测
                this.setTransportType("stdio");
            }
        }

        return this;
    }
}
