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

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Map;

/**
 * APISIX Consumer 模型
 *
 * 表示 APISIX Admin API 中的 Consumer 对象
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApisixConsumer {

    /**
     * 消费者用户名（唯一标识）
     */
    private String username;

    /**
     * 插件配置
     */
    private Map<String, Object> plugins;

    /**
     * 描述
     */
    private String desc;

    /**
     * 标签
     */
    private Map<String, String> labels;

    /**
     * 创建时间（只读字段，由 APISIX 自动管理）
     */
    private Long create_time;

    /**
     * 更新时间（只读字段，由 APISIX 自动管理）
     */
    private Long update_time;

    /**
     * 创建 key-auth 插件配置的 Consumer
     */
    public static ApisixConsumer withKeyAuth(String username, String apiKey) {
        ApisixConsumer consumer = new ApisixConsumer();
        consumer.setUsername(username);
        consumer.setPlugins(Map.of(
                "key-auth", Map.of("key", apiKey)
        ));
        return consumer;
    }
}
