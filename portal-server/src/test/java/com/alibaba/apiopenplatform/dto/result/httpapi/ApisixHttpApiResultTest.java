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
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ApisixHttpApiResult 测试
 */
class ApisixHttpApiResultTest {

    @Test
    void shouldConvertFromRoute() {
        // Given
        ApisixRoute route = new ApisixRoute();
        route.setId("test-route-id");
        route.setName("Test API");
        route.setUri("/api/test/*");
        route.setMethods(List.of("GET", "POST"));
        route.setStatus(true);

        // When
        ApisixHttpApiResult result = new ApisixHttpApiResult().convertFrom(route);

        // Then
        assertEquals("test-route-id", result.getApiId());
        assertEquals("Test API", result.getApiName());
        assertEquals("test-route-id", result.getRouteId());
        assertEquals("/api/test/*", result.getUri());
        assertEquals(2, result.getMethods().size());
        assertTrue(result.getMethods().contains("GET"));
        assertTrue(result.getMethods().contains("POST"));
        assertTrue(result.getEnabled());
    }

    @Test
    void shouldUseIdAsNameWhenNameIsNull() {
        // Given
        ApisixRoute route = new ApisixRoute();
        route.setId("route-without-name");
        route.setName(null);
        route.setUri("/api/no-name/*");
        route.setStatus(true);

        // When
        ApisixHttpApiResult result = new ApisixHttpApiResult().convertFrom(route);

        // Then
        assertEquals("route-without-name", result.getApiId());
        assertEquals("route-without-name", result.getApiName());
    }

    @Test
    void shouldHandleNullStatusAsEnabled() {
        // Given
        ApisixRoute route = new ApisixRoute();
        route.setId("route-null-status");
        route.setUri("/api/test/*");
        route.setStatus(null);

        // When
        ApisixHttpApiResult result = new ApisixHttpApiResult().convertFrom(route);

        // Then
        assertTrue(result.getEnabled());
    }

    @Test
    void shouldHandleDisabledRoute() {
        // Given
        ApisixRoute route = new ApisixRoute();
        route.setId("disabled-route");
        route.setUri("/api/disabled/*");
        route.setStatus(false);

        // When
        ApisixHttpApiResult result = new ApisixHttpApiResult().convertFrom(route);

        // Then
        assertFalse(result.getEnabled());
    }
}
