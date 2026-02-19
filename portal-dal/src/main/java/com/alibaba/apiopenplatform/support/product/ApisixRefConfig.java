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

package com.alibaba.apiopenplatform.support.product;

import lombok.Data;

/**
 * APISIX 网关资源配置
 *
 * 用于引用 APISIX 网关中的特定资源（如 MCP Server Route）
 */
@Data
public class ApisixRefConfig {

    /**
     * 路由 ID
     */
    private String routeId;

    /**
     * MCP Server 名称
     */
    private String mcpServerName;

    /**
     * Model 路由名称
     */
    private String modelRouteName;
}
