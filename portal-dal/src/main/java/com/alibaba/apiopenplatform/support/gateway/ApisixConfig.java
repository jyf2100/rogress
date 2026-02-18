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

package com.alibaba.apiopenplatform.support.gateway;

import com.alibaba.apiopenplatform.support.common.Encrypted;
import lombok.Data;

/**
 * APISIX 网关配置
 *
 * 用于连接 APISIX Admin API 的配置信息
 */
@Data
public class ApisixConfig {

    /**
     * APISIX Admin API 端点地址
     * 例如: http://localhost:9180
     */
    private String adminApiEndpoint;

    /**
     * APISIX Admin API Key
     * 用于认证 Admin API 请求
     */
    @Encrypted
    private String adminApiKey;

    /**
     * 请求超时时间（毫秒）
     * 默认 30000ms (30秒)
     */
    private Integer timeout = 30000;

    /**
     * 构建唯一标识键
     * 用于缓存和去重
     *
     * @return 唯一标识字符串
     */
    public String buildUniqueKey() {
        return String.format("%s:%s", adminApiEndpoint, adminApiKey);
    }
}
