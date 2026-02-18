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

package com.alibaba.apiopenplatform.converter;

import com.alibaba.apiopenplatform.support.gateway.ApisixConfig;

import jakarta.persistence.Converter;

/**
 * APISIX 配置 JPA 转换器
 *
 * 用于在 ApisixConfig 对象和 JSON 字符串之间进行转换
 */
@Converter(autoApply = true)
public class ApisixConfigConverter extends JsonConverter<ApisixConfig> {

    protected ApisixConfigConverter() {
        super(ApisixConfig.class);
    }
}
